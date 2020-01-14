vera 
================

### [master]
[![Build Status](https://travis-ci.org/DianwodaCompany/vera.svg?branch=master)](https://travis-ci.org/DianwodaCompany/vera)
[![Coverage Status](https://coveralls.io/repos/github/DianwodaCompany/vera/badge.svg?branch=master)](https://coveralls.io/github/DianwodaCompany/vera?branch=master)
[![Coverity Scan Build Status](https://scan.coverity.com/projects/19406/badge.svg)](https://scan.coverity.com/projects/dianwodacompany-vera)

<!-- MarkdownTOC -->

- [Vera 解决什么问题](#vera-解决什么问题)
- [系统详述](#系统详述)
    - [整体架构](#整体架构)
    - [Redis 多主数据同步问题](#redis-多主数据同步问题)
    - [高可用&可运维](#高可用&可运维)
        - [Vera 系统高可用](#vera-系统高可用)
        - [Redis 自身高可用](#redis-自身高可用)
        - [可运维](#可运维)
    - [文件存储结构](#文件存储结构)
    - [测试数据](#测试数据)
        - [延时测试](#延时测试)
- [Contribution](#Contribution)  
- [To do list](#Todolist)  
- [License](#license)

<!-- /MarkdownTOC -->


<a name="vera-解决什么问题"></a>
# Vera解决什么问题
Redis在点我达内部得到了广泛的使用，根据运维数据统计，整个点我达全部Redis Master的读写请求在每秒60W+，其中不包括Slave的读请求，很多业务甚至会将Redis当成内存数据库使用。这样，就对Redis多数据中心提出了很大的需求，一是为了提升可用性，解决数据中心 DR(Disaster Recovery) 问题，二是公司业务实现异地多活，不同机房有自己的数据中心，每个数据中心仅读写当前数据中心的数据，但各数据中心的Redis数据需要同步，允许存在一定的延时。在这样的需求下，Vera应运而生, 一定程度上，Vera实现了Redis Cluster的多主数据同步。  

为了方便描述，后面用 DC 代表数据中心 (Data Center)。

<a name="系统详述"></a>
# 系统详述
<a name="整体架构"></a>
## 整体架构
整体架构图如下所示：  
![design](https://raw.github.com/DianwodaCompany/vera/master/doc/image/total.jpg)  

-  Console：用来提供后台用户操控界面，供用户进行Redis集群侦听配置和各Piper实例的同步配置。
-  NamerServer：各Piper信息与各任务执行情况的集中查看及维护，主要是通过Piper的心跳上报；
-  Piper:Vera中最主要的结点，主要功能如下：
- - Redis命令存储：通过侦听RedisCluster中master结点产生的Redis命令存储到本地文件；
- - 同步其它Piper的数据并写入本地Redis Master中；所以Piper有两种角色，一种是数据产生者，侦听Redis Master并获取新数据，提供给其它需要获取该RedisMaster数据的Piper, 第二种是数据消费者，同步其它Piper的数据并消费该数据，即写入Redis Cluster;

<a name="redis-多主数据同步问题"></a>
## Redis 多主数据同步问题
多数据中心首先要解决的是数据同步问题，即数据如何从一个 DC 传输到另外一个 DC。针对Redis Cluster来说，我们决定采用伪 Redis slave的方案，即实现 Redis 协议，伪装成为 Redis slave，让 Redis master 推送数据至该伪 slave。这个伪 slave的功能是在piper中实现，考虑到目前绝大多数公司都是采用Redis Sentinel架构来搭建Redis集群，所以Vera通过传入Sentinel集群和Redis Master名字来自动获取Redis Master的IP和端口，从而实现连接及侦听功能，如下图所示：  
![pipers](https://raw.github.com/DianwodaCompany/vera/master/doc/image/pipers.jpg)  
Piper接收redis command数据，写入本地的Blockfile, 其中Blockfile采用内存映射方式实现来提高写入速度，同时Blockfile会定时Flush到硬盘以持久化，Blockfile会记录writeOffset及commitOffset分别表示写入及刷新磁盘的位置。其它Piper通过上传自身的同步offset来同步获取增量数据。

### 使用 piper 带来的优势  
- 减少 master 同步次数  
如果异地机房Redis slave 直接连向 redis master，多个 slave 会导致 master 多次同步，而piper可以将增量数据存储在本地，通过机房间的piper的数据同步，异地机房的piper写入异地机房的redis master, 利用redis自身的主从同步，redis slave再从master获取数据。
- 网络异常时减少全量同步  
piper将Redis日志数据缓存到磁盘，这样，可以缓存大量的日志数据 (Redis 将数据缓存到内存 ring buffer，容量有限)，当数据中心之间的网络出现较长时间异常时仍然可以续传日志数据。  
- 更加灵活的数据传输方式
1.piper之间传输协议可以自定义，方便支持压缩 (目前暂未支持)。2.根据业务可以配置每次启动是从最新数据开始同步，还是从上一次的同步位置开始同步；
- 安全性提升  
多个机房之间的数据传输往往需要通过公网进行，这样数据的安全性变得极为重要，piper之间的数据传输也可以加密 (暂未实现)，提升安全性。

### piper增量同步
piper之间的数据同步采用增量同步方式，假设piperA开始同步piperB的数据，有如下三种方式可以实现：
- piperA仅同步piperB后面新进来的数据，忽略之前的历史数据, 默认采用方式；
- piperA取出保存在本地piperB的同步offset, 接着这个offset继续同步；这种方式存在一个问题：如果piperA之前很久未同步过piperB的数据，会导致会有很多历史数据同步过来，不推荐；
- piperA取出保存在本地piperB的同步offset, 接着这个offset继续同步, 同时对同步过来的数据做处理，过滤一定时间间隔之外的数据；

### 避免双向同步
两个redis cluster数据双向同步，就需要两个piper也执行双向同步;但按照目前的架构设计，双向同步会引起数据循环重复同步，即假设RedisA新增一条数据，PiperA侦听到此消息后将其写入本地文件，此后PiperB根据本地的offset同步到该新增数据后，并将该数据写入本地文件，因为是双向同步,所以PiperA根据本地offset又会同步到PiperB的新增数据，而这次数据原先就是从PiperA同步过来的，从而引起数据重复同步。针对这种情况Vera采用的做法是，在piper中保存近30秒的历史数据，如果同步过来的数据存在于该历史数据中，则丢弃。


## 高可用&可运维
<a name="Vera-系统高可用"></a>
### Piper 系统高可用
如果 Piper 挂掉，多数据中心之间的数据传输可能会中断，解决这个问题，Vera设计了两种解决方式：
1) 每个Piper可以部署一主多从，从节点实时从主节点复制数据，当主节点挂掉后，其它Piper会选择该主Piper的从节点可以数据同步，但Piper本身的主从关系没有改变，如果主Piper一直不能恢复，从节点可以配置Redis侦听；
2) 首先预启动一个Piper作为备用，当主Piper挂掉后，操作后台，将挂掉的Piper上面的职能迁移到该备用Piper上面，从而恢复数据同步(因为是人工操作，如果配置的是仅同步新数据，则操作时间内的数据就无法同步)；
<a name="redis-自身高可用"></a>
### Namer Server 系统高可用
Namer Server可以部署多个，每个namer server是无状态的，相互之间没有数据传输，如果部署多个，可以设置Piper同时向多个Namer Server上报信息，当需要从Namer Server获取数据时，会先请求默认配置的NamerServer，如果该NamerServer不可用，会随机从其它可用的NamerServer中挑选出一个，重新请求获取数据；
### Redis 自身高可用
Redis Master也可能会挂，Redis本身提供哨兵 (Sentinel) 机制保证集群的高可用；
### Vera 可运维
各piper间的数据同步可能会因为网络中断、主机重启、redis sentinel迁移、pipe迁移等原因而中断同步，除了piper与redis本身的高可用，同时Vera还提供了运维手段。运维人员可以通过console界面进行操作，如果redis sentinel迁移了，可以重新配置新的redis sentinel,如果piper迁移，也可以重新配置新的piper同步；
运维界面如下：
![console](https://raw.github.com/DianwodaCompany/vera/master/doc/image/console-snapshot.png)  

<a name="文件存储结构"></a>
## 文件存储结构
文件存储结构如下：

![command](https://raw.github.com/DianwodaCompany/vera/master/doc/image/command.jpg)  
piper侦听redis cluster，将接收到的命令同步写入BlockFile，每个BlockFile目前的大小为200M, 写满一个再生成一个新文件，源piper发起数据请求都会带上offset, 目的piper根据offset找到相应blockfile, 并且定位到offset位置读取相应redis命令数据，默认最多读取50条，同时返回下一次应该读取的nextOffset给调用方。其中piper读写blockfile的方式采用内存映射加定时flush硬盘的方式；


<a name="测试数据"></a>
## 测试数据
<a name="延时测试"></a>
### 延时测试
#### 测试方案
测试方式如下图所示。新数据写入Redis Master，Piper1侦听到RedisMaster事件将数据写入本地文件，同时通知其它订阅该Piper的其它Piper来获取新数据，Piper2请求Piper1获取数据后，再写入Redis Master2, 整个测试延时时间为 t1+t2+t3。  
![test](https://raw.github.com/DianwodaCompany/vera/master/doc/image/delay.jpg)  
#### 测试数据
因为Piper间获取新增数据有两种方式: 1. Consumer定时向Provider请求数据，采用pull的方式；2. 为避免没数据时频繁的pull, Provider会hold该请求，如果这时有新增数据，会响应该请求,使得Consumer能在第一时间获取该数据；
在点我达生产环境单个机房进行了测试，大部分情况基本上是在ms级别的范围内。但如果由于Provider hold请求，如果Consumer超时未得到响应，此时Consumer会等待一些时间重新pull, 这个时间默认是10秒，所以这种情况下延时时间为 t1+t2+t3 + 10000，出现次数较少 。

<a name="Contribution"></a>
# Contribution
Thanks for all the people who contributed to Vera !
<a href="https://github.com/DianwodaCompany/vera/graphs/contributors">
<br>
<img class="avatar" src="https://avatars0.githubusercontent.com/u/7858413?s=96&amp;v=4" width="48" height="48" alt="@ainihong001">
<img class="avatar" src="https://avatars2.githubusercontent.com/u/20179128?s=96&amp;v=4" width="48" height="48" alt="@yueyeliuxing">
<img class="avatar" src="https://avatars1.githubusercontent.com/u/54735957?s=460&v=4" width="48" height="48" alt="@gammabin">
</a>

<a name="Todolist"></a>
# To do list
  * piper实现主备功能, 减少master piper挂掉对其它piper数据同步造成的影响； (已经在0.0.2版本中实现)
  * namer-server实现双主功能, 以防止单个namer挂掉造成影响  (已经在0.0.2版本中实现);
  * piper主从同步目前是piper slave自己发现master并自动发起同步的，后期可视情况是否放到后台可配；

<a name="license"></a>
# License
The project is licensed under the [Apache 2 license](https://github.com/DianwodaCompany/vera/master/LICENSE).
