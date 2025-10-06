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

    @Column(name = "template_code")
    private Integer templateCode; // 模板代码（取自模板 JSON 中的 templateCode 数值）
    
    @Column(name = "submit")
    private Boolean submit = false; // 是否已正式提交（false=草稿或未最终提交，true=最终提交）
    
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

    public TaskSubmissions(String taskUrl, String githubId, String aiContextUrl, Users user, Tasks task, Integer templateCode) {
        this.taskUrl = taskUrl;
        this.githubId = githubId;
        this.aiContextUrl = aiContextUrl;
        this.user = user;
        this.task = task;
        this.templateCode = templateCode;
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

    public Integer getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(Integer templateCode) {
        this.templateCode = templateCode;
    }
    
    public Boolean getSubmit() {
        return submit;
    }

    public void setSubmit(Boolean submit) {
        this.submit = submit;
    }
    
    // 在保存前自动更新时间
    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updateTime = LocalDateTime.now();
    }
}