package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ReportExportService {
    private final ObjectMapper mapper = new ObjectMapper();

    public static class PreviewResult {
        public Map<String,Object> meta = new LinkedHashMap<>();
        public List<Map<String,Object>> header = new ArrayList<>();
        public List<Map<String,Object>> sections = new ArrayList<>();
    }


    private static final Map<Integer,String> TEMPLATE_CACHE = new HashMap<>();
    private static volatile boolean scanned = false;
    private static final String BASE_PATTERN = "classpath:/static/ReportTemplate/*.json";
    private static final String LEGACY_PATH = "static/ReportTemplate/report_template.json";

    private synchronized void scanIfNeeded() throws IOException {
        if(scanned) return;
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(BASE_PATTERN);
        for(Resource r : resources){
            if(!r.isReadable()) continue;
            try(InputStream in = r.getInputStream()){
                JsonNode root = mapper.readTree(in);
                JsonNode codeNode = root.get("templateCode");
                if(codeNode!=null && codeNode.canConvertToInt()){
                    int code = codeNode.asInt();
                    // 保存相对路径形式供 ClassPathResource 使用
                    String filename = Objects.requireNonNull(r.getFilename());
                    TEMPLATE_CACHE.put(code, "static/ReportTemplate/"+filename);
                }
            }catch(Exception ignore){ /* 单个失败忽略 */ }
        }
        scanned = true;
    }

    private JsonNode loadTemplate(int templateCode) throws IOException {
        scanIfNeeded();
        String path = TEMPLATE_CACHE.get(templateCode);
        if(path == null){
            // fallback legacy
            path = LEGACY_PATH;
        }
        ClassPathResource res = new ClassPathResource(path);
        if(!res.exists()){
            // 最终兜底：抛出明确异常
            throw new FileNotFoundException("模板未找到 (code="+templateCode+")，尝试路径: "+path);
        }
        try(InputStream in = res.getInputStream()){
            return mapper.readTree(in);
        }
    }

    /** 预览：组合结构但不输出二进制 */
    public PreviewResult buildPreview(Integer templateCode, Map<String,Object> data) throws IOException {
        int code = templateCode == null ? 1 : templateCode;
        JsonNode root = loadTemplate(code);
        PreviewResult pr = new PreviewResult();
        pr.meta.put("templateName", text(root,"templateName"));
        pr.meta.put("templateCode", code);
        pr.meta.put("generatedAt", LocalDateTime.now().toString());

        // header
        JsonNode header = root.path("header");
        if(header.isArray()) {
            for(JsonNode h : header) {
                String key = text(h,"key");
                Object value = valueOrDefault(data, key, h.path("default"));
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("label", text(h,"label"));
                row.put("key", key);
                row.put("value", value);
                pr.header.add(row);
            }
        }
        // sections
        JsonNode sections = root.path("sections");
        if(sections.isArray()) {
            for(JsonNode s : sections) {
                String key = text(s,"key");
                String type = text(s,"type");
                Object value = valueOrDefault(data, key, s.path("default"));
                Map<String,Object> sec = new LinkedHashMap<>();
                sec.put("title", text(s,"title"));
                sec.put("key", key);
                sec.put("type", type);
                sec.put("value", value);
                pr.sections.add(sec);
            }
        }
        return pr;
    }

    /** 基于 PreviewResult 生成一个简单的 HTML 预览（不含样式或仅内联基础样式）。 */
    public String buildHtmlPreview(Integer templateCode, Map<String,Object> data) throws IOException {
        PreviewResult pr = buildPreview(templateCode, data);
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"/>" +
                "<title>预览-"+escapeHtml(String.valueOf(pr.meta.getOrDefault("templateName","报告预览")))+"</title>" +
                "<style>body{font-family:-apple-system,Segoe UI,Helvetica,Arial,sans-serif;line-height:1.5;padding:24px;background:#f5f7fa;color:#1f2937;}h1{margin:0 0 18px;font-size:22px;}h2{margin:32px 0 12px;font-size:18px;border-bottom:1px solid #e2e8f0;padding-bottom:4px;}table{border-collapse:collapse;width:100%;margin:0 0 24px;font-size:14px;}th,td{border:1px solid #e2e8f0;padding:6px 8px;text-align:left;vertical-align:top;}th{background:#f1f5f9;}ul{margin:6px 0 16px 20px;padding:0;}li{margin:2px 0;}code{background:#e2e8f0;padding:2px 4px;border-radius:4px;font-size:12px;} .empty{color:#94a3b8;font-style:italic;} .footer{margin-top:48px;font-size:12px;color:#64748b;text-align:center;} .section-block{margin-bottom:28px;padding:16px 18px;background:#fff;border:1px solid #e2e8f0;border-radius:10px;box-shadow:0 1px 2px rgba(0,0,0,0.04);} .badge{display:inline-block;background:#3b82f6;color:#fff;font-size:12px;padding:2px 8px;border-radius:12px;margin-left:8px;} .meta-row{font-size:12px;color:#64748b;margin:0 0 24px;} pre{white-space:pre-wrap;word-break:break-word;} </style></head><body>");
        sb.append("<h1>"+escapeHtml(String.valueOf(pr.meta.getOrDefault("templateName","实验报告")))+"<span class=\"badge\">预览</span></h1>");
        sb.append("<div class=\"meta-row\">模板代码: "+pr.meta.get("templateCode")+" ｜ 生成时间: "+escapeHtml(String.valueOf(pr.meta.get("generatedAt")))+"</div>");
        // Header table
        if(!pr.header.isEmpty()){
            sb.append("<table><thead><tr><th style=\"width:160px;\">字段</th><th>内容</th></tr></thead><tbody>");
            for(Map<String,Object> h: pr.header){
                sb.append("<tr><td>"+escapeHtml(String.valueOf(h.get("label")))+"</td><td>"+renderValue(h.get("value"))+"</td></tr>");
            }
            sb.append("</tbody></table>");
        }
        // Sections
        for(Map<String,Object> s: pr.sections){
            sb.append("<div class=\"section-block\"><h2>"+escapeHtml(String.valueOf(s.get("title")))+"</h2>");
            String type = String.valueOf(s.get("type"));
            Object val = s.get("value");
            if("list".equalsIgnoreCase(type)){
                List<String> items = normalizeList(val);
                if(items.isEmpty()) sb.append("<div class=\"empty\">(未填写)</div>");
                else { sb.append("<ul>"); for(String it: items){ sb.append("<li>"+escapeHtml(it)+"</li>"); } sb.append("</ul>"); }
            } else {
                String text = stringify(val);
                if(text==null || text.isBlank()) sb.append("<div class=\"empty\">(未填写)</div>");
                else sb.append("<pre>"+escapeHtml(text)+"</pre>");
            }
            sb.append("</div>");
        }
        sb.append("<div class=\"footer\">此预览不代表最终 DOCX 排版，仅展示内容结构。</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String escapeHtml(String s){
        if(s==null) return "";
        return s.replace("&","&amp;")
                .replace("<","&lt;")
                .replace(">","&gt;")
                .replace("\"","&quot;")
                .replace("'","&#39;");
    }
    private String renderValue(Object v){
        if(v==null) return "<span class=\"empty\">(未填写)</span>";
        if(v instanceof List){
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) v;
            if(list.isEmpty()) return "<span class=\"empty\">(未填写)</span>";
            return list.stream().map(o->escapeHtml(Objects.toString(o,""))).collect(Collectors.joining("<br/>"));
        }
        String s = stringify(v);
        if(s==null || s.isBlank()) return "<span class=\"empty\">(未填写)</span>";
        return escapeHtml(s);
    }

    /** 导出文档 */
    public byte[] exportDoc(Integer templateCode, Map<String,Object> data) throws IOException {
        PreviewResult pr = buildPreview(templateCode, data);
        try (XWPFDocument doc = new XWPFDocument()) {
            // ===== 封面页（Cover Page） =====
            XWPFParagraph coverTitle = doc.createParagraph();
            coverTitle.setAlignment(ParagraphAlignment.CENTER);
            coverTitle.setSpacingAfter(800);
            XWPFRun ct = coverTitle.createRun();
            ct.setBold(true); ct.setFontSize(24); // 更大字号
            ct.setText(String.valueOf(pr.meta.getOrDefault("templateName","实验报告")));

            // 中心逐行输出 header 信息： label：value 形式
            for(Map<String,Object> h : pr.header) {
                XWPFParagraph line = doc.createParagraph();
                line.setAlignment(ParagraphAlignment.CENTER);
                line.setSpacingAfter(200);
                // Label (bold, bigger)
                XWPFRun labelRun = line.createRun();
                labelRun.setBold(true);
                labelRun.setFontSize(16);
                labelRun.setText(String.valueOf(h.get("label")) + "：");
                // Value (normal)
                XWPFRun valueRun = line.createRun();
                valueRun.setFontSize(14);
                valueRun.setText(stringify(h.get("value")));
            }

            // 底部添加生成时间
            XWPFParagraph gen = doc.createParagraph();
            gen.setAlignment(ParagraphAlignment.CENTER);
            gen.setSpacingBefore(600);
            XWPFRun gr = gen.createRun();
            gr.setFontSize(12);
            gr.setItalic(true);
            gr.setText("生成时间：" + LocalDateTime.now().toString().replace('T',' ').substring(0,19));

            // 手动分页，开始正文
            XWPFParagraph pageBreak = doc.createParagraph();
            pageBreak.setPageBreak(true);
            // ===== 正文（Sections） =====

            // sections
            for(Map<String,Object> s : pr.sections) {
                // section 标题
                XWPFParagraph pTitle = doc.createParagraph();
                pTitle.setSpacingBefore(240);
                XWPFRun sr = pTitle.createRun();
                sr.setBold(true); sr.setFontSize(14);
                sr.setText(String.valueOf(s.get("title")));

                Object val = s.get("value");
                String type = String.valueOf(s.get("type"));
                if("list".equalsIgnoreCase(type)) {
                    List<String> items = normalizeList(val);
                    if(items.isEmpty()) items = Collections.singletonList("(未填写)");
                    int idx=1;
                    for(String it : items) {
                        XWPFParagraph ip = doc.createParagraph();
                        XWPFRun ir = ip.createRun();
                        ir.setText((idx++)+". "+ it);
                    }
                } else { // text / textarea
                    String text = stringify(val);
                    if(text == null || text.isBlank()) text = "(未填写)";
                    // 支持换行
                    for(String line : text.split("\\r?\\n")) {
                        XWPFParagraph cp = doc.createParagraph();
                        XWPFRun cr = cp.createRun();
                        cr.setText(line);
                    }
                }
            }

            // ===== 结尾签署区 (Footer sign block) =====
            XWPFParagraph spacer = doc.createParagraph();
            spacer.setSpacingBefore(600);
            XWPFRun sp = spacer.createRun();
            sp.setText("");

            XWPFParagraph footerTitle = doc.createParagraph();
            footerTitle.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun ftr = footerTitle.createRun();
            ftr.setBold(true);
            ftr.setFontSize(12);

            // 三行：实验评分 / 指导教师签字 / 日期  （下划线预留填写）
            addFooterLine(doc, "实验评分", 14);
            addFooterLine(doc, "指导教师签字", 14);
            addFooterLine(doc, "日期", 20);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.write(bos);
            return bos.toByteArray();
        }
    }

    private Object valueOrDefault(Map<String,Object> data, String key, JsonNode defNode) {
        if(data != null && data.containsKey(key)) {
            Object v = data.get(key);
            if(v != null) return v;
        }
        if(defNode == null || defNode.isMissingNode() || defNode.isNull()) return "";
        if(defNode.isArray()) {
            List<String> list = new ArrayList<>();
            defNode.forEach(n -> list.add(n.asText()));
            return list;
        }
        return defNode.asText();
    }

    private String text(JsonNode node, String field) { return node.path(field).asText(""); }

    private List<String> normalizeList(Object value) {
        if(value == null) return new ArrayList<>();
        if(value instanceof List) {
            return ((List<?>)value).stream().map(o->Objects.toString(o,""))
                    .filter(s->!s.isBlank()).collect(Collectors.toList());
        }
        if(value instanceof String) {
            String s = (String) value;
            if(s.contains("\n")) {
                return Arrays.stream(s.split("\r?\n")).map(String::trim).filter(t->!t.isEmpty()).collect(Collectors.toList());
            } else {
                return Collections.singletonList(s.trim());
            }
        }
        return Collections.singletonList(String.valueOf(value));
    }

    private String stringify(Object o) {
        if(o == null) return "";
        if(o instanceof List) {
            return ((List<?>)o).stream().map(v->Objects.toString(v, ""))
                    .collect(Collectors.joining("; "));
        }
        return Objects.toString(o, "");
    }

    // Footer helper: creates a line like "标签：____________" with adjustable underline length
    private void addFooterLine(XWPFDocument doc, String label, int underlineLen) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(200);
        XWPFRun rLabel = p.createRun();
        rLabel.setFontSize(12);
        rLabel.setBold(true);
        rLabel.setText(label + "：");

        XWPFRun rLine = p.createRun();
        rLine.setFontSize(12);
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<underlineLen;i++) sb.append('_');
        rLine.setText(sb.toString());
    }
}
