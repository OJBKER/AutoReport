git项目后请进行<br>



初始化设置:<br>
1.在根目录执行mvn compile<br>
2.进入前端目录frontend，安装前端依赖：<br>
cd frontend<br>
npm install<br>



启动项目_本地测试:<br>
1.运行Spring Boot<br>
mvn clean package
点击run-oauth2-dev.bat
2.运行前端开发服务器<br>
进入frontend文件夹
命令npm run dev




生成项目:<br>
1.在 frontend 目录下运行：<br>
npm run build<br>
生成 frontend/dist 文件夹<br>
2.将 dist 文件夹里的所有内容复制到后端项目的 src/main/resources/static 目录。<br>
3.在根目录运行<br>
mvn clean package<br>
