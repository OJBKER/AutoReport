// 无需补充，已含 @ManyToOne 关联
package com.example.demo.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.io.Serializable;

@Entity(name = "user_tasks")
public class UserTasks implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "student_number")
    private Users user;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Tasks task;

    private String status; // 完成状态
    private LocalDateTime submitTime;
    private Integer score;

    public UserTasks() {}
    public UserTasks(Long id, Users user, Tasks task, String status, LocalDateTime submitTime, Integer score) {
        this.id = id;
        this.user = user;
        this.task = task;
        this.status = status;
        this.submitTime = submitTime;
        this.score = score;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Users getUser() { return user; }
    public void setUser(Users user) { this.user = user; }
    public Tasks getTask() { return task; }
    public void setTask(Tasks task) { this.task = task; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSubmitTime() { return submitTime; }
    public void setSubmitTime(LocalDateTime submitTime) { this.submitTime = submitTime; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
