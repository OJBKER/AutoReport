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

    // 根据用户ID查询用户任务完成情况
    @GetMapping("/api/user-tasks")
    public List<UserTasks> getUserTasksByUserId(@RequestParam Long userId) {
        return userTasksRepository.findByUser_Id(userId);
    }
}