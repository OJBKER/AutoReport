package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.demo.repository.UsersRepository;
import java.util.Optional;
import com.example.demo.entity.Users;
import com.example.demo.entity.Classes;
import com.example.demo.entity.Tasks;
import com.example.demo.entity.UserTasks;
import javax.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PutMapping;
import com.example.demo.repository.ClassesRepository;

@RestController
public class UserController {
    @Autowired
    private UsersRepository usersRepository;
    
    @Autowired
    private ClassesRepository classesRepository;
    
    @PostMapping("/api/slogin")
    public Map<String, Object> sLogin(@RequestBody Map<String, String> body, HttpSession session) {
        String studentId = body.get("studentId");
        String password = body.get("password");
        Map<String, Object> result = new HashMap<>();
        
        if (studentId == null || password == null) {
            result.put("success", false);
            result.put("message", "学号和密码不能为空");
            return result;
        }
        
        Optional<Users> userOpt = usersRepository.findByStudentNumberAndPassword(Long.valueOf(studentId), password);
        boolean exists = userOpt.isPresent();
        result.put("success", exists);
        result.put("message", exists ? "安全检查通过" : "学号或密码错误");
        
        if (exists) {
            session.setAttribute("studentId", studentId);
            session.setAttribute("password", password);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(studentId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        return result;
    }

    @GetMapping("/api/user/me")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OAuth2User principal, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        if (principal != null) {
            // GitHub OAuth2 用户
            return buildGitHubUserResponse(principal, result);
        }
        
        // 检查学校用户 session
        Object studentIdObj = session.getAttribute("studentId");
        Object passwordObj = session.getAttribute("password");
        if (studentIdObj != null && passwordObj != null) {
            return buildSchoolUserResponse(studentIdObj.toString(), passwordObj.toString(), result);
        }
        
        result.put("error", "No authentication found");
        return result;
    }
    
    private Map<String, Object> buildGitHubUserResponse(OAuth2User principal, Map<String, Object> result) {
        String githubId = principal.getAttribute("id").toString();
        result.put("id", githubId);
        result.put("login", principal.getAttribute("login"));
        result.put("name", principal.getAttribute("name"));
        result.put("email", principal.getAttribute("email"));
        result.put("avatarUrl", principal.getAttribute("avatar_url"));
        result.put("loginType", "github");
        
        Optional<Users> userOpt = usersRepository.findByGithubId(githubId);
        if (userOpt.isPresent()) {
            // 现有用户
            Users user = userOpt.get();
            result.put("userId", user.getId());
            result.put("githubIdExists", true);
            // 返回 studentNumber（可能为空）
            result.put("studentNumber", user.getStudentNumber());
            if (user.getStudentNumber() == null) {
                result.put("needsBinding", true); // 前端可据此提示绑定
            } else {
                result.put("needsBinding", false);
            }
            addClassInfo(result, user);
            addTasksInfo(result, user);
            addUserTasksInfo(result, user);
        } else {
            // 首次GitHub登录，自动创建用户记录
            Users newUser = new Users();
            newUser.setName(principal.getAttribute("name"));
            newUser.setGithubId(githubId);
            // 暂时不设置班级，等待后续绑定
            newUser.setClasses(null);
            
            try {
                Users savedUser = usersRepository.save(newUser);
                result.put("userId", savedUser.getId());
                result.put("githubIdExists", false);
                result.put("isNewUser", true);
                result.put("message", "首次GitHub登录，已自动创建账户");
                result.put("studentNumber", null);
                result.put("needsBinding", true);
                
                // 新用户暂无班级和任务信息
                result.put("classes", null);
                result.put("userRelatedTasks", new ArrayList<>());
                result.put("userTasks", new ArrayList<>());
            } catch (Exception e) {
                result.put("githubIdExists", false);
                result.put("isNewUser", false);
                result.put("error", "创建用户记录失败");
                result.put("dbUser", "创建失败");
            }
        }
        return result;
    }
    
    private Map<String, Object> buildSchoolUserResponse(String studentId, String password, Map<String, Object> result) {
        result.put("studentId", studentId); // 兼容旧字段
        result.put("loginType", "school");
        Optional<Users> userOpt = usersRepository.findByStudentNumberAndPassword(Long.valueOf(studentId), password);
        if (userOpt.isPresent()) {
            Users user = userOpt.get();
            result.put("userId", user.getId());
            result.put("name", user.getName());
            result.put("studentNumber", user.getStudentNumber());
            result.put("needsBinding", false);
            addClassInfo(result, user);
            addTasksInfo(result, user);
            addUserTasksInfo(result, user);
        } else {
            result.put("studentNumber", null);
            result.put("needsBinding", true);
        }
        return result;
    }
    
    private void addClassInfo(Map<String, Object> result, Users user) {
        if (user.getClasses() != null) {
            Map<String, Object> classInfo = new HashMap<>();
            classInfo.put("classId", user.getClasses().getClassName());
            classInfo.put("name", user.getClasses().getName());
            result.put("classes", classInfo);
        } else {
            result.put("classes", null);
        }
    }
    
    private void addTasksInfo(Map<String, Object> result, Users user) {
        // 通过 user->userTasks->tasks 链路获取用户相关的所有任务
        if (user.getUserTasks() != null) {
            List<UserTasks> userTasksList = user.getUserTasks();
            List<Map<String, Object>> tasksList = new ArrayList<>();
            
            for (UserTasks userTask : userTasksList) {
                if (userTask.getTask() != null) {
                    Tasks task = userTask.getTask();
                    Map<String, Object> taskInfo = new HashMap<>();
                    taskInfo.put("id", task.getId());
                    taskInfo.put("title", task.getTitle());
                    taskInfo.put("description", task.getDescription());
                    taskInfo.put("deadline", task.getDeadline());
                    tasksList.add(taskInfo);
                }
            }
            result.put("userRelatedTasks", tasksList);
        } else {
            result.put("userRelatedTasks", new ArrayList<>());
        }
    }
    
    private void addUserTasksInfo(Map<String, Object> result, Users user) {
        // 通过 user->userTasks 获取用户任务完成记录
        if (user.getUserTasks() != null) {
            List<UserTasks> userTasksList = user.getUserTasks();
            List<Map<String, Object>> userTasksInfoList = new ArrayList<>();
            
            for (UserTasks userTask : userTasksList) {
                Map<String, Object> userTaskInfo = new HashMap<>();
                userTaskInfo.put("id", userTask.getId());
                userTaskInfo.put("status", userTask.getStatus());
                userTaskInfo.put("submitTime", userTask.getSubmitTime());
                userTaskInfo.put("score", userTask.getScore());
                
                // 添加关联的任务信息 (userTasks->tasks)
                if (userTask.getTask() != null) {
                    Tasks task = userTask.getTask();
                    Map<String, Object> taskInfo = new HashMap<>();
                    taskInfo.put("id", task.getId());
                    taskInfo.put("title", task.getTitle());
                    taskInfo.put("description", task.getDescription());
                    taskInfo.put("deadline", task.getDeadline());
                    taskInfo.put("templateCode", task.getTemplateCode()); // 让前端按模板匹配可提交任务
                    userTaskInfo.put("task", taskInfo);
                }
                
                userTasksInfoList.add(userTaskInfo);
            }
            result.put("userTasks", userTasksInfoList);
        } else {
            result.put("userTasks", new ArrayList<>());
        }
    }
    
    // GitHub用户绑定学号和班级信息
    @PutMapping("/api/user/bind-student-info")
    public Map<String, Object> bindStudentInfo(@RequestBody Map<String, String> body, @AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> result = new HashMap<>();
        
        if (principal == null) {
            result.put("success", false);
            result.put("message", "未登录或非GitHub用户");
            return result;
        }
        
        String githubId = principal.getAttribute("id").toString();
        String studentNumber = body.get("studentNumber");
        String classId = body.get("classId");
        
        if (studentNumber == null || classId == null) {
            result.put("success", false);
            result.put("message", "学号和班级ID不能为空");
            return result;
        }
        
        try {
            // 查找GitHub用户
            Optional<Users> userOpt = usersRepository.findByGithubId(githubId);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }
            
            // 检查学号是否已被其他用户使用
            Optional<Users> existingUserOpt = usersRepository.findByStudentNumberAndPassword(Long.valueOf(studentNumber), null);
            if (existingUserOpt.isPresent() && !existingUserOpt.get().getGithubId().equals(githubId)) {
                result.put("success", false);
                result.put("message", "该学号已被其他用户绑定");
                return result;
            }
            
            // 查找班级是否存在
            Optional<Classes> classOpt = classesRepository.findByClassName(classId);
            if (!classOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "班级不存在");
                return result;
            }
            
            // 更新用户信息
            Users user = userOpt.get();
            user.setStudentNumber(Long.valueOf(studentNumber));
            user.setClasses(classOpt.get());
            
            usersRepository.save(user);
            
            result.put("success", true);
            result.put("message", "学号和班级信息绑定成功");
            result.put("studentNumber", studentNumber);
            result.put("classId", classId);
            result.put("className", classOpt.get().getName());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "绑定失败：" + e.getMessage());
        }
        
        return result;
    }
}