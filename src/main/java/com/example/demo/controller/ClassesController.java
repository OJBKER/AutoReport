package com.example.demo.controller;

import com.example.demo.entity.Classes;
import com.example.demo.repository.ClassesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
public class ClassesController {
    @Autowired
    private ClassesRepository classesRepository;

    // 获取所有班级列表
    @GetMapping("/api/classes")
    public List<Map<String, Object>> getAllClasses() {
        List<Classes> classesList = classesRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Classes clazz : classesList) {
            Map<String, Object> classInfo = new HashMap<>();
            classInfo.put("classId", clazz.getClassName());
            classInfo.put("name", clazz.getName());
            result.add(classInfo);
        }
        
        return result;
    }
    
    // 获取班级详细信息（包括学生数量等统计）
    @GetMapping("/api/classes/detailed")
    public List<Map<String, Object>> getAllClassesDetailed() {
        List<Classes> classesList = classesRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Classes clazz : classesList) {
            Map<String, Object> classInfo = new HashMap<>();
            classInfo.put("classId", clazz.getClassName());
            classInfo.put("name", clazz.getName());
            
            // 统计班级学生数量
            int studentCount = clazz.getStudents() != null ? clazz.getStudents().size() : 0;
            classInfo.put("studentCount", studentCount);
            
            // 统计班级任务数量
            int taskCount = clazz.getTasks() != null ? clazz.getTasks().size() : 0;
            classInfo.put("taskCount", taskCount);
            
            result.add(classInfo);
        }
        
        return result;
    }
}