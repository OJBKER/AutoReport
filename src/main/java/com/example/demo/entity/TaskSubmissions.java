package com.example.demo.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_submissions")
public class TaskSubmissions implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "task_url", columnDefinition = "TEXT")
    private String taskUrl; // 存储任务的URL
    
    
    @Column(name = "github_id", columnDefinition = "TEXT")
    private String githubId; // GitHub的ID
    
    @Column(name = "update_time")
    private LocalDateTime updateTime; // 更新时间
    
    @Column(name = "ai_context_url", columnDefinition = "TEXT")
    private String aiContextUrl; // 存储和AI对话的上下文记录的URL

    @Column(name = "template_type", length = 64)
    private String templateType; // 模板类型标识（如 software / physics 等）
    
    // 关联到Users表（通过student_number字段）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "student_number")
    private Users user;

    // 关联到Tasks表
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", referencedColumnName = "id")
    private Tasks task;

    // 构造函数
    public TaskSubmissions() {}

    public TaskSubmissions(String taskUrl, String githubId, String aiContextUrl, Users user, Tasks task, String templateType) {
        this.taskUrl = taskUrl;
        this.githubId = githubId;
        this.aiContextUrl = aiContextUrl;
        this.user = user;
        this.task = task;
        this.templateType = templateType;
        this.updateTime = LocalDateTime.now();
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskUrl() {
        return taskUrl;
    }

    public void setTaskUrl(String taskUrl) {
        this.taskUrl = taskUrl;
    }

    public String getGithubId() {
        return githubId;
    }

    public void setGithubId(String githubId) {
        this.githubId = githubId;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public String getAiContextUrl() {
        return aiContextUrl;
    }
    
    public void setAiContextUrl(String aiContextUrl) {
        this.aiContextUrl = aiContextUrl;
    }
    
    public Users getUser() {
        return user;
    }
    
    public void setUser(Users user) {
        this.user = user;
    }
    
    public Tasks getTask() {
        return task;
    }
    
    public void setTask(Tasks task) {
        this.task = task;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }
    
    // 在保存前自动更新时间
    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updateTime = LocalDateTime.now();
    }
}