---
title: 请求合并案例
date: 2025-07-17 01:43:00 +0800
categories:
  - ["java"]
---

# 目的
> 提升高频随机请求的吞吐量，减轻被依赖方(如db、下游微服务等)压力，且不产生数据一致性问题

# 思路
> 按`请求特征+时间桶+请求数上限`归类，同类请求合并为一个批处理执行，执行完逐一告知各请求结果

# 优点
> 1. 充分利用特定场景的特性：批量吞吐量 > 随机请求吞吐量，减少额外开销  
>    `如：mysql insert-values 批量插入、同分片批量读写等`
> 2. 不侵入调用方

# 缺点
> 1. 线程上下文无法有效传递，导致部分功能无法生效，如本地事务、调用链等
> 2. 单次请求耗时有所增加：时间桶+批处理执行耗时

# 假代码
```java
Object key = "请求特征";
// map+多产单消定量队列
var map = new ConcurrentHashMap<Object, MpscArrayQueue>();
Queue queue;
int afterMargin; // 加入后队列剩余容量
var future = new CompletableFuture(); // 本次调用返回的future
while(true){
    if((queue=map.get(key)) == null) {
        queue = new MpscArrayQueue(队列容量);
        Queue oldQueue = map.putIfAbsent(key, queue); // cas
        if(oldQueue !=null){
            queue = oldQueue;
        }
    }
    if((idx=queue.offer(请求入参+future)) > 0) { // cas
        break; // 加入队列，跳出循环
    }
    // 队列满了
    map.remove(key, queue); // cas，将老队列从请求特征集合中移除，重新初始化
    Thread.onSpinWait(); // 自旋等待时，提示处理器优化，但线程状态不切换
}
if(afterMargin == 队列容量-1) { // 首个放入队列
    创建延时任务(时间桶刻度) {
        if(禁止队列生产 == false) { // cas
            return;
        }
        map.remove(key, queue); // cas删掉，重新初始化
        遍历队列; // MpscArrayQueue#offer()不提供返回余量，自行实现则要自旋遍历加入队列的数量
        执行批处理逻辑;
        结果逐一返回给对应future; // 返回结果也是一个cas
    }
}else if(afterMargin == 0 && 禁止队列生产 == true) { // 最后放入队列，且禁止成功(cas)
    map.remove(key, queue); // cas删掉，重新初始化
    尝试取消延时任务 // cas
    遍历队列; // MpscArrayQueue#offer()不提供返回余量，自行实现则要自旋遍历加入队列的数量
    执行批处理逻辑;
    结果逐一返回给对应future;  // 返回结果也是一个cas
}
return future;
```
> 上述假代码的线程安全，由7个cas+1个自旋解决：  
> - cas1: 将`队列` 初始化放到 `请求特征集合`
> - cas2: `请求参数`加入 `队列`
> - cas3: 加入`队列`失败(满/禁止生产)，将`队列`从`请求特征集合`中移除
> - cas4: 禁止队列生产
> - cas5: 禁止队列生产后，将`队列`从`请求特征集合`中移除
> - cas6: 尝试取消延时任务
> - cas7: 批处理逻辑执行后，将结果一一返回给队列的每个请求future
> - 自旋1: MpscArrayQueue#offer()不提供返回余量，自行实现，则要自旋遍历加入队列

# 真实代码
待上传maven中央仓库