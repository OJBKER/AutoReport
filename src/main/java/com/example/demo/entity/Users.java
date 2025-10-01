package com.example.demo.entity;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;

@Entity(name = "users")
public class Users implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(name = "student_number")
    private Long studentNumber;
    private String password;
    private String githubId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", referencedColumnName = "class_id")
    private Classes classes;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private java.util.List<UserTasks> userTasks;

    public Users() {}
    public Users(Long id, String name, Long studentNumber, String password, String githubId, Classes classes) {
        this.id = id;
        this.name = name;
        this.studentNumber = studentNumber;
        this.password = password;
        this.githubId = githubId;
        this.classes = classes;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getStudentNumber() { return studentNumber; }
    public void setStudentNumber(Long studentNumber) { this.studentNumber = studentNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getGithubId() { return githubId; }
    public void setGithubId(String githubId) { this.githubId = githubId; }

    public Classes getClasses() { return classes; }
    public void setClasses(Classes classes) { this.classes = classes; }

    public java.util.List<UserTasks> getUserTasks() { return userTasks; }
    public void setUserTasks(java.util.List<UserTasks> userTasks) { this.userTasks = userTasks; }
}
