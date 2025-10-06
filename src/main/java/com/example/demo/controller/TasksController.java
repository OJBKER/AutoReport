package com.example.demo.controller;

import com.example.demo.entity.Tasks;
import com.example.demo.entity.UserTasks;
import com.example.demo.repository.TasksRepository;
import com.example.demo.repository.UserTasksRepository;
import com.example.demo.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
public class TasksController {
    @Autowired
    private TasksRepository tasksRepository;
    
    @Autowired
    private UserTasksRepository userTasksRepository;
    
    @Autowired
    private UsersRepository usersRepository;

    // 根据班级ID查询任务列表（现在使用className作为主键）
    @GetMapping("/api/tasks")
    public List<Tasks> getTasksByClassId(@RequestParam String className) {
        return tasksRepository.findByClasses_ClassName(className);
    }
    
    // 获取班级任务详细统计信息
    @GetMapping("/api/tasks/detailed")
    public Map<String, Object> getDetailedTasksByClassId(@RequestParam String className) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取班级所有任务
        List<Tasks> tasks = tasksRepository.findByClasses_ClassName(className);
        
        // 获取班级总学生数
        Long totalStudents = usersRepository.countByClasses_ClassName(className);
        
        // 为每个任务添加统计信息
        List<Map<String, Object>> detailedTasks = new ArrayList<>();
        for (Tasks task : tasks) {
            Map<String, Object> taskDetail = new HashMap<>();
            taskDetail.put("id", task.getId());
            taskDetail.put("title", task.getTitle());
            taskDetail.put("description", task.getDescription());
            taskDetail.put("deadline", task.getDeadline());
            taskDetail.put("templateCode", task.getTemplateCode());
            
            // 获取该任务的完成统计
            List<UserTasks> userTasksList = userTasksRepository.findByTask_Id(task.getId());
            
            // 统计各种状态的数量
            long completedCount = userTasksList.stream()
                .filter(ut -> "已完成".equals(ut.getStatus())).count();
            long inProgressCount = userTasksList.stream()
                .filter(ut -> "进行中".equals(ut.getStatus())).count();
            long notStartedCount = userTasksList.stream()
                .filter(ut -> "未开始".equals(ut.getStatus())).count();
            
            // 该任务的认领总人数（实际参与的人数）
            long taskTotalParticipants = userTasksList.size();
            
            // 计算完成率（基于认领人数，而不是班级总人数）
            double completionRate = taskTotalParticipants > 0 ? 
                (double) completedCount / taskTotalParticipants * 100 : 0;
            
            // 计算平均分
            double averageScore = userTasksList.stream()
                .filter(ut -> ut.getScore() != null && "已完成".equals(ut.getStatus()))
                .mapToInt(UserTasks::getScore)
                .average()
                .orElse(0.0);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("taskTotalParticipants", taskTotalParticipants); // 任务认领总人数
            statistics.put("completedCount", completedCount);
            statistics.put("inProgressCount", inProgressCount);
            statistics.put("notStartedCount", notStartedCount);
            statistics.put("completionRate", Math.round(completionRate * 100) / 100.0);
            statistics.put("averageScore", Math.round(averageScore * 100) / 100.0);
            
            taskDetail.put("statistics", statistics);
            detailedTasks.add(taskDetail);
        }
        
        // 班级整体统计
        Map<String, Object> classStats = new HashMap<>();
        classStats.put("totalTasks", tasks.size());
        classStats.put("totalStudents", totalStudents);
        
        result.put("className", className);
        result.put("tasks", detailedTasks);
        result.put("classStatistics", classStats);
        
        return result;
    }
}
