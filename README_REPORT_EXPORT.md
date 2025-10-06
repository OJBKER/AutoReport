# 报告导出功能说明

## 接口概览

1. 预览（结构化 JSON）
   - `POST /api/report/export/preview`
   - 请求体：
     ```json
     { "templateCode": 1, "data": { "experimentName": "实验一" } }
     ```
   - 返回：
     ```json
     {
       "success": true,
       "preview": {
         "meta": { "templateName": "软件需求分析实验报告", "templateCode": 1, "generatedAt": "2025-10-07T00:00:00" },
         "header": [ {"label":"实验名称","key":"experimentName","value":"实验一"}, ...],
         "sections": [ {"title":"一、实验需求分析","key":"experimentNameDetail","type":"text","value":"..."}, ... ]
       }
     }
     ```

2. 导出 DOCX
   - `POST /api/report/export`
   - 请求体同上
   - 响应：`application/octet-stream`（真实是 DOCX）；文件名：`report-<templateCode>.docx`

## 模板解析与排版规则
- 模板来源：`classpath: static/ReportTemplate/report_template.json`
- 目前忽略 `templateCode` 分支，仅加载该文件；未来可命名为 `report_template_<code>.json` 动态选择。
- `header` 数组：现已改为生成“封面页”（不再使用表格）。规则：
  1. 第一行：报告标题（模板 `templateName`），加粗 24 号，居中。
  2. 之后每个 header 项一行：`label：value`，14 号，居中，行距适度（SpacingAfter≈200）。
  3. 末尾追加生成时间行（斜体 12 号）。
  4. 自动插入分页符，分页后开始正文 sections。
- `sections`：
  - `type: text|textarea` -> 标题（加粗 14 号）+ 内容（按换行拆分为多段）。
  - `type: list` -> 标题 + 每条一段，前缀 `1. 2. ...`。

### 文档尾部（签署区）
导出时在正文末尾自动追加签署/确认区域（预览 JSON 不包含此部分）：
1. 标题行：`签署 / 确认：`
2. 三行填写项（加粗标签 + 下划线预留）：
  - 实验评分：____________
  - 指导教师签字：____________
  - 日期：____________________
下划线长度可在 `ReportExportService#addFooterLine` 中调整（最后一个日期行使用更长下划线方便填写）。

## 默认值与占位
- 前端未提供字段时使用模板 `default`（若存在）。
- 默认值也不存在或为空则在导出时显示 `(未填写)`。
- list 类型：将字符串按换行或数组元素拆分；空列表时生成一行 `(未填写)`。

## 预览与导出一致性
- 预览结构不包含“排版样式信息”，但字段顺序与正文内容一一对应：封面信息 = `header`，正文 = `sections`。导出时才应用封面页样式与分页。

## 错误处理
- 预览：`success=false` 且附 `error` 字段。
- 导出：返回 HTTP 500 + 文本内容 `EXPORT ERROR: <message>`。

## 扩展建议（后续可做）
- 按 `templateCode` 多模板路由。
- 占位符语法，例如在模板 JSON 中标记 `${studentNumber}` 自动替换。
- 图片 / 表格 / 复杂段落样式支持。
- 缓存解析后的模板结构，减少 IO。
- 使用更严格的输入校验（字段白名单、类型校验）。

## 前端适配
- `DocGenBlock.vue` 已直接兼容：
  - 预览：显示 JSON 原文（点击折叠）。
  - 导出：使用 `templateCode` 命名文件 `C<code>-timestamp.docx`（在组件内）。

## 开发测试
- 单元测试：`ReportExportServiceTest` 含两个简单用例（预览字段、生成 docx 结构）。
- 手动测试：
  ```bash
  curl -X POST http://localhost:8080/api/report/export/preview \
       -H "Content-Type: application/json" \
       -d '{"templateCode":1,"data":{"experimentName":"实验一"}}'
  ```

## 代码位置
- 服务类：`com.example.demo.service.ReportExportService`
- 控制器：`com.example.demo.controller.ReportExportController`
- 模板文件：`src/main/resources/static/ReportTemplate/report_template.json`

