package com.example.demo.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.service.ReportExportService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 占位接口：报告导出相关（预览 / 导出）
 * 目前仅提供壳实现，返回固定/伪造数据，后续再接入真实模板与生成逻辑。
 */
@RestController
@RequestMapping("/api/report")
public class ReportExportController {

    @Autowired
    private ReportExportService reportExportService;

    /**
     * 预览接口：返回服务端将要用于生成文档的结构（stub）。
    * 输入格式暂约定：{ templateCode: number|null, data: object }
     */
    @PostMapping("/export/preview")
    public Map<String,Object> preview(@RequestBody(required = false) Map<String,Object> body) {
        Map<String,Object> input = body == null ? new HashMap<>() : body;
        Integer templateCode = toInt(input.get("templateCode"));
        Map<String,Object> data = safeMap(input.get("data"));
        Map<String,Object> resp = new HashMap<>();
        try {
            ReportExportService.PreviewResult pr = reportExportService.buildPreview(templateCode, data);
            resp.put("success", true);
            resp.put("preview", pr);
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("error", e.getMessage());
        }
        return resp;
    }

    /**
     * HTML 预览：返回可直接嵌入 iframe 的完整 HTML 字符串。
     */
    @PostMapping("/export/preview/html")
    public ResponseEntity<byte[]> previewHtml(@RequestBody(required = false) Map<String,Object> body) {
        Map<String,Object> input = body == null ? new HashMap<>() : body;
        Integer templateCode = toInt(input.get("templateCode"));
        Map<String,Object> data = safeMap(input.get("data"));
        try {
            String html = reportExportService.buildHtmlPreview(templateCode, data);
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_HTML);
            headers.set(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
            headers.setContentLength(bytes.length);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            byte[] err = ("PREVIEW ERROR: "+e.getMessage()).getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            return new ResponseEntity<>(err, headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 导出接口：返回一个伪 DOCX（二进制字节表示）占位；真实实现应生成并返回 application/vnd.openxmlformats-officedocument.wordprocessingml.document。
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportDoc(@RequestBody(required = false) Map<String,Object> body) {
        Map<String,Object> input = body == null ? new HashMap<>() : body;
        Integer templateCode = toInt(input.get("templateCode"));
        Map<String,Object> data = safeMap(input.get("data"));
        try {
            byte[] bytes = reportExportService.exportDoc(templateCode, data);
            String fileName = "report-" + (templateCode==null?"default":templateCode) + ".docx";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.setContentLength(bytes.length);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            byte[] err = ("EXPORT ERROR: "+e.getMessage()).getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentLength(err.length);
            return new ResponseEntity<>(err, headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Integer toInt(Object o) {
        if(o == null) return null;
        if(o instanceof Number) return ((Number)o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> safeMap(Object o) {
        if(o instanceof Map) return (Map<String,Object>)o;
        return new HashMap<>();
    }
}
