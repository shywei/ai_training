# ============================================================
# Java 服务通用镜像（agent-runtime / mcp-product-server / mcp-knowledge-server）
# 通过 MODULE 构建参数指定模块，三个服务共用一份 Dockerfile
#
#   docker build -f docker/Dockerfile.java --build-arg MODULE=agent-runtime .
# ============================================================

FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /build

# 先只拷 pom，利用镜像层缓存依赖（代码改动不触发重新下载）
COPY ${MODULE}/pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline -DskipTests || true

# 再拷源码打包
COPY ${MODULE}/src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
