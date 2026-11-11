# ============================================================
# Java 服务通用轻量镜像（agent-runtime / mcp-product-server / mcp-knowledge-server）
#
# 说明：
#   1. 日常本地开发与 Docker 编排：
#      推荐直接使用 docker-compose.yml 挂载本地编译好的 target/app.jar 到基础镜像运行，
#      无需在 Docker 内进行漫长耗时的 Maven 下载与打包，实现秒级启动与即时更新。
#   2. 单体独立镜像打包（CI/CD、镜像仓库推送或生产独立部署）：
#      在宿主机先执行构建（如运行 ./build-all.sh 或在各模块下执行 mvn clean package -DskipTests），
#      然后使用此轻量 Dockerfile 直接基于已有产物封装：
#
#      构建示例（在 enterprise-ai-agent 根目录下执行）：
#        docker build -f docker/Dockerfile.java --build-arg MODULE=agent-runtime -t agent-runtime:latest .
#        docker build -f docker/Dockerfile.java --build-arg MODULE=mcp-product-server -t mcp-product-server:latest .
#        docker build -f docker/Dockerfile.java --build-arg MODULE=mcp-knowledge-server -t mcp-knowledge-server:latest .
# ============================================================

FROM eclipse-temurin:17-jre-jammy

ARG MODULE
WORKDIR /app

# 直接将本地预编译的统一产物 app.jar 拷入镜像
COPY ${MODULE}/target/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
