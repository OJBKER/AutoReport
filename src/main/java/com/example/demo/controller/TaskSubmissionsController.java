package com.example.demo.controller;

import com.example.demo.entity.TaskSubmissions;
import com.example.demo.entity.Users;
import com.example.demo.entity.Tasks;
import com.example.demo.repository.TaskSubmissionsRepository;
import com.example.demo.repository.UsersRepository;
import com.example.demo.repository.TasksRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/task-submissions")
public class TaskSubmissionsController {
    
    @Autowired
    private TaskSubmissionsRepository taskSubmissionsRepository;
    
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private TasksRepository tasksRepository;
    
    // 创建或更新任务提交记录
    @PostMapping
    public Map<String, Object> createOrUpdateSubmission(@RequestBody Map<String, Object> requestBody,
                                                        @AuthenticationPrincipal OAuth2User principal,
                                                        HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Long taskId = Long.valueOf(requestBody.get("taskId").toString());
            String taskUrl = (String) requestBody.get("taskUrl");
            String aiContextUrl = (String) requestBody.get("aiContextUrl");
            Integer templateCode = null;
            if (requestBody.get("templateCode") != null) {
                try {
                    templateCode = Integer.valueOf(requestBody.get("templateCode").toString());
                } catch (NumberFormatException ignore) {
                }
            }
            Boolean submit = null;
            if (requestBody.get("submit") != null) {
                Object sObj = requestBody.get("submit");
                if (sObj instanceof Boolean) {
                    submit = (Boolean) sObj;
                } else if (sObj instanceof String) {
                    String sv = ((String) sObj).trim().toLowerCase();
                    if ("true".equals(sv) || "1".equals(sv) || "yes".equals(sv)) submit = true;
                    else if ("false".equals(sv) || "0".equals(sv) || "no".equals(sv)) submit = false;
                } else if (sObj instanceof Number) {
                    submit = ((Number) sObj).intValue() != 0;
                }
            }
            
            // 获取当前用户信息
            Users currentUser = getCurrentUser(principal, session);
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }
            
            // 检查任务是否存在
            Optional<Tasks> taskOpt = tasksRepository.findById(taskId);
            if (!taskOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }
            
            // 查找是否已存在提交记录（优先 githubId，若为空则用 studentNumber 作为回退）
            Optional<TaskSubmissions> existingSubmission;
            boolean useGithub = currentUser.getGithubId() != null && !currentUser.getGithubId().isEmpty();
            if (useGithub) {
                existingSubmission = taskSubmissionsRepository.findByTask_IdAndGithubId(taskId, currentUser.getGithubId());
            } else {
                // 对于学校账号（无 githubId），允许多版本历史，因此这里只能取列表再选最新；
                // 但为了保持与旧逻辑兼容（单记录覆盖），尝试按 学号 获取最新一条
                List<TaskSubmissions> fallbackHistory = taskSubmissionsRepository
                        .findAllByTask_IdAndUser_StudentNumberOrderByUpdateTimeDesc(taskId, currentUser.getStudentNumber());
                existingSubmission = fallbackHistory.isEmpty() ? Optional.empty() : Optional.of(fallbackHistory.get(0));
            }
            
            TaskSubmissions submission;
            if (existingSubmission.isPresent()) {
                // 更新现有记录
                submission = existingSubmission.get();
                submission.setTaskUrl(taskUrl);
                submission.setAiContextUrl(aiContextUrl);
                submission.setUpdateTime(LocalDateTime.now());
                submission.setTemplateCode(templateCode);
                if (submit != null) submission.setSubmit(submit);
            } else {
                // 创建新记录
                submission = new TaskSubmissions();
                submission.setTask(taskOpt.get()); // 设置Task关联而不是taskId
                submission.setTaskUrl(taskUrl);
                submission.setAiContextUrl(aiContextUrl);
                submission.setGithubId(currentUser.getGithubId()); // 可能为 null（学校账号）
                submission.setUser(currentUser);
                submission.setTemplateCode(templateCode);
                if (submit != null) submission.setSubmit(submit); // 默认 false，除非显式传入
            }
            
            TaskSubmissions savedSubmission = taskSubmissionsRepository.save(submission);
            
