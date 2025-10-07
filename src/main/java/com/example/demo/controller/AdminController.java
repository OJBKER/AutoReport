package com.example.demo.controller;

import com.example.demo.entity.Users;
import com.example.demo.entity.Classes;
import com.example.demo.repository.UsersRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;

import javax.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UsersRepository usersRepository;

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
