package com.example.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.*;

/**
 * 提供可用实验报告模板列表：扫描 classpath:/static/ReportTemplate/*.json
 * 返回每个模板的 templateType / templateName / templateCode / 访问 url
 */
@RestController
@RequestMapping("/api/report/templates")
public class TemplateController {

    @Autowired
    private ResourcePatternResolver resolver;

    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> listTemplates() {
        Map<String,Object> resp = new HashMap<>();
        List<Map<String,Object>> templates = new ArrayList<>();
        try {
            // 扫描所有模板文件
            Resource[] resources = resolver.getResources("classpath:/static/ReportTemplate/*.json");
            for (Resource r : resources) {
                try (InputStream is = r.getInputStream()) {
                    JsonNode root = mapper.readTree(is);
                    String templateType = text(root, "templateType");
                    String templateName = text(root, "templateName");
                    Integer code = intVal(root, "templateCode");
                    if (templateType == null || templateName == null) continue; // 必须字段
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("templateType", templateType);
                    m.put("templateName", templateName);
                    if (code != null) m.put("templateCode", code);
                    // 静态资源访问 URL（由 Spring Boot 静态资源映射）
                    m.put("url", "/ReportTemplate/" + Objects.requireNonNull(r.getFilename()));
                    templates.add(m);
                } catch (Exception single) {
                    // 单个文件解析失败则跳过
                }
            }
            templates.sort(Comparator.comparing(o -> Objects.toString(o.get("templateType"), "")));
            resp.put("success", true);
            resp.put("templates", templates);
            resp.put("count", templates.size());
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", "模板扫描失败: " + e.getMessage());
        }
        return resp;
    }

    private String text(JsonNode n, String f) { return n.has(f) && !n.get(f).isNull() ? n.get(f).asText() : null; }
    private Integer intVal(JsonNode n, String f) { return n.has(f) && n.get(f).canConvertToInt() ? n.get(f).asInt() : null; }
}