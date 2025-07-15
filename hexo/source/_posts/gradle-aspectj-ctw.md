---
title: AspectJ在Gradle的CTW案例
categories:
  - ["java"]
---
`AspectJ`的运行方式主要有三种：  
1. **CTW(Compile Time Weaving)**
  > 编译时织入，依托 `构建工具(maven/gradle等)` 和 `AJC(AspectJ Compiler)`，在项目编译时修改字节码
2. **LTW(Load Time Weaving)**
  > 加载时织入，依托 `Java Agent` 机制，在 `ClassLoader` 加载类时修改字节码
3. **RTW(Run Time Weaving)**
  > 运行时织入，依托 `JDK代理`、`CGLIB` 等代理机制，在运行时动态修改字节码

---
以下主要记录 `LTW` 方式下的组合：**Gradle+AJC** 

# 1.Gradle配置
在项目 `settings.gradle` 中配置，统一管理该插件版本
```groovy
plugins {
    id 'io.freefair.settings.plugin-versions' version '版本'
}
```

在项目根目录 `build.gradle` 中配置
```groovy
buildscript {
    dependencies {
        classpath "org.aspectj:aspectjtools:版本"
    }
}
```
接着在 `项目/模块` 的 `build.gradle` 中配置
```groovy
plugins {
    id "io.freefair.aspectj.post-compile-weaving"
}
```
# 2.Idea配置
1. 安装Idea插件 `AspectJ`
2. 在Idea菜单`Project Structure` (即`项目结构`) >`Facet` 中，添加 `AspectJ` 并选择对应模块

# 3.注解式切面

与spring aop的使用方式类似
> 如 `@Around`、`@Before`、`@After`、`@Pointcut`、`@AfterReturning`、`@AfterThrowing` 等

主要不同点

1. **切面顺序**
- 同一Aspect类有多个 `PointCut` 时，按定义的顺序  
- 不同Aspect类有同一连结点时，顺序随机，除非在Aspect类中声明`Declare Precedence`
```java
package com.aa;
@Aspect
@DeclarePrecedence("com.aa.AspectA,com.bb.AspectB")
public class AspectA { ... }

package com.bb;
@Aspect
@DeclarePrecedence("com.bb.AspectB,com.cc.AspectC")
public class AspectB { ... }

package com.cc;
@Aspect
public class AspectC { ... }
```
上述三个切面的执行顺序为：`A > B > C`

2. **切面作用域**
> 默认为`切面静态单例`，可选 `perthis` 和 `pertarget`
> - **singleton**
> > 每个 `切面类` 有一个 `切面实例`
> >>注意！此时在spring中配置@Bean时，不能用`new 切面类()`，而是 `Aspects.aspectOf(切面类.class)`
> - **perthis**
> > 每个 `目标实例` 有一个 `切面实例`，`切面实例` 随着 `目标实例` 的 创建而创建、GC回收而回收
> - **pertarget**
> > 每个 `目标类` 各有一个 `切面实例`，同一 `目标类` 的 `多个实例`共用该类的`切面实例`
---

# 相关链接
[Freefair Gradle Plugin 8.14 Document](https://docs.freefair.io/gradle-plugins/8.14/reference/#_aspectj "Freefair Gradle Plugin 8.14 Document")