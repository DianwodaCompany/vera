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
    - [Redis 数据复制问题](#redis-数据复制问题)
    - [机房切换](#机房切换)
        - [切换流程](#切换流程)
    - [高可用](#高可用)
        - [XPipe 系统高可用](#xpipe-系统高可用)
        - [Redis 自身高可用](#redis-自身高可用)
    - [测试数据](#测试数据)
        - [延时测试](#延时测试)
    - [跨公网部署及架构](#跨公网部署及架构)
- [深入了解](#深入了解)
- [技术交流](#技术交流)
- [License](#license)

<!-- /MarkdownTOC -->


<a name="vera-解决什么问题"></a>
# Vera 解决什么问题
Redis 在点我达内部得到了广泛的使用，根据运维数据统计，整个点我达全部 Redis Master的读写请求在每秒 60W+，其中不包括Slave的读请求，很多业务甚至会将 Redis 当成内存数据库使用。这样，就对 Redis 多数据中心提出了很大的需求，一是为了提升可用性，解决数据中心 DR(Disaster Recovery) 问题，二是公司业务实现异地多活，不同机房有自己的数据中心，每个数据中心读写当前数据中心的数据，但各数据中心的Redis数据需要同步，允许存在一定的延时，在这样的需求下，Vera 应运而生 。  

为了方便描述，后面用 DC 代表数据中心 (Data Center)。

<a name="系统详述"></a>
# 系统详述
<a name="整体架构"></a>
## 整体架构
整体架构图如下所示：  
![design](https://raw.github.com/DianwodaCompany/vera/master/doc/image/total.jpg)  





