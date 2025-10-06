package com.example.demo.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;

@Entity(name = "tasks")
public class Tasks implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private LocalDateTime deadline;

    @Column(name = "template_code")
    private Integer templateCode; // 对应报告模板代码（可为空表示未指定）

    @ManyToOne
    @JoinColumn(name = "class_id", referencedColumnName = "class_id")
    private Classes classes;

    @OneToMany(mappedBy = "task")
    @JsonIgnore
    private java.util.List<UserTasks> userTasks;

    public Tasks() {}
    public Tasks(Long id, String title, String description, LocalDateTime deadline, Classes classes, Integer templateCode) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.classes = classes;
        this.templateCode = templateCode;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public Classes getClasses() { return classes; }
    public void setClasses(Classes classes) { this.classes = classes; }
    public Integer getTemplateCode() { return templateCode; }
    public void setTemplateCode(Integer templateCode) { this.templateCode = templateCode; }
}
