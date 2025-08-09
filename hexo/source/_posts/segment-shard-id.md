---
title: 号段ID与业务全局ID设计
categories:
  - ["分布式系统"]
---

# 号段ID
> 顾名思义，通过存储（如rdms、kv-store等）最大ID，由节点向存储申请一段连续ID段到本地分配，来满足分布式系统对高吞吐生成全局唯一ID的需求。

### 优化点
> 预加载
>  - 加载ID段长度：MAX(申请下限,MIN(申请上限, 分片ID时间桶（如小时/分钟等）QPS * 希望缓冲时长(秒)))
>    - 时间桶QPS：时间桶开始时间戳+LongAddr(递增随机到数组一个元素，查时sum，适合写多读少统计)，当前时间>时间桶范围时，cas初始化开始时间戳和LongAddr
>  - 双ID段：正式+预备，正式ID段分配率≥阈值，且预备ID段未初始，则线程cas竞争预加载ID段（双重检测） 

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
- 哈希算法：混合xxhash64+murmurhash64
- 哈希环：16位(65536个虚拟节点)，每个虚拟节点对应一个物理分片
- ID结构：64位8字节，高1位0（正数），中16位为分片ID（哈希环虚拟节点），低47位为号段ID

### 生成逻辑
``` java
short shardGene = (xxhash64(所属业务ID) ^ murmurhash64(所属业务ID)) & 0xFFFF;
long orderNo = shardGene << 47 | (segment.generate(1) & 0x7FFFFFFFFFFF);
```

### 路由
``` java
TreeMap<Short, Shard> shardMap = 配置中心热加载;
short shardGene = (req.getOrderNo() >> 47) & 0xFFFF;
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
  - 内部出入参：涉及order_id(内部用)和order_no(对外,128位)，DB只存储order_id，order_no可解密成order_id
  - 对外（如openApi）出入参：只涉及order_no
  - order_no：分片信息不加密
    - 64-128位ID：号段ID用AES-CTR加密，绑定高16位分片ID作为初始向量（IV）,密钥版本记录在表里(仅用于审计)，查询时无解密路由分片，表存混淆后的order_no（建索引）

### 溢出
- 号段ID溢出：
  - ID长度增加到128位（16字节）:
    - 路由: 按字节长度选不同位移取分片信息，兼容新老位长的ID
    - 表shard_gene字段：采取CAS WHEN LENGTH区分新老ID
    - 表主键：类型改成VARBINARY(16)
### demo ddl
``` sql
CREATE TABLE `test_order` (
  `order_id` BIGINT(20) UNSIGNED NOT NULL COMMENT '订单号: ((xxhash(buyer_uid)^murmurhash(buyer_uid))&0xFFFF)<<47|(号段ID&0x7FFFFFFFFFFF)',
  `order_no` BIGINT(20) NOT NULL, // 或类型改成VARBINARY(16)
  `order_no_kv` INT NOT NULL, // 密钥版本 
  `buyer_uid` BIGINT(20) NOT NULL,
  `shard_gene` SMALLINT UNSIGNED GENERATED ALWAYS AS (`order_no` >> 47) VIRTUAL COMMENT '分片基因冗余，便于查询',
  PRIMARY KEY (`order_id`) USING BTREE,
  INDEX `idx_order_no` (`order_no`) USING BTREE
  INDEX `idx_buyer_uid` (`buyer_uid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
```
> 每个虚拟节点的订单号是趋势递增的，一段周期后mysql页分裂会趋于稳定
