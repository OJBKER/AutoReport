package com.example.demo.repository;

import com.example.demo.entity.TaskSubmissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TaskSubmissionsRepository extends JpaRepository<TaskSubmissions, Long> {
    
    // 根据任务ID查找所有提交记录
    List<TaskSubmissions> findByTask_Id(Long taskId);
    
    // 根据学号查找提交记录（推荐使用，直接查询无需JOIN）
    
    // 根据GitHub ID查找提交记录
    List<TaskSubmissions> findByGithubId(String githubId);

    // 根据 学号(studentNumber) 查找提交记录（当 githubId 为空时的回退）
    List<TaskSubmissions> findByUser_StudentNumber(Long studentNumber);
    
    // 根据用户关联查找提交记录（通过外键关联查询，确保数据一致性）
    
    // 根据任务ID和学号查找提交记录（推荐使用，性能最佳）
    
    // 根据任务ID和GitHub ID查找提交记录 (可能存在多次覆盖的历史，需要取最新)
    Optional<TaskSubmissions> findByTask_IdAndGithubId(Long taskId, String githubId); // 保留旧方法（若仍被其他代码调用）

    // 返回某个用户某任务的全部提交历史（按更新时间倒序），便于取最新
    @Query("SELECT ts FROM TaskSubmissions ts WHERE ts.task.id = :taskId AND ts.githubId = :githubId ORDER BY ts.updateTime DESC")
    List<TaskSubmissions> findAllByTaskAndGithubOrderByUpdateTimeDesc(@Param("taskId") Long taskId, @Param("githubId") String githubId);

    // 回退：根据任务ID + 学号 查询全部历史记录（按更新时间倒序）
    List<TaskSubmissions> findAllByTask_IdAndUser_StudentNumberOrderByUpdateTimeDesc(Long taskId, Long studentNumber);
    
    // 查询某个班级的所有任务提交记录
    @Query("SELECT ts FROM TaskSubmissions ts JOIN ts.task t WHERE t.classes.className = :className")
    List<TaskSubmissions> findByTaskClassId(@Param("className") String className);
    
    // 统计某个任务的提交数量
    Long countByTask_Id(Long taskId);
    
    // 统计某个用户的提交数量（通过学号，推荐使用）
}