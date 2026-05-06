# Async Profiler 配置指南

## Async Profiler 简介
Async Profiler 是一个低开销的 Java 采样分析器，支持 CPU、内存、锁等多种分析模式。

## 安装

### Linux
```bash
# 下载最新版本
curl -LO https://github.com/jvm-profiling-tools/async-profiler/releases/download/v2.9/async-profiler-2.9-linux-x64.tar.gz
tar -xzf async-profiler-2.9-linux-x64.tar.gz
```

### Windows
```powershell
# 下载最新版本
Invoke-WebRequest -Uri "https://github.com/jvm-profiling-tools/async-profiler/releases/download/v2.9/async-profiler-2.9-windows-x64.zip" -OutFile "async-profiler.zip"
Expand-Archive -Path "async-profiler.zip" -DestinationPath "."
```

### macOS
```bash
curl -LO https://github.com/jvm-profiling-tools/async-profiler/releases/download/v2.9/async-profiler-2.9-macos-x64.tar.gz
tar -xzf async-profiler-2.9-macos-x64.tar.gz
```

## 基本使用

### CPU 分析
```bash
# 采样 60 秒
./profiler.sh -d 60 -f cpu.html <pid>

# 采样并输出火焰图
./profiler.sh -d 60 -e cpu -f flamegraph.html <pid>

# 指定采样频率
./profiler.sh -d 60 -e cpu -i 1000 -f cpu.html <pid>
```

### 内存分配分析
```bash
# 分析内存分配
./profiler.sh -d 60 -e alloc -f alloc.html <pid>

# 分析每个对象的分配大小
./profiler.sh -d 60 -e alloc -s -f alloc-size.html <pid>
```

### 锁分析
```bash
# 分析锁竞争
./profiler.sh -d 60 -e lock -f lock.html <pid>
```

### Wall-clock 分析
```bash
# 分析所有线程的运行时间（包括阻塞）
./profiler.sh -d 60 -e wall -f wall.html <pid>
```

## OAISS Chain 分析脚本

### profile-cpu.sh
```bash
#!/bin/bash

ASYNC_PROFILER_HOME="/opt/async-profiler"
APP_NAME="oaiss-chain-backend"
OUTPUT_DIR="/var/log/oaiss/profiler"

PID=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "Application $APP_NAME not found"
    exit 1
fi

mkdir -p $OUTPUT_DIR
TIMESTAMP=$(date +%Y%m%d%H%M%S)
OUTPUT_FILE="$OUTPUT_DIR/cpu-$TIMESTAMP.html"

echo "Starting CPU profiling for $APP_NAME (PID: $PID)..."
$ASYNC_PROFILER_HOME/profiler.sh -d 60 -e cpu -f $OUTPUT_FILE $PID

echo "CPU profile saved to: $OUTPUT_FILE"
```

### profile-memory.sh
```bash
#!/bin/bash

ASYNC_PROFILER_HOME="/opt/async-profiler"
APP_NAME="oaiss-chain-backend"
OUTPUT_DIR="/var/log/oaiss/profiler"

PID=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "Application $APP_NAME not found"
    exit 1
fi

mkdir -p $OUTPUT_DIR
TIMESTAMP=$(date +%Y%m%d%H%M%S)
OUTPUT_FILE="$OUTPUT_DIR/alloc-$TIMESTAMP.html"

echo "Starting memory allocation profiling for $APP_NAME (PID: $PID)..."
$ASYNC_PROFILER_HOME/profiler.sh -d 60 -e alloc -f $OUTPUT_FILE $PID

echo "Memory profile saved to: $OUTPUT_FILE"
```

### profile-all.sh
```bash
#!/bin/bash

ASYNC_PROFILER_HOME="/opt/async-profiler"
APP_NAME="oaiss-chain-backend"
OUTPUT_DIR="/var/log/oaiss/profiler"

PID=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "Application $APP_NAME not found"
    exit 1
fi

mkdir -p $OUTPUT_DIR
TIMESTAMP=$(date +%Y%m%d%H%M%S)

echo "Starting comprehensive profiling for $APP_NAME (PID: $PID)..."

# CPU
echo "Profiling CPU..."
$ASYNC_PROFILER_HOME/profiler.sh -d 30 -e cpu -f "$OUTPUT_DIR/cpu-$TIMESTAMP.html" $PID

# Memory
echo "Profiling memory allocation..."
$ASYNC_PROFILER_HOME/profiler.sh -d 30 -e alloc -f "$OUTPUT_DIR/alloc-$TIMESTAMP.html" $PID

# Lock
echo "Profiling lock contention..."
$ASYNC_PROFILER_HOME/profiler.sh -d 30 -e lock -f "$OUTPUT_DIR/lock-$TIMESTAMP.html" $PID

echo "All profiles saved to: $OUTPUT_DIR"
```

## 高级用法

### 指定采样事件
```bash
# CPU 周期
./profiler.sh -d 60 -e cycles -f cycles.html <pid>

# 缓存未命中
./profiler.sh -d 60 -e cache-misses -f cache.html <pid>

# 分支预测失败
./profiler.sh -d 60 -e branch-misses -f branch.html <pid>
```

### 过滤特定包/类
```bash
# 只分析 OAISS Chain 代码
./profiler.sh -d 60 -e cpu --include 'com.oaiss.chain.*' -f filtered.html <pid>

# 排除特定包
./profiler.sh -d 60 -e cpu --exclude 'java.*,javax.*' -f no-jdk.html <pid>
```

### 输出格式
```bash
# HTML 火焰图（默认）
./profiler.sh -d 60 -e cpu -f output.html <pid>

# 纯文本
./profiler.sh -d 60 -e cpu -f output.txt --fmt text <pid>

# collapsed 格式（可导入其他工具）
./profiler.sh -d 60 -e cpu -f output.collapsed --fmt collapsed <pid>

# JFR 格式
./profiler.sh -d 60 -e cpu -f output.jfr --fmt jfr <pid>
```

## 与 JFR 结合

Async Profiler 可以输出 JFR 格式，然后用 JMC 分析：

```bash
# 生成 JFR 格式
./profiler.sh -d 60 -e cpu -f output.jfr --fmt jfr <pid>

# 用 JMC 打开
jmc output.jfr
```

## 性能影响

| 模式 | 开销 | 说明 |
|------|------|------|
| CPU | < 5% | 默认采样频率 |
| Alloc | 5-15% | 取决于分配速率 |
| Lock | < 3% | 仅记录锁事件 |
| Wall | < 5% | 包含阻塞时间 |

## 常见问题诊断

### 问题1：CPU 使用率高
```bash
# 1. CPU 分析
./profiler.sh -d 60 -e cpu -f cpu-high.html <pid>

# 2. 查看热点方法
# 打开火焰图，找到最宽的部分
```

### 问题2：频繁 GC
```bash
# 1. 内存分配分析
./profiler.sh -d 60 -e alloc -f alloc-gc.html <pid>

# 2. 找到分配最多的方法
# 打开火焰图，找到分配热点
```

### 问题3：响应慢
```bash
# 1. Wall-clock 分析
./profiler.sh -d 60 -e wall -f wall-slow.html <pid>

# 2. 锁分析
./profiler.sh -d 60 -e lock -f lock-slow.html <pid>
```

## 参考链接

- GitHub：https://github.com/jvm-profiling-tools/async-profiler
- 教程：https://krzysztofslusarski.github.io/2022/12/12/async-manual.html