            result.put("success", true);
            result.put("message", existingSubmission.isPresent() ? "任务提交记录已更新" : "任务提交记录已创建");
            result.put("submissionId", savedSubmission.getId());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作失败: " + e.getMessage());
        }
        
        return result;
    }
    
    // 获取用户的所有任务提交记录
    @GetMapping("/user")
    public Map<String, Object> getUserSubmissions(@AuthenticationPrincipal OAuth2User principal,
                                                  HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Users currentUser = getCurrentUser(principal, session);
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "用户未登录或会话失效，请重新登录。");
                return result;
            }
            String githubId = currentUser.getGithubId();
            List<TaskSubmissions> submissions;
            if (githubId != null && !githubId.isEmpty()) {
                submissions = taskSubmissionsRepository.findByGithubId(githubId);
            } else {
                // 回退：按学号查询
                submissions = taskSubmissionsRepository.findByUser_StudentNumber(currentUser.getStudentNumber());
            }
            result.put("success", true);
            result.put("submissions", mapSubmissionList(submissions));
            result.put("count", submissions.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取提交记录失败: " + e.getMessage());
        }
        return result;
    }
    
    // 获取特定任务的提交记录
    @GetMapping("/task/{taskId}")
    public Map<String, Object> getTaskSubmissions(@PathVariable Long taskId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<TaskSubmissions> submissions = taskSubmissionsRepository.findByTask_Id(taskId);
            result.put("success", true);
            result.put("submissions", mapSubmissionList(submissions));
            result.put("count", submissions.size());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取任务提交记录失败: " + e.getMessage());
        }
        
        return result;
    }
    
    // 获取用户特定任务的提交记录
    @GetMapping("/user/task/{taskId}")
    public Map<String, Object> getUserTaskSubmission(@PathVariable Long taskId,
                                                     @AuthenticationPrincipal OAuth2User principal,
                                                     HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Users currentUser = getCurrentUser(principal, session);
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }
            
            // 使用历史列表取最新，避免 NonUniqueResultException；支持 githubId 为空回退
            List<TaskSubmissions> history;
            if (currentUser.getGithubId() != null && !currentUser.getGithubId().isEmpty()) {
                history = taskSubmissionsRepository.findAllByTaskAndGithubOrderByUpdateTimeDesc(taskId, currentUser.getGithubId());
            } else {
                history = taskSubmissionsRepository.findAllByTask_IdAndUser_StudentNumberOrderByUpdateTimeDesc(taskId, currentUser.getStudentNumber());
            }
            result.put("success", true);
            result.put("hasSubmission", !history.isEmpty());
            if(!history.isEmpty()){
                result.put("submission", mapSubmission(history.get(0))); // 最新
                result.put("historyCount", history.size());
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取提交记录失败: " + e.getMessage());
        }
        
        return result;
    }
    
    // 删除提交记录
    @DeleteMapping("/{submissionId}")
    public Map<String, Object> deleteSubmission(@PathVariable Long submissionId,
                                               @AuthenticationPrincipal OAuth2User principal,
                                               HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Users currentUser = getCurrentUser(principal, session);
            if (currentUser == null) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }
            
            Optional<TaskSubmissions> submissionOpt = taskSubmissionsRepository.findById(submissionId);
            if (!submissionOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "提交记录不存在");
                return result;
            }
            
            TaskSubmissions submission = submissionOpt.get();
            if (!submission.getUser().getStudentNumber().equals(currentUser.getStudentNumber())) {
                result.put("success", false);
                result.put("message", "无权限删除此提交记录");
                return result;
            }
            
            taskSubmissionsRepository.delete(submission);
            result.put("success", true);
            result.put("message", "提交记录已删除");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
        }
        
        return result;
    }
    
    // 辅助方法：获取当前用户
    private Users getCurrentUser(OAuth2User principal, HttpSession session) {
        if (principal != null) {
            // GitHub OAuth2 用户
            String githubId = principal.getAttribute("id").toString();
            Optional<Users> userOpt = usersRepository.findByGithubId(githubId);
            return userOpt.orElse(null);
        }
        
        // 学校用户
        Object studentIdObj = session.getAttribute("studentId");
        Object passwordObj = session.getAttribute("password");
        if (studentIdObj != null && passwordObj != null) {
            Optional<Users> userOpt = usersRepository.findByStudentNumberAndPassword(
                Long.valueOf(studentIdObj.toString()), passwordObj.toString());
            return userOpt.orElse(null);
        }
        
        return null;
    }

    // ===== 以下为 DTO 构造辅助方法，避免直接序列化 JPA 实体造成懒加载/循环问题 =====
    private List<Map<String, Object>> mapSubmissionList(List<TaskSubmissions> list) {
        return list.stream().map(this::mapSubmission).toList();
    }

    private Map<String, Object> mapSubmission(TaskSubmissions submission) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", submission.getId());
        m.put("taskUrl", submission.getTaskUrl());
        m.put("aiContextUrl", submission.getAiContextUrl());
    m.put("githubId", submission.getGithubId());
    m.put("templateCode", submission.getTemplateCode());
    m.put("submit", submission.getSubmit());
        // 时间字段（容错：实体里可能只有 createTime/updateTime 之一）
    // 目前实体只有 updateTime，createTime 不存在；保留占位键便于前端统一处理
    m.put("createTime", null);
    m.put("updateTime", submission.getUpdateTime());
        // 关联 task 做瘦身，防止懒加载深入 user/classes
        Tasks task = submission.getTask();
        if (task != null) {
            Map<String, Object> taskInfo = new HashMap<>();
            taskInfo.put("id", task.getId());
            taskInfo.put("title", safeTaskTitle(task));
            taskInfo.put("deadline", task.getDeadline());
            m.put("task", taskInfo);
            m.put("taskId", task.getId());
        } else {
            m.put("task", null);
        }
        // 只暴露 user 的最小必要信息，避免 classes 懒代理
        Users u = submission.getUser();
        if (u != null) {
            Map<String, Object> uInfo = new HashMap<>();
            uInfo.put("id", u.getId());
            uInfo.put("name", u.getName());
            uInfo.put("studentNumber", u.getStudentNumber());
            uInfo.put("githubId", u.getGithubId());
            m.put("user", uInfo);
        } else {
            m.put("user", null);
        }
        return m;
    }

    // 防御式读取 task 标题，避免潜在懒加载异常（即使 unlikely）
    private String safeTaskTitle(Tasks task) {
        try { return task.getTitle(); } catch (Exception e) { return null; }
    }
}