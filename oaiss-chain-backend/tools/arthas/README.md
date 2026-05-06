# Arthas 配置和使用指南

## Arthas 简介
Arthas 是阿里巴巴开源的 Java 诊断工具，可以在不重启 JVM 的情况下进行问题诊断。

## 安装方式

### 方式一：直接下载
```bash
# 下载最新版本
curl -O https://arthas.aliyun.com/download/latest_version
unzip latest_version -d arthas
```

### 方式二：使用脚本安装
```bash
# Linux/Mac
curl -L https://arthas.aliyun.com/install.sh | sh

# Windows
# 下载 https://arthas.aliyun.com/download/latest_version 并解压
```

## 启动 Arthas

### 基本启动
```bash
# 查找 Java 进程
java -jar arthas-boot.jar

# 直接指定 PID
java -jar arthas-boot.jar <pid>

# 远程连接
java -jar arthas-boot.jar --target-ip 192.168.1.100
```

### OAISS Chain 启动脚本
```bash
#!/bin/bash
# arthas-connect.sh

APP_NAME="oaiss-chain-backend"
PID=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "Application $APP_NAME not found"
    exit 1
fi

echo "Connecting to $APP_NAME (PID: $PID)..."
java -jar arthas-boot.jar $PID
```

## 常用命令

### 1. 查看应用信息
```bash
# 查看 JVM 信息
dashboard

# 查看线程信息
thread

# 查看 CPU 使用率最高的线程
thread -n 3

# 查看死锁
thread -b
```

### 2. 类和方法诊断
```bash
# 查看类信息
sc -d com.oaiss.chain.service.CarbonReportService

# 查看方法信息
sm -d com.oaiss.chain.service.CarbonReportService createReport

# 反编译类
jad com.oaiss.chain.service.CarbonReportService
```

### 3. 方法监控
```bash
# 监控方法执行时间
monitor com.oaiss.chain.service.CarbonReportService createReport

# 监控方法调用路径
stack com.oaiss.chain.service.CarbonReportService createReport

# 查看方法参数和返回值
watch com.oaiss.chain.service.CarbonReportService createReport '{params, returnObj}' -x 2

# 记录方法调用
tt -t com.oaiss.chain.service.CarbonReportService createReport
```

### 4. 性能分析
```bash
# 方法调用耗时统计
profiler start

# 采样一段时间后
profiler stop

# 生成火焰图
profiler start --event cpu --duration 60
profiler stop --format html
```

### 5. 内存分析
```bash
# 查看堆内存使用
heapdump /tmp/heapdump.hprof

# 查看类加载信息
classloader

# 查看 JVM 属性
vmtool --action getSystemProperty --propertyName java.version
```

## OAISS Chain 常用诊断场景

### 场景1：接口响应慢
```bash
# 1. 找到慢方法
trace com.oaiss.chain.controller.* * '#cost > 100'

# 2. 监控具体方法
monitor -c 5 com.oaiss.chain.service.CarbonReportService createReport

# 3. 分析调用链
stack com.oaiss.chain.service.CarbonReportService createReport '#cost > 500'
```

### 场景2：内存泄漏
```bash
# 1. 查看大对象
vmtool --action getHeapMemoryUsage

# 2. 导出堆转储
heapdump --live /tmp/oaiss-heap-live.hprof

# 3. 分析类实例数量
sc -d com.oaiss.chain.entity.*
```

### 场景3：线程阻塞
```bash
# 1. 查看阻塞线程
thread -b

# 2. 查看线程堆栈
thread <thread-id>

# 3. 监控线程状态
thread -n 5 -i 1000
```

## Web Console

Arthas 支持 Web Console，可以通过浏览器访问：

```bash
# 启动时指定端口
java -jar arthas-boot.jar --telnet-port 3658 --http-port 8563

# 访问地址
http://localhost:8563
```

## 配置文件

### arthas.properties
```properties
# Telnet 端口
telnetPort=3658

# HTTP 端口
httpPort=8563

# IP 白名单
ipWhitelist=127.0.0.1,192.168.1.*

# 会话超时时间（秒）
sessionTimeout=300

# 执行超时时间（毫秒）
executeTimeout=30000
```

## 注意事项

1. **生产环境使用**：建议使用 IP 白名单限制访问
2. **性能影响**：trace/monitor 命令有一定性能开销，谨慎使用
3. **权限控制**：确保只有授权人员可以连接 Arthas
4. **日志记录**：开启操作日志，便于审计

## 参考链接

- 官方文档：https://arthas.aliyun.com/doc/
- GitHub：https://github.com/alibaba/arthas
- 在线教程：https://arthas.aliyun.com/doc/arthas-tutorials.html
