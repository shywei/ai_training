@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================================
:: Enterprise AI Agent - 全模块一键手动打包脚本 (Windows)
:: ============================================================

set "PROJECT_ROOT=%~dp0"
cd /d "%PROJECT_ROOT%"

echo ============================================================
echo   Enterprise AI Agent - 开始全模块本地打包
echo ============================================================

:: 1. agent-runtime
echo.
echo [1/4] 正在构建 agent-runtime 模块...
cd /d "%PROJECT_ROOT%agent-runtime"
call mvn clean package -DskipTests
if errorlevel 1 goto error
if not exist "%PROJECT_ROOT%agent-runtime\target\app.jar" goto jar_missing_runtime
echo [✓] agent-runtime 构建成功: agent-runtime\target\app.jar

:: 2. mcp-product-server
echo.
echo [2/4] 正在构建 mcp-product-server 模块...
cd /d "%PROJECT_ROOT%mcp-product-server"
call mvn clean package -DskipTests
if errorlevel 1 goto error
if not exist "%PROJECT_ROOT%mcp-product-server\target\app.jar" goto jar_missing_product
echo [✓] mcp-product-server 构建成功: mcp-product-server\target\app.jar

:: 3. mcp-knowledge-server
echo.
echo [3/4] 正在构建 mcp-knowledge-server 模块...
cd /d "%PROJECT_ROOT%mcp-knowledge-server"
call mvn clean package -DskipTests
if errorlevel 1 goto error
if not exist "%PROJECT_ROOT%mcp-knowledge-server\target\app.jar" goto jar_missing_knowledge
echo [✓] mcp-knowledge-server 构建成功: mcp-knowledge-server\target\app.jar

:: 4. web
echo.
echo [4/4] 正在构建 web 前端模块...
cd /d "%PROJECT_ROOT%web"
call npm install
if errorlevel 1 goto error
call npm run build
if errorlevel 1 goto error
if not exist "%PROJECT_ROOT%web\dist\index.html" goto dist_missing
echo [✓] web 构建成功: web\dist\

:: 完成提示
cd /d "%PROJECT_ROOT%"
echo.
echo ============================================================
echo   [SUCCESS] 所有模块打包完成！
echo   产物清单：
echo     - agent-runtime\target\app.jar
echo     - mcp-product-server\target\app.jar
echo     - mcp-knowledge-server\target\app.jar
echo     - web\dist\
echo.
echo   接下来可直接运行：
echo     docker compose up -d
echo ============================================================
exit /b 0

:jar_missing_runtime
echo [ERROR] agent-runtime 构建完成但未找到 target\app.jar！
goto error

:jar_missing_product
echo [ERROR] mcp-product-server 构建完成但未找到 target\app.jar！
goto error

:jar_missing_knowledge
echo [ERROR] mcp-knowledge-server 构建完成但未找到 target\app.jar！
goto error

:dist_missing
echo [ERROR] web 构建完成但未找到 dist\index.html！
goto error

:error
cd /d "%PROJECT_ROOT%"
echo.
echo ============================================================
echo   [ERROR] 打包失败，请检查上方日志！
echo ============================================================
exit /b 1
