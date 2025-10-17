package com.example.demo.repository;

import com.example.demo.entity.UserTasks;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTasksRepository extends JpaRepository<UserTasks, Long> {
    // (旧) 按主键 id 查找 —— 当前 user_tasks.user_id 实际存 student_number，保留仅兼容，不再推荐使用
    @Deprecated
    java.util.List<UserTasks> findByUser_Id(Long userId);
    
    // 按任务ID查找用户任务记录
    java.util.List<UserTasks> findByTask_Id(Long taskId);

    // 按任务ID+班级筛选
    java.util.List<UserTasks> findByTask_IdAndUser_Classes_ClassName(Long taskId, String className);

    // 新：按用户学号(student_number) 查找，与当前表结构一致 (user_tasks.user_id -> users.student_number)
    java.util.List<UserTasks> findByUser_StudentNumber(Long studentNumber);

    // 精确查找：按学号与任务ID
    java.util.Optional<UserTasks> findByUser_StudentNumberAndTask_Id(Long studentNumber, Long taskId);

    // 校验：按ID和班级查找
    java.util.Optional<UserTasks> findByIdAndTask_Classes_ClassName(Long id, String className);

    // 统计：按班级汇总各状态数量
    @org.springframework.data.jpa.repository.Query("SELECT ut.status, COUNT(ut) FROM user_tasks ut JOIN ut.user u WHERE u.classes.className = :className GROUP BY ut.status")
    java.util.List<Object[]> aggregateStatusByClass(@org.springframework.data.repository.query.Param("className") String className);

    // 统计：该班级所有 user_tasks 总数
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(ut) FROM user_tasks ut JOIN ut.user u WHERE u.classes.className = :className")
    Long countAllByClass(@org.springframework.data.repository.query.Param("className") String className);

    // 统计：按任务分组的完成情况（示例：返回 taskId, status, count）
    @org.springframework.data.jpa.repository.Query("SELECT ut.task.id, ut.status, COUNT(ut) FROM user_tasks ut JOIN ut.user u WHERE u.classes.className = :className GROUP BY ut.task.id, ut.status")
    java.util.List<Object[]> aggregateTaskStatusByClass(@org.springframework.data.repository.query.Param("className") String className);

    // 明细：获取某班级全部 user_tasks (返回 ut,user,task)
    @org.springframework.data.jpa.repository.Query("SELECT ut, u, t FROM user_tasks ut JOIN ut.user u JOIN ut.task t WHERE u.classes.className = :className")
    java.util.List<Object[]> findDetailsByClass(@org.springframework.data.repository.query.Param("className") String className);
}
