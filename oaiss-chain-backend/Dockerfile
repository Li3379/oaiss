# =============================================
# 双碳链动系统后端 Dockerfile
# OAISS Chain Backend Dockerfile
# Author: OAISS Team
# =============================================

# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 复制pom.xml并下载依赖（利用Docker缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app

# 创建非root用户
RUN groupadd -r oaiss && useradd -r -g oaiss oaiss

# 复制构建产物
COPY --from=build /app/target/*.jar app.jar

# 设置权限
RUN chown -R oaiss:oaiss /app

# 切换到非root用户
USER oaiss

# 暴露端口
EXPOSE 8080

# JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
