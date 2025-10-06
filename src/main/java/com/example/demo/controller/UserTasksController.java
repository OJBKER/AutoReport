package com.example.demo.controller;

import com.example.demo.entity.UserTasks;
import com.example.demo.repository.UserTasksRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserTasksController {
    @Autowired
    private UserTasksRepository userTasksRepository;

    // 查询用户任务：优先按 studentNumber（当前表 user_tasks.user_id 存的是 users.student_number）
    // 兼容旧调用：如果只传 userId（其实是 studentNumber 数值）也仍尝试
    @GetMapping("/api/user-tasks")
    public List<UserTasks> getUserTasks(
            @RequestParam(value = "studentNumber", required = false) Long studentNumber,
            @RequestParam(value = "userId", required = false) Long legacyUserId
    ) {
        Long sn = studentNumber != null ? studentNumber : legacyUserId;
        if (sn == null) return java.util.Collections.emptyList();
        return userTasksRepository.findByUser_StudentNumber(sn);
    }
}