package com.example.demo.controller;

import com.example.demo.entity.TaskSubmissions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.repository.TaskSubmissionsRepository;
import com.example.demo.repository.UsersRepository;
import com.example.demo.repository.TasksRepository;
import com.example.demo.entity.Users;
import com.example.demo.entity.Tasks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/report/draft")
public class ReportDraftController {
    @Autowired
    private TaskSubmissionsRepository taskSubmissionsRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private TasksRepository tasksRepository;
    @Autowired
    private com.example.demo.repository.UserTasksRepository userTasksRepository;

    @PostMapping
    public Map<String, Object> saveDraft(@RequestBody Map<String, Object> formData,
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
            // 提前解析传入的 id（如果存在则表示要覆盖更新）
            Long incomingId = null;
            if (formData.containsKey("id") && formData.get("id") != null) {
                try {
                    String idStr = formData.get("id").toString().trim();
                    if(!idStr.isEmpty()) incomingId = Long.valueOf(idStr);
                } catch (NumberFormatException e) {
                    result.put("success", false);
                    result.put("message", "无效的草稿ID");
                    return result;
                }
            }
            Long taskId = formData.containsKey("taskId") && formData.get("taskId") != null && !formData.get("taskId").toString().isEmpty()
                    ? Long.valueOf(formData.get("taskId").toString()) : null;
            Tasks task = null;
            if (taskId != null) {
                task = tasksRepository.findById(taskId).orElse(null);
            }
            // 单独提取 aiContextUrl，避免重复存储
            Object aiContext = formData.remove("aiContextUrl");
            // 提取模板类型（可选）
            Integer templateCode = null;
            if (formData.containsKey("templateCode")) {
                Object tc = formData.get("templateCode");
                if (tc != null) {
                    try { templateCode = Integer.valueOf(tc.toString()); } catch (NumberFormatException ignore) {}
                }
            }
            // 提交标记（可选）
            Boolean submit = null;
            if (formData.containsKey("submit")) {
                Object sObj = formData.get("submit");
                if (sObj instanceof Boolean) submit = (Boolean) sObj;
                else if (sObj instanceof String) {
                    String sv = ((String) sObj).trim().toLowerCase();
                    if ("true".equals(sv) || "1".equals(sv) || "yes".equals(sv)) submit = true;
                    else if ("false".equals(sv) || "0".equals(sv) || "no".equals(sv)) submit = false;
                } else if (sObj instanceof Number) submit = ((Number) sObj).intValue() != 0;
            }
            // 避免把 id 序列化进 taskUrl 内容（仅用于定位记录）
            formData.remove("id");
            ObjectMapper mapper = new ObjectMapper();
            String jsonContent = mapper.writeValueAsString(formData);
            TaskSubmissions draft;
            boolean isUpdate = false;
            if (incomingId != null) {
                Optional<TaskSubmissions> existingOpt = taskSubmissionsRepository.findById(incomingId);
                if (existingOpt.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "草稿不存在，无法覆盖");
                    return result;
                }
                draft = existingOpt.get();
                // 校验：必须是当前用户且是草稿（task 为空）
                if (!sameUser(currentUser, draft)) {
                    result.put("success", false);
                    result.put("message", "无权限覆盖该草稿");
                    return result;
                }
                if (draft.getTask() != null) {
                    result.put("success", false);
                    result.put("message", "该记录已绑定任务，不能作为草稿覆盖");
                    return result;
                }
                isUpdate = true;
            } else {
                draft = new TaskSubmissions();
                draft.setGithubId(currentUser.getGithubId());
                draft.setUser(currentUser);
            }
            draft.setTask(task); // 可为空代表草稿
            draft.setTaskUrl(jsonContent);
            if (aiContext != null) draft.setAiContextUrl(aiContext.toString());
            draft.setTemplateCode(templateCode);
            if (submit != null) draft.setSubmit(submit);
            draft.setUpdateTime(LocalDateTime.now());
            TaskSubmissions saved = taskSubmissionsRepository.save(draft);
            result.put("success", true);
            result.put("id", saved.getId());
            result.put("message", isUpdate ? "草稿已更新" : "草稿已新增");
            // 如果是正式提交（submit=true）且关联了具体任务，则更新 user_tasks 状态
            if (Boolean.TRUE.equals(draft.getSubmit()) && taskId != null && currentUser.getStudentNumber() != null) {
                try {
                    var utOpt = userTasksRepository.findByUser_StudentNumberAndTask_Id(currentUser.getStudentNumber(), taskId);
                    if (utOpt.isPresent()) {
                        var ut = utOpt.get();
                        boolean overwrite = (ut.getStatus() != null && "已完成".equals(ut.getStatus()));
                        ut.setStatus("已完成"); // 保持已完成
                        ut.setSubmitTime(LocalDateTime.now()); // 每次覆盖更新提交时间
                        userTasksRepository.save(ut);
                        result.put("userTaskUpdated", true);
                        result.put("userTaskOverwrite", overwrite);
                    } else {
                        result.put("userTaskUpdated", false);
                        result.put("userTaskNote", "未找到匹配的 user_tasks 记录");
                    }
                } catch (Exception ex) {
                    result.put("userTaskUpdated", false);
                    result.put("userTaskError", ex.getMessage());
                }
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
        }
        return result;
    }

    // 查询当前用户最近 N 条草稿（taskId 为空）
    @GetMapping("/recent")
    public Map<String, Object> listRecentDrafts(@RequestParam(defaultValue = "5") int limit,
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
            // 直接从 repository 取全部后内存过滤（数据量大时应写自定义查询）
            var all = fetchAllByUser(currentUser);
            var drafts = all.stream()
                    .filter(ts -> ts.getTask() == null)
                    .sorted((a,b)-> b.getUpdateTime().compareTo(a.getUpdateTime()))
                    .limit(Math.max(1, Math.min(limit, 50)))
                    .map(ts -> {
                        Map<String,Object> m = new HashMap<>();
                        m.put("id", ts.getId());
                        m.put("updateTime", ts.getUpdateTime());
                        m.put("aiContextUrl", ts.getAiContextUrl());
                        m.put("length", ts.getTaskUrl() == null ? 0 : ts.getTaskUrl().length());
                        return m;
                    }).toList();
            result.put("success", true);
            result.put("drafts", drafts);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    // 获取草稿列表或单条详情：
    // 1) 不带 id 参数：返回列表（不含 taskUrl，大幅减小 payload）
    // 2) 带 id 参数：返回该草稿的完整内容（含 taskUrl 与 aiContextUrl）
    @GetMapping("/all")
    public Map<String, Object> listAllDrafts(@RequestParam(value = "id", required = false) Long id,
                                             @RequestParam(value = "templateCode", required = false) Integer templateCode,
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

            // 详情模式
            if (id != null) {
                var opt = taskSubmissionsRepository.findById(id);
                if (opt.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "草稿不存在");
                    return result;
                }
                TaskSubmissions ts = opt.get();
                if (ts.getTask() != null) { // 不是草稿
                    result.put("success", false);
                    result.put("message", "该记录不是草稿");
                    return result;
                }
                if (!sameUser(currentUser, ts)) { // 权限校验（支持 githubId 为空）
                    result.put("success", false);
                    result.put("message", "无权限访问该草稿");
                    return result;
                }
                Map<String,Object> draft = new HashMap<>();
                draft.put("id", ts.getId());
                draft.put("updateTime", ts.getUpdateTime());
                draft.put("taskUrl", ts.getTaskUrl());
                draft.put("aiContextUrl", ts.getAiContextUrl());
                draft.put("templateCode", ts.getTemplateCode());
                draft.put("submit", ts.getSubmit());
                result.put("success", true);
                result.put("mode", "detail");
                result.put("draft", draft);
                return result;
            }

            // 列表模式
            var all = fetchAllByUser(currentUser);
        var drafts = all.stream()
            .filter(ts -> ts.getTask() == null)
            .filter(ts -> templateCode == null || (ts.getTemplateCode() != null && ts.getTemplateCode().equals(templateCode)))
                    .sorted((a,b)-> b.getUpdateTime().compareTo(a.getUpdateTime()))
                    .map(ts -> {
                        Map<String,Object> m = new HashMap<>();
                        m.put("id", ts.getId());
                        m.put("updateTime", ts.getUpdateTime());
                        m.put("templateCode", ts.getTemplateCode());
                        m.put("submit", ts.getSubmit());
                        return m; // 不含 taskUrl，点击再取详情
                    }).toList();
            result.put("success", true);
            result.put("mode", "list");
            result.put("count", drafts.size());
            result.put("drafts", drafts);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
        }
        return result;
    }

    // 辅助方法：获取当前用户
    private Users getCurrentUser(OAuth2User principal, HttpSession session) {
        if (principal != null) {
            String githubId = principal.getAttribute("id").toString();
            Optional<Users> userOpt = usersRepository.findByGithubId(githubId);
            return userOpt.orElse(null);
        }
        Object studentIdObj = session.getAttribute("studentId");
        Object passwordObj = session.getAttribute("password");
        if (studentIdObj != null && passwordObj != null) {
            Optional<Users> userOpt = usersRepository.findByStudentNumberAndPassword(
                Long.valueOf(studentIdObj.toString()), passwordObj.toString());
            return userOpt.orElse(null);
        }
        return null;
    }

    // ======== 新增辅助：用户匹配（兼容 githubId 为空） ========
    private boolean sameUser(Users currentUser, TaskSubmissions ts){
        if (currentUser == null || ts == null) return false;
        String g1 = currentUser.getGithubId();
        String g2 = ts.getGithubId();
        if (g1 != null && g2 != null) {
            return g1.equals(g2);
        }
        // 至少有一边 githubId 为空时，fallback 使用 学号 比较
        if (currentUser.getStudentNumber() != null && ts.getUser() != null) {
            return currentUser.getStudentNumber().equals(ts.getUser().getStudentNumber());
        }
        return false;
    }

    private java.util.List<TaskSubmissions> fetchAllByUser(Users currentUser){
        if (currentUser == null) return java.util.Collections.emptyList();
        if (currentUser.getGithubId() != null && !currentUser.getGithubId().isEmpty()) {
            return taskSubmissionsRepository.findByGithubId(currentUser.getGithubId());
        }
        if (currentUser.getStudentNumber() != null) {
            return taskSubmissionsRepository.findByUser_StudentNumber(currentUser.getStudentNumber());
        }
        return java.util.Collections.emptyList();
    }
}
