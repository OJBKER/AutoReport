package com.example.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ReportExportServiceTest {

    @Autowired
    private ReportExportService service;

    @Test
    void preview_should_fill_defaults_and_overrides() throws Exception {
        Map<String,Object> data = new HashMap<>();
        data.put("experimentName", "实验一");
        ReportExportService.PreviewResult pr = service.buildPreview(1, data);
        assertNotNull(pr);
        assertEquals(1, pr.meta.get("templateCode"));
        assertTrue(pr.header.stream().anyMatch(h -> "experimentName".equals(h.get("key")) && "实验一".equals(h.get("value"))));
    }

    @Test
    void export_should_return_docx_bytes() throws Exception {
        Map<String,Object> data = new HashMap<>();
        byte[] bytes = service.exportDoc(1, data);
        assertNotNull(bytes);
        // basic PK marker for docx (PK zipped structure)
        assertTrue(bytes.length > 100, "docx size too small");
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);
    }
}
