package com.example.demo.repository;

import com.example.demo.entity.UserTasks;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTasksRepository extends JpaRepository<UserTasks, Long> {
    // 按用户ID查找任务完成记录
    java.util.List<UserTasks> findByUser_Id(Long userId);
}
