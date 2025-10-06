package com.example.demo.repository;

import com.example.demo.entity.UserTasks;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTasksRepository extends JpaRepository<UserTasks, Long> {
    // (旧) 按主键 id 查找 —— 当前 user_tasks.user_id 实际存 student_number，保留仅兼容，不再推荐使用
    @Deprecated
    java.util.List<UserTasks> findByUser_Id(Long userId);
    
    // 按任务ID查找用户任务记录
    java.util.List<UserTasks> findByTask_Id(Long taskId);

    // 新：按用户学号(student_number) 查找，与当前表结构一致 (user_tasks.user_id -> users.student_number)
    java.util.List<UserTasks> findByUser_StudentNumber(Long studentNumber);

    // 精确查找：按学号与任务ID
    java.util.Optional<UserTasks> findByUser_StudentNumberAndTask_Id(Long studentNumber, Long taskId);
}
