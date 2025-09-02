---
title: 号段ID与业务全局ID
date: 2025-08-09 13:29:00 +0800
categories:
  - ["分布式系统"]
---

# 号段
> 顾名思义，通过存储（如rdms、kv-store等）最大ID，由节点向存储申请一段连续号段到本地分配，来满足分布式系统对高吞吐生成全局唯一ID的需求。

### 优化点
> 预加载
> - 加载号段长度：MAX(步长下限,MIN(步长上限, 消耗（上次步长/耗时）平均QPS * 希望缓冲时长(秒)))
> - 双号段：正式+预备，正式号段分配率≥阈值，且预备号段未初始，则线程cas竞争预加载号段（双重检测） 

> 全局缓冲
> - Redis：双写Redis+DB(先Redis申请，无可分配号段则到DB申请号段并回写Redis)，核心使用`INCRBY`命令
> - 容灾
>   - DB短暂故障：节点向DB申请号段捕获一定量异常后，短期不再请求DB，由Redis自动扩充号段（version+1)，超期后节点尝试请求Redis，DB恢复后校验version+maxId同步Redis号段
>   - Redis短暂故障：请求>达到异常阈值>周期不请求>重试请求>捕获,以此循环

> 多计数器： 每1-N个一致性哈希虚拟节点用一个号段计数器，减少cas竞争率

--------------------------------------------------------------------------

# 业务全局ID

### 目标
- 生成速率 ≥ 百万ID/s
- 无时间回拨风险
- 弹性扩缩容
- 拆合冷热分片
- 同所属业务ID（如C端用户）数据(如订单)在同分片
- 迁移数据量低，且不中断服务

### 方案
- 一致性哈希算法：混合xxhash64+murmurhash64
- 哈希环：每个虚拟节点对应一个物理分片(一般N:1)，按负载/数据均匀情况映射不同虚拟节点到物理节点
- ID结构：
> ### 64位8字节(万亿级)
> - 1位：0(正数)
> - 14-16位分片基因：8,192-65,536个虚拟节点
> - 5-7位加密版本：32-128个，金融或军事级建议≥128个加密版本(如季度换密钥，32个版本能用8年，数据归档后可复用低版本号)
> - 42位号段ID：约43,980亿

> ### 128位16字节
> - 1位：0(正数)
> - 23位分片基因：8,388,608个虚拟节点
> - 20位加密版本：每天换密钥，能用3,000年
> - 84位号段ID：正常用不完...
> > 分片粒度更细，可细粒度的映射冷热虚拟节点到对应物理节点，更少的数据迁移解决数据倾斜问题

### 生成逻辑
``` java
short shardGene = (xxhash64(所属业务ID) ^ murmurhash64(所属业务ID)) & 0xFFFF;
long orderNo = shardGene << 48 | (segment.generate(1) & 0xFFFFFFFFFFFF);
```

### 路由
``` java
TreeMap<Short, Shard> shardMap = 配置中心热加载;
short shardGene = (req.getOrderNo() >> 48) & 0xFFFF;
Shard shard = shardMap.ceilingEntry(shardGene); // 查找分片
```

### 拆合冷热分片
- 路由表变更
  - 新旧物理分片按映射的虚拟节点，更新到路由表
- 数据迁移
  - 迁移：全量+增量同步源分片数据到目标分片
  - 切流：将源虚拟节点的流量，按比例(如模序号)滚动(如每次5%+1分钟监控)切到目标分片
    - 回退：实现数据双向同步，通过cas(version)控制并发更新

### 安全
- ID有序性
  - 加密算法
    - ChaCha20：低15位加密，分片基因(16位)
    - AES CTR：
  - 请求时，根据特定位标识，恢复位的位置

### 溢出
- 号段ID溢出：ID长度增加到128位（16字节），路由时判断ID字节长度选择不同位移方式

### demo ddl
``` sql
CREATE TABLE `test_order` (
	`order_id` BIGINT(20) NOT NULL COMMENT '订单id, ((xxhash(buyer_uid)^murmurhash(buyer_uid))&0xFFFF)<<48|(号段ID&0xFFFFFFFFFFFF)',
	`order_no` BIGINT(20) NOT NULL COMMENT '混淆的订单id，可选',
	`buyer_uid` BIGINT(20) NOT NULL COMMENT '买家uid',
	PRIMARY KEY (`order_id`) USING BTREE,
	INDEX `order_no` (`order_no`) USING BTREE,
	INDEX `buyer_uid` (`buyer_uid`) USING BTREE
) COLLATE='utf8mb4_bin' ENGINE=InnoDB;
```
> 每个虚拟节点的订单号是趋势递增的，一段周期后mysql页分裂会趋于稳定
