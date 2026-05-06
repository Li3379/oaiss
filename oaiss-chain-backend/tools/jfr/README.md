# JFR (Java Flight Recorder) 配置指南

## JFR 简介
Java Flight Recorder 是 JDK 内置的低开销事件记录框架，适合生产环境持续监控。

## 启动 JFR

### 方式一：启动时开启 JFR
```bash
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
     -XX:FlightRecorderOptions=stackdepth=256 \
     -jar oaiss-chain-backend.jar
```

### 方式二：运行时启动 JFR
```bash
# 使用 jcmd 启动录制
jcmd <pid> JFR.start name=oaiss_recording duration=60s filename=recording.jfr

# 使用 jcmd 停止录制
jcmd <pid> JFR.stop name=oaiss_recording
```

### 方式三：通过 JMX 启动
```bash
java -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9010 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -jar oaiss-chain-backend.jar
```

## JFR 配置文件

### oaiss-jfr-config.jfc
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration version="2.0" label="OAISS Chain JFR Config" 
                description="Custom JFR configuration for OAISS Chain Backend"
                provider="OAISS Team">

  <!-- CPU 事件 -->
  <event name="jdk.CPULoad">
    <setting name="enabled" control="enabled-cpu">true</setting>
    <setting name="period">1s</setting>
  </event>

  <!-- JVM 信息 -->
  <event name="jdk.JVMInformation">
    <setting name="enabled">true</setting>
  </event>

  <!-- GC 事件 -->
  <event name="jdk.GCHeapSummary">
    <setting name="enabled" control="enabled-gc">true</setting>
  </event>

  <event name="jdk.GCPhasePause">
    <setting name="enabled" control="enabled-gc">true</setting>
  </event>

  <event name="jdk.GCPhasePauseLevel1">
    <setting name="enabled" control="enabled-gc">true</setting>
  </event>

  <!-- 线程事件 -->
  <event name="jdk.ThreadStart">
    <setting name="enabled" control="enabled-thread">true</setting>
  </event>

  <event name="jdk.ThreadEnd">
    <setting name="enabled" control="enabled-thread">true</setting>
  </event>

  <event name="jdk.ThreadAllocationStatistics">
    <setting name="enabled" control="enabled-thread">true</setting>
    <setting name="period">10s</setting>
  </event>

  <!-- 方法采样 -->
  <event name="jdk.ExecutionSample">
    <setting name="enabled" control="enabled-method">true</setting>
    <setting name="period">20ms</setting>
  </event>

  <!-- 异常事件 -->
  <event name="jdk.JavaErrorThrow">
    <setting name="enabled" control="enabled-exception">true</setting>
  </event>

  <event name="jdk.JavaExceptionThrow">
    <setting name="enabled" control="enabled-exception">true</setting>
  </event>

  <!-- 文件 I/O -->
  <event name="jdk.FileRead">
    <setting name="enabled" control="enabled-io">true</setting>
  </event>

  <event name="jdk.FileWrite">
    <setting name="enabled" control="enabled-io">true</setting>
  </event>

  <!-- 网络 I/O -->
  <event name="jdk.SocketRead">
    <setting name="enabled" control="enabled-io">true</setting>
  </event>

  <event name="jdk.SocketWrite">
    <setting name="enabled" control="enabled-io">true</setting>
  </event>

  <!-- 控制选项 -->
  <control>
    <selection name="enabled-cpu" default="true" label="Enable CPU Events">
      <option name="true">true</option>
      <option name="false">false</option>
    </selection>

    <selection name="enabled-gc" default="true" label="Enable GC Events">
      <option name="true">true</option>
      <option name="false">false</option>
    </selection>

    <selection name="enabled-thread" default="true" label="Enable Thread Events">
      <option name="true">true</option>
      <option name="false">false</option>
    </selection>

    <selection name="enabled-method" default="true" label="Enable Method Profiling">
      <option name="true">true</option>
      <option name="false">false</option>
    </selection>

    <selection name="enabled-exception" default="true" label="Enable Exception Events">
      <option name="true">true</option>
      <option name="false">false</option>
    </selection>

    <selection name="enabled-io" default="true" label="Enable I/O Events">
      <option name="true">true</option>
      <option name="false">false</option>
    </selection>
  </control>
</configuration>
```

## 常用录制场景

### 场景1：性能分析（60秒）
```bash
jcmd <pid> JFR.start \
  name=perf_analysis \
  duration=60s \
  filename=perf-$(date +%Y%m%d%H%M%S).jfr \
  settings=oaiss-jfr-config.jfc
```

### 场景2：问题诊断（持续录制）
```bash
# 开始持续录制
jcmd <pid> JFR.start \
  name=continuous \
  maxsize=100m \
  maxage=1h \
  disk=true \
  filename=/var/log/oaiss/jfr/continuous.jfr

# 问题发生后停止并保存
jcmd <pid> JFR.stop name=continuous
```

### 场景3：GC 分析
```bash
jcmd <pid> JFR.start \
  name=gc_analysis \
  duration=300s \
  filename=gc-analysis.jfr \
  settings=profile
```

## 分析 JFR 文件

### 使用 JDK Mission Control (JMC)
1. 下载 JMC：https://www.oracle.com/java/technologies/jdk-mission-control.html
2. 打开 .jfr 文件
3. 查看各种事件视图

### 使用命令行工具
```bash
# 打印摘要
jfr print recording.jfr

# 打印特定事件
jfr print --events CPULoad,GCHeapSummary recording.jfr

# 转换为 JSON
jfr print --json recording.jfr > recording.json
```

## OAISS Chain 集成脚本

### start-jfr.sh
```bash
#!/bin/bash

APP_NAME="oaiss-chain-backend"
PID=$(ps -ef | grep $APP_NAME | grep -v grep | awk '{print $2}')
JFR_DIR="/var/log/oaiss/jfr"
CONFIG_FILE="/opt/oaiss/tools/jfr/oaiss-jfr-config.jfc"

if [ -z "$PID" ]; then
    echo "Application $APP_NAME not found"
    exit 1
fi

mkdir -p $JFR_DIR

TIMESTAMP=$(date +%Y%m%d%H%M%S)
JFR_FILE="$JFR_DIR/oaiss-$TIMESTAMP.jfr"

echo "Starting JFR recording for $APP_NAME (PID: $PID)..."
jcmd $PID JFR.start \
    name=oaiss_recording \
    duration=300s \
    filename=$JFR_FILE \
    settings=$CONFIG_FILE

echo "JFR recording will be saved to: $JFR_FILE"
```

## 性能影响

| 配置 | 开销 | 适用场景 |
|------|------|----------|
| default | < 1% | 生产环境持续监控 |
| profile | 1-2% | 性能分析 |
| oaiss-jfr-config | 1-2% | OAISS 自定义监控 |

## 参考链接

- JFR 官方文档：https://docs.oracle.com/javacomponents/jmc-5-4/jfr-runtime-guide/about.htm
- JMC 下载：https://www.oracle.com/java/technologies/jdk-mission-control.html
