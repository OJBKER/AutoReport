package com.example.demo.controller;

import com.example.demo.entity.Users;
import com.example.demo.entity.Classes;
import com.example.demo.repository.UsersRepository;
import com.example.demo.repository.UserTasksRepository;
import com.example.demo.repository.TasksRepository;
import com.example.demo.repository.ClassesRepository;
import com.example.demo.repository.TaskSubmissionsRepository;
import com.example.demo.entity.Tasks;
import com.example.demo.entity.UserTasks;
import com.example.demo.entity.TaskSubmissions;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;

import javax.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private UserTasksRepository userTasksRepository;
    @Autowired
    private TasksRepository tasksRepository;
    @Autowired
    private ClassesRepository classesRepository;
    @Autowired
    private TaskSubmissionsRepository taskSubmissionsRepository;
    // 管理员：按任务查看提交明细（含最新提交内容）
    @GetMapping("/task-submissions/{taskId}")
    public Map<String,Object> getTaskSubmissions(@PathVariable Long taskId,
                                                 HttpSession session,
                                                 @AuthenticationPrincipal OAuth2User principal) {
        Map<String,Object> resp = new HashMap<>();
        Users admin = resolveCurrentUser(session, principal);
        if (admin == null) { resp.put("success", false); resp.put("error", "NOT_LOGIN"); return resp; }
        if (admin.getIsAdmin() == null || !admin.getIsAdmin()) { resp.put("success", false); resp.put("error", "NO_PERMISSION"); return resp; }
        if (admin.getClasses() == null) { resp.put("success", false); resp.put("error", "NO_CLASS_BOUND"); return resp; }

        Optional<Tasks> taskOpt = tasksRepository.findById(taskId);
        if (taskOpt.isEmpty()) { resp.put("success", false); resp.put("error", "TASK_NOT_FOUND"); return resp; }
        Tasks task = taskOpt.get();
        if (task.getClasses() == null || !admin.getClasses().getClassName().equals(task.getClasses().getClassName())) {
            resp.put("success", false); resp.put("error", "NOT_SAME_CLASS"); return resp; }

        String className = admin.getClasses().getClassName();
        java.util.List<UserTasks> userTasks = userTasksRepository.findByTask_IdAndUser_Classes_ClassName(taskId, className);
        java.util.List<Map<String,Object>> data = new ArrayList<>();
        for (UserTasks ut : userTasks) {
            Users student = ut.getUser();
            TaskSubmissions submission = fetchLatestSubmissionFor(ut, student);
            Map<String,Object> row = new HashMap<>();
            row.put("userTaskId", ut.getId());
            row.put("status", ut.getStatus());
            row.put("score", ut.getScore());
            row.put("submitTime", ut.getSubmitTime());
            if (student != null) {
                row.put("studentNumber", student.getStudentNumber());
                row.put("studentName", student.getName());
                row.put("githubId", student.getGithubId());
            }
            if (submission != null) {
                row.put("submissionId", submission.getId());
                row.put("taskUrl", submission.getTaskUrl());
                row.put("aiContextUrl", submission.getAiContextUrl());
                row.put("submissionUpdateTime", submission.getUpdateTime());
                row.put("submitFlag", submission.getSubmit());
                row.put("submissionTemplateCode", submission.getTemplateCode());
            }
            data.add(row);
        }
        resp.put("success", true);
        resp.put("task", mapTask(task));
        resp.put("count", data.size());
        resp.put("data", data);
        return resp;
    }

    // 管理员：保存评分
    @PostMapping("/user-tasks/{userTaskId}/score")
    @Transactional
    public Map<String,Object> updateUserTaskScore(@PathVariable Long userTaskId,
                                                  @RequestBody Map<String,Object> body,
                                                  HttpSession session,
                                                  @AuthenticationPrincipal OAuth2User principal) {
        Map<String,Object> resp = new HashMap<>();
        Users admin = resolveCurrentUser(session, principal);
        if (admin == null) { resp.put("success", false); resp.put("error", "NOT_LOGIN"); return resp; }
        if (admin.getIsAdmin() == null || !admin.getIsAdmin()) { resp.put("success", false); resp.put("error", "NO_PERMISSION"); return resp; }
        if (admin.getClasses() == null) { resp.put("success", false); resp.put("error", "NO_CLASS_BOUND"); return resp; }

        Optional<UserTasks> utOpt = userTasksRepository.findByIdAndTask_Classes_ClassName(userTaskId, admin.getClasses().getClassName());
        if (utOpt.isEmpty()) { resp.put("success", false); resp.put("error", "NOT_FOUND"); return resp; }

        UserTasks ut = utOpt.get();
        Integer score = null;
        if (body.containsKey("score") && body.get("score") != null) {
            try {
                score = Integer.valueOf(body.get("score").toString());
            } catch (NumberFormatException nfe) {
                resp.put("success", false);
                resp.put("error", "INVALID_SCORE");
                return resp;
            }
        }
        String status = body.get("status") == null ? null : body.get("status").toString();
        ut.setScore(score);
        if (status != null && !status.isEmpty()) {
            ut.setStatus(status);
        }
        if (score != null && ut.getSubmitTime() == null) {
            ut.setSubmitTime(java.time.LocalDateTime.now());
        }
        userTasksRepository.save(ut);

        resp.put("success", true);
        resp.put("data", mapUserTask(ut));
        return resp;
    }

    // 获取与当前登录管理员同班级的全部学生信息
    @GetMapping("/class-users")
    public Map<String, Object> getSameClassUsers(HttpSession session, @AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> resp = new HashMap<>();
        Users admin = resolveCurrentUser(session, principal);
        if (admin == null) {
            resp.put("success", false);
            resp.put("error", "NOT_LOGIN");
            return resp;
        }
        if (admin.getIsAdmin() == null || !admin.getIsAdmin()) {
            resp.put("success", false);
            resp.put("error", "NO_PERMISSION");
            return resp;
        }
        if (admin.getClasses() == null) {
            resp.put("success", true);
            resp.put("data", Collections.emptyList());
            resp.put("classId", null);
            return resp;
        }
        String className = admin.getClasses().getClassName();
        List<Users> users = usersRepository.findByClasses_ClassNameOrderByStudentNumberAsc(className);
        // 只返回部分字段，避免暴露敏感数据
        List<Map<String, Object>> list = new ArrayList<>();
        for (Users u : users) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("studentNumber", u.getStudentNumber());
            m.put("githubId", u.getGithubId());
            m.put("isAdmin", u.getIsAdmin());
            list.add(m);
        }
        resp.put("success", true);
        resp.put("classId", className);
        resp.put("data", list);
        resp.put("count", list.size());
        return resp;
    }

    // 班级任务统计：统计当前管理员班级所有 user_tasks 记录的状态分布及任务分组情况
    @GetMapping("/class-task-stats")
    public Map<String,Object> getClassTaskStats(HttpSession session, @AuthenticationPrincipal OAuth2User principal) {
        Map<String,Object> resp = new HashMap<>();
        Users admin = resolveCurrentUser(session, principal);
        if (admin == null) {
            resp.put("success", false); resp.put("error", "NOT_LOGIN"); return resp;
        }
        if (admin.getIsAdmin() == null || !admin.getIsAdmin()) {
            resp.put("success", false); resp.put("error", "NO_PERMISSION"); return resp;
        }
        if (admin.getClasses() == null) {
            resp.put("success", true);
            resp.put("classId", null);
            resp.put("total", 0);
            resp.put("statusBreakdown", Collections.emptyList());
            resp.put("taskStatus", Collections.emptyList());
            return resp;
        }
        String className = admin.getClasses().getClassName();
        Long total = userTasksRepository.countAllByClass(className);
    java.util.List<Object[]> statusAgg = userTasksRepository.aggregateStatusByClass(className);
    java.util.List<Object[]> taskStatusAgg = userTasksRepository.aggregateTaskStatusByClass(className);

        // 统一返回结构：statusBreakdown => [{status, count}], taskStatus => [{taskId, status, count}]
        java.util.List<Map<String,Object>> statusList = new java.util.ArrayList<>();
        for (Object[] arr : statusAgg) {
            Map<String,Object> row = new HashMap<>();
            row.put("status", arr[0]);
            row.put("count", arr[1]);
            statusList.add(row);
        }
        java.util.List<Map<String,Object>> taskStatusList = new java.util.ArrayList<>();
        for (Object[] arr : taskStatusAgg) {
            Map<String,Object> row = new HashMap<>();
            row.put("taskId", arr[0]);
            row.put("status", arr[1]);
            row.put("count", arr[2]);
            taskStatusList.add(row);
        }
        resp.put("success", true);
        resp.put("classId", className);
        resp.put("total", total);
        resp.put("statusBreakdown", statusList);
        resp.put("taskStatus", taskStatusList);
        return resp;
    }

    // 班级 user_tasks 明细：提供前端筛选使用
    @GetMapping("/class-user-tasks")
    public Map<String,Object> getClassUserTasks(HttpSession session, @AuthenticationPrincipal OAuth2User principal) {
        Map<String,Object> resp = new HashMap<>();
        Users admin = resolveCurrentUser(session, principal);
        if (admin == null) { resp.put("success", false); resp.put("error", "NOT_LOGIN"); return resp; }
        if (admin.getIsAdmin() == null || !admin.getIsAdmin()) { resp.put("success", false); resp.put("error", "NO_PERMISSION"); return resp; }
        if (admin.getClasses() == null) { resp.put("success", true); resp.put("classId", null); resp.put("data", java.util.Collections.emptyList()); return resp; }
        String className = admin.getClasses().getClassName();
        java.util.List<Object[]> rows = userTasksRepository.findDetailsByClass(className);
        java.util.List<Map<String,Object>> list = new java.util.ArrayList<>();
        for (Object[] arr : rows) {
            // arr[0]=UserTasks, arr[1]=Users, arr[2]=Tasks
            com.example.demo.entity.UserTasks ut = (com.example.demo.entity.UserTasks) arr[0];
            com.example.demo.entity.Users u = (com.example.demo.entity.Users) arr[1];
            com.example.demo.entity.Tasks t = (com.example.demo.entity.Tasks) arr[2];
            Map<String,Object> m = new HashMap<>();
            m.put("id", ut.getId());
            m.put("status", ut.getStatus());
            m.put("submitTime", ut.getSubmitTime());
            m.put("score", ut.getScore());
            m.put("studentNumber", u.getStudentNumber());
            m.put("name", u.getName());
            m.put("taskId", t.getId());
            m.put("taskTitle", t.getTitle());
            list.add(m);
        }
        resp.put("success", true);
        resp.put("classId", className);
        resp.put("count", list.size());
        resp.put("data", list);
        return resp;
    }

    // 创建任务（发布任务）
    @PostMapping("/tasks")
    @Transactional
    public Map<String,Object> createTask(HttpSession session,
                                         @AuthenticationPrincipal OAuth2User principal,
                                         @RequestBody Map<String,Object> body) {
        Map<String,Object> resp = new HashMap<>();
        Users admin = resolveCurrentUser(session, principal);
        if (admin == null) { resp.put("success", false); resp.put("error","NOT_LOGIN"); return resp; }
        if (admin.getIsAdmin() == null || !admin.getIsAdmin()) { resp.put("success", false); resp.put("error","NO_PERMISSION"); return resp; }
        if (admin.getClasses() == null) { resp.put("success", false); resp.put("error","NO_CLASS_BOUND"); return resp; }

    String title = stringVal(body.get("title"));
    String description = stringVal(body.get("description"));
    String deadlineStr = stringVal(body.get("deadline")); // ISO datetime or date
    String targetStudentNumberStr = stringVal(body.get("targetStudentNumber")); // 可选：单个学生学号
        Integer templateCode = null;
        if (body.get("templateCode") != null) {
            try { templateCode = Integer.valueOf(body.get("templateCode").toString()); } catch (NumberFormatException ignore) {}
        }
        if (title == null || title.trim().isEmpty()) { resp.put("success", false); resp.put("error","TITLE_REQUIRED"); return resp; }

        java.time.LocalDateTime deadline = null;
        if (deadlineStr != null && !deadlineStr.isEmpty()) {
            try {
                // Try parse full datetime first
                deadline = java.time.LocalDateTime.parse(deadlineStr);
            } catch (Exception e) {
                try {
                    // Fallback: date only -> at end of day
                    java.time.LocalDate d = java.time.LocalDate.parse(deadlineStr);
                    deadline = d.atTime(23,59,59);
                } catch (Exception ignored) {}
            }
        }

        String className = admin.getClasses().getClassName();
        // Attach managed Classes reference (avoid detached entity issues)
        com.example.demo.entity.Classes cls = classesRepository.findById(className).orElse(admin.getClasses());
        Tasks t = new Tasks();
        t.setTitle(title);
        t.setDescription(description);
        t.setDeadline(deadline);
        t.setTemplateCode(templateCode);
        t.setClasses(cls);
        Tasks saved = tasksRepository.save(t);

        int createdCount = 0;
        java.util.List<UserTasks> createList = new java.util.ArrayList<>();
        if (targetStudentNumberStr != null && !targetStudentNumberStr.isEmpty()) {
            // 单个学生分配
            try {
                Long stuNum = Long.valueOf(targetStudentNumberStr);
                Optional<Users> optStu = usersRepository.findByStudentNumber(stuNum);
                if (optStu.isEmpty()) {
                    resp.put("success", false);
                    resp.put("error", "STUDENT_NOT_FOUND");
                    return resp;
                }
                Users targetStu = optStu.get();
                if (targetStu.getClasses() == null || !className.equals(targetStu.getClasses().getClassName())) {
                    resp.put("success", false);
                    resp.put("error", "NOT_SAME_CLASS");
                    return resp;
                }
                UserTasks ut = new UserTasks();
                ut.setUser(targetStu);
                ut.setTask(saved);
                ut.setStatus("未开始");
                ut.setSubmitTime(null);
                ut.setScore(null);
                userTasksRepository.save(ut);
                createdCount = 1;
                createList.add(ut);
            } catch (NumberFormatException e) {
                resp.put("success", false);
                resp.put("error", "INVALID_STUDENT_NUMBER");
                return resp;
            }
        } else {
            // 为班级所有学生创建 user_tasks 记录
            java.util.List<com.example.demo.entity.Users> students = usersRepository.findByClasses_ClassNameOrderByStudentNumberAsc(className);
            for (com.example.demo.entity.Users stu : students) {
                UserTasks ut = new UserTasks();
                ut.setUser(stu);
                ut.setTask(saved);
                ut.setStatus("未开始");
                ut.setSubmitTime(null);
                ut.setScore(null);
                createList.add(ut);
            }
            if(!createList.isEmpty()) {
                userTasksRepository.saveAll(createList);
                createdCount = createList.size();
            }
        }

        resp.put("success", true);
        resp.put("data", mapTask(saved));
        resp.put("createdUserTasks", createdCount);
        resp.put("assignMode", targetStudentNumberStr != null && !targetStudentNumberStr.isEmpty() ? "single" : "class");
        return resp;
    }

    private Map<String,Object> mapTask(Tasks t){
        Map<String,Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("deadline", t.getDeadline());
        m.put("templateCode", t.getTemplateCode());
        m.put("classId", t.getClasses() != null ? t.getClasses().getClassName() : null);
        return m;
    }

    private Map<String,Object> mapUserTask(UserTasks ut) {
        Map<String,Object> m = new HashMap<>();
        m.put("id", ut.getId());
        m.put("status", ut.getStatus());
        m.put("score", ut.getScore());
        m.put("submitTime", ut.getSubmitTime());
        if (ut.getTask() != null) {
            m.put("task", mapTask(ut.getTask()));
            m.put("taskId", ut.getTask().getId());
        }
        Users student = ut.getUser();
        if (student != null) {
            Map<String,Object> stu = new HashMap<>();
            stu.put("id", student.getId());
            stu.put("name", student.getName());
            stu.put("studentNumber", student.getStudentNumber());
            stu.put("githubId", student.getGithubId());
            m.put("user", stu);
        }
        return m;
    }

    private TaskSubmissions fetchLatestSubmissionFor(UserTasks ut, Users student) {
        if (ut == null || ut.getTask() == null || student == null) { return null; }
        Long taskId = ut.getTask().getId();
        if (taskId == null) { return null; }
        if (student.getGithubId() != null && !student.getGithubId().isEmpty()) {
            java.util.List<TaskSubmissions> list = taskSubmissionsRepository.findAllByTaskAndGithubOrderByUpdateTimeDesc(taskId, student.getGithubId());
            return list.isEmpty() ? null : list.get(0);
        }
        if (student.getStudentNumber() != null) {
            java.util.List<TaskSubmissions> list = taskSubmissionsRepository.findAllByTask_IdAndUser_StudentNumberOrderByUpdateTimeDesc(taskId, student.getStudentNumber());
            return list.isEmpty() ? null : list.get(0);
        }
        return null;
    }

    private String stringVal(Object o){ return o==null? null : o.toString(); }

    private Users resolveCurrentUser(HttpSession session, OAuth2User principal) {
        // 1. 直接 session 缓存（学校账号或已手动放入）
        Object userObj = session.getAttribute("user");
        if (userObj instanceof Users) {
            return (Users) userObj;
        }
        // 2. GitHub OAuth2 登录
        if (principal != null) {
            String githubId = String.valueOf(principal.getAttribute("id"));
            Optional<Users> opt = usersRepository.findByGithubId(githubId);
            if (opt.isPresent()) {
                Users u = opt.get();
                session.setAttribute("user", u); // 放入 session 便于后续使用
                return u;
            }
        }
        // 3. 学校账号（通过 studentId/password 保存在 session）
        Object sid = session.getAttribute("studentId");
        Object pwd = session.getAttribute("password");
        if (sid != null && pwd != null) {
            try {
                Optional<Users> uOpt = usersRepository.findByStudentNumberAndPassword(Long.valueOf(sid.toString()), pwd.toString());
                if (uOpt.isPresent()) {
                    Users u = uOpt.get();
                    session.setAttribute("user", u);
                    return u;
                }
            } catch (NumberFormatException ignore) {}
        }
        return null;
    }
}
