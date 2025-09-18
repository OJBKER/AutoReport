package com.example.demo.controller;
import com.example.demo.repository.UsersRepository;
import com.example.demo.entity.Users;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

@RestController
@RequestMapping("/api/text")
public class TextController {
    @Autowired
    private UsersRepository usersRepository;
    @PostMapping
    public Map<String, Object> receiveText(@RequestBody Map<String, Object> payload) {
        String text = payload.getOrDefault("text", "").toString();
        Users users = new Users(text, text);
        usersRepository.save(users);
        Map<String, Object> result = new HashMap<>();
        result.put("text", "已存储到数据库：" + text);
        return result;
    }

    @PostMapping("/export")
    public ResponseEntity<ByteArrayResource> exportDoc() {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            List<Users> usersList = usersRepository.findAll();
            for (Users u : usersList) {
                XWPFParagraph para = doc.createParagraph();
                XWPFRun run = para.createRun();
                run.setText(u.getName());
            }
            doc.write(out);
            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export.docx");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(resource.contentLength())
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
