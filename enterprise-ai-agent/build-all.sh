#!/usr/bin/env bash
# ============================================================
# Enterprise AI Agent - 全模块一键手动打包脚本 (Linux / macOS)
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "============================================================"
echo "  Enterprise AI Agent - 开始全模块本地打包"
echo "============================================================"

# 1. agent-runtime
echo ""
echo "[1/4] 正在构建 agent-runtime 模块..."
(cd "$SCRIPT_DIR/agent-runtime" && mvn clean package -DskipTests)
if [ ! -f "$SCRIPT_DIR/agent-runtime/target/app.jar" ]; then
    echo "[ERROR] 构建完成但未找到 agent-runtime/target/app.jar"
    exit 1
fi
echo "[✓] agent-runtime 构建成功: agent-runtime/target/app.jar"

# 2. mcp-product-server
echo ""
echo "[2/4] 正在构建 mcp-product-server 模块..."
(cd "$SCRIPT_DIR/mcp-product-server" && mvn clean package -DskipTests)
if [ ! -f "$SCRIPT_DIR/mcp-product-server/target/app.jar" ]; then
    echo "[ERROR] 构建完成但未找到 mcp-product-server/target/app.jar"
    exit 1
fi
echo "[✓] mcp-product-server 构建成功: mcp-product-server/target/app.jar"

# 3. mcp-knowledge-server
echo ""
echo "[3/4] 正在构建 mcp-knowledge-server 模块..."
(cd "$SCRIPT_DIR/mcp-knowledge-server" && mvn clean package -DskipTests)
if [ ! -f "$SCRIPT_DIR/mcp-knowledge-server/target/app.jar" ]; then
    echo "[ERROR] 构建完成但未找到 mcp-knowledge-server/target/app.jar"
    exit 1
fi
echo "[✓] mcp-knowledge-server 构建成功: mcp-knowledge-server/target/app.jar"

# 4. web
echo ""
echo "[4/4] 正在构建 web 前端模块..."
(cd "$SCRIPT_DIR/web" && npm install && npm run build)
if [ ! -d "$SCRIPT_DIR/web/dist" ]; then
    echo "[ERROR] 构建完成但未找到 web/dist 目录"
    exit 1
fi
echo "[✓] web 构建成功: web/dist/"

echo ""
echo "============================================================"
echo "  [SUCCESS] 所有模块打包完成！"
echo "  产物清单："
echo "    - agent-runtime/target/app.jar"
echo "    - mcp-product-server/target/app.jar"
echo "    - mcp-knowledge-server/target/app.jar"
echo "    - web/dist/"
echo ""
echo "  接下来可直接运行："
echo "    docker compose up -d"
echo "============================================================"
