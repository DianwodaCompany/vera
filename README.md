# vera 

==============

### [master]
[![Build Status](https://travis-ci.org/DianwodaCompany/vera.svg?branch=master)](https://travis-ci.org/DianwodaCompany/vera)
[![Coverage Status](https://coveralls.io/repos/github/DianwodaCompany/vera/badge.svg?branch=master)](https://coveralls.io/github/DianwodaCompany/vera?branch=master)
[![Coverity Scan Build Status](https://scan.coverity.com/projects/8884/badge.svg)](https://scan.coverity.com/projects/DianwodaCompany-vera)

<!-- MarkdownTOC -->

- [Vera 解决什么问题](#vera-解决什么问题)
- [系统详述](#系统详述)
    - [整体架构](#整体架构)
    - [Redis 多主数据同步问题](#redis-多主数据同步问题)
    - [高可用](#高可用)
        - [Vera 系统高可用](#vera-系统高可用)
        - [Redis 自身高可用](#redis-自身高可用)
    - [测试数据](#测试数据)
        - [延时测试](#延时测试)
- [License](#license)

<!-- /MarkdownTOC -->


<a name="vera-解决什么问题"></a>
# Vera 解决什么问题
Redis 在点我达内部得到了广泛的使用，根据运维数据统计，整个点我达全部 Redis Master的读写请求在每秒 60W+，其中不包括Slave的读请求，很多业务甚至会将 Redis 当成内存数据库使用。这样，就对 Redis 多数据中心提出了很大的需求，一是为了提升可用性，解决数据中心 DR(Disaster Recovery) 问题，二是公司业务实现异地多活，不同机房有自己的数据中心，每个数据中心仅读写当前数据中心的数据，但各数据中心的Redis数据需要同步，允许存在一定的延时，在这样的需求下，Vera 应运而生, 一定程度上，Vera实现了Redis Cluster的多主数据同步。  

为了方便描述，后面用 DC 代表数据中心 (Data Center)。

<a name="系统详述"></a>
# 系统详述
<a name="整体架构"></a>
## 整体架构
整体架构图如下所示：  
![design](https://raw.github.com/DianwodaCompany/vera/master/doc/image/total.jpg)  

- Console 用来提供用户界面，供用户进行Redis集群侦听配置和各Piper实例的同步配置。
- NamerServer 各Piper信息与各任务实现情况的集中维护，主要是通过Piper的心跳上报；
- Piper 最主要的结点，主要功能如下：1) Redis命令存储：通过侦听RedisCluster中master结点产生的Redis命令存储到本地文件；2) 同步其它Piper的数据并写入本地Redis Master中； 所以Piper有两种角色，一种是数据产生者，侦听Redis Master并获取新数据，提供给其它需要同步该RedisMaster的Piper, 第二种是数据消费者，同步其它Piper的数据并消费该数据，即写入Redis;

<a name="redis-多主数据同步问题"></a>
## Redis 多主数据同步问题
多数据中心首先要解决的是数据同步问题，即数据如何从一个 DC 传输到另外一个 DC。我们决定采用伪 slave 的方案，即实现 Redis 协议，伪装成为 Redis slave，让 Redis master 推送数据至伪 slave。这个伪 slave的功能是在piper中实现，考虑到目前绝大多数公司都是采用Redis Sentinel架构来搭建Redis集群，所以Vera通过传入Sentinel集群和Redis Master名字来自动获取Redis Master的IP和端口，从页实现数据侦听功能，如下图所示：  
![keepers](https://raw.github.com/DianwodaCompany/vera/master/doc/image/pipers.jpg)  

使用 piper 带来的优势  

- 减少 master 同步次数  
如果异地机房 redis slave 直接连向 redis master，多个 slave 会导致 master 多次同步，而 piper可以将增量数据存储在本地，通过机房间的piper的数据同步，异地机房的piper写入异地机房的redis master, 利用redis自身的主从同步，redis slave再从master获取数据。
- 网络异常时减少全量同步  
piper 将 Redis 日志数据缓存到磁盘，这样，可以缓存大量的日志数据 (Redis 将数据缓存到内存 ring buffer，容量有限)，当数据中心之间的网络出现较长时间异常时仍然可以续传日志数据。  
- 更加灵活的数据传输方式
1.piper之间传输协议可以自定义，方便支持压缩 (目前暂未支持)。2.根据业务可以配置每次启动是从最新数据开始同步，还是从上一次的同步位置开始同步；
- 安全性提升  
多个机房之间的数据传输往往需要通过公网进行，这样数据的安全性变得极为重要，piper 之间的数据传输也可以加密 (暂未实现)，提升安全性。

## 高可用
<a name="Vear-系统高可用"></a>
### Vera 系统高可用
如果 Piper 挂掉，多数据中心之间的数据传输可能会中断，解决这个问题，Vera设计了两种解决方式：
1) Piper 有主备两个节点，备节点实时从主节点复制数据，当主节点挂掉后，备节点会被提升为主节点，代替主节点进行服务(主备功能将在下一个版本实现)。
2) 预启动一个Piper作为备用，操作后台，将挂断的Piper上面的职能迁移到该备用Piper上面，从而恢复数据同步；
<a name="redis-自身高可用"></a>
### Redis 自身高可用
Redis 也可能会挂，Redis 本身提供哨兵 (Sentinel) 机制保证集群的高可用。

## 测试数据
<a name="延时测试"></a>
### 延时测试
#### 测试方案
测试方式如下图所示。新数据写入Redis Master，Piper1侦听到RedisMaster事件将数据写入本地文件，同时通知其它订阅该Piper的其它Piper来获取新数据，Piper2请求Piper1获取数据后，再写入Redis Master2, 整个测试延时时间为 t1+t2+t3。  
![test](https://raw.github.com/DianwodaCompany/vera/master/doc/image/delay.jpg)  
#### 测试数据
因为Piper间获取新增数据有两种方式: 1. Consumer定时向Provider请求数据，采用pull的方式；2. 为避免没数据时频繁的pull, Provider会hold该请求，如果这时有新增数据，会重启该请求,使得Consumer能在第一时间获取该数据；
在点我达生产环境单个机房进行了测试，大部分情况基本上是在ms级别的范围内。但如果由于Provider hold请求，导致Consumer超时未得到响应，此时Consumer会等待一些时间重新pull, 默认是10秒。

<a name="license"></a>
# License
The project is licensed under the [Apache 2 license](https://github.com/DianwodaCompany/vera/master/LICENSE).
