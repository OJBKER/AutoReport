package com.example.demo.controller;

import com.example.demo.entity.Tasks;
import com.example.demo.repository.TasksRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TasksController {
    @Autowired
    private TasksRepository tasksRepository;

    // 根据班级ID查询任务列表（现在使用className作为主键）
    @GetMapping("/api/tasks")
    public List<Tasks> getTasksByClassId(@RequestParam String className) {
        return tasksRepository.findByClasses_ClassName(className);
    }
}
