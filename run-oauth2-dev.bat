@echo off
REM Windows 启动脚本，激活 oauth2 配置
setlocal
set JAR_PATH=target\demo-0.0.1-SNAPSHOT.jar
if not exist %JAR_PATH% (
	echo [ERROR] 未找到 %JAR_PATH%，请先运行 mvn package
	pause
	exit /b 1
)
set PROXY_PORT=7897
echo 正在以 oauth2 profile 启动 Spring Boot，并设置代理端口 %PROXY_PORT% ...
set JAVA_PROXY_OPTS=-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=%PROXY_PORT% -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=%PROXY_PORT%
java %JAVA_PROXY_OPTS% -jar %JAR_PATH% --spring.profiles.active=oauth2
echo.
echo Spring Boot 已退出。
pause
