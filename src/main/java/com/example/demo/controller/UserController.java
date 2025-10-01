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
import java.util.Collections;

@RestController
public class UserController {
    @Autowired
    private UsersRepository usersRepository;
    
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
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(studentId, null, Collections.emptyList());
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
            Users user = userOpt.get();
            result.put("userId", user.getId());
            addClassInfo(result, user);
            addTasksInfo(result, user);
            addUserTasksInfo(result, user);
        }
        return result;
    }
    
    private Map<String, Object> buildSchoolUserResponse(String studentId, String password, Map<String, Object> result) {
        result.put("studentId", studentId);
        result.put("loginType", "school");
        
        Optional<Users> userOpt = usersRepository.findByStudentNumberAndPassword(Long.valueOf(studentId), password);
        if (userOpt.isPresent()) {
            Users user = userOpt.get();
            result.put("userId", user.getId());
            result.put("name", user.getName());
            addClassInfo(result, user);
            addTasksInfo(result, user);
            addUserTasksInfo(result, user);
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
                    userTaskInfo.put("task", taskInfo);
                }
                
                userTasksInfoList.add(userTaskInfo);
            }
            result.put("userTasks", userTasksInfoList);
        } else {
            result.put("userTasks", new ArrayList<>());
        }
    }
}