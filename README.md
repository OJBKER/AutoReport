git项目后请进行



初始化设置:
1.在根目录执行mvn compile
2.进入前端目录frontend，安装前端依赖：
cd frontend
npm install



启动项目:
1.运行Spring Boot
mvn spring-boot:run
2.运行前端开发服务器
npm run dev



生成项目:
1.在 frontend 目录下运行：
npm run build
生成 frontend/dist 文件夹
2.将 dist 文件夹里的所有内容复制到后端项目的 src/main/resources/static 目录。
3.在根目录运行
mvn clean package
