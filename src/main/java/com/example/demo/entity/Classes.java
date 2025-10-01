package com.example.demo.entity;

import javax.persistence.*;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity(name = "classes")
public class Classes {
    @Id
    @Column(name = "class_id")
    private String className;

    @Column(name = "class_name")
    private String name;

    @OneToMany(mappedBy = "classes")
    @JsonIgnore
    private List<Users> students;

    @OneToMany(mappedBy = "classes")
    @JsonIgnore
    private List<Tasks> tasks;

    public Classes() {}
    public Classes(String className, String name) {
        this.className = className;
        this.name = name;
    }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public List<Users> getStudents() { return students; }
    public void setStudents(List<Users> students) { this.students = students; }
    
    public List<Tasks> getTasks() { return tasks; }
    public void setTasks(List<Tasks> tasks) { this.tasks = tasks; }
}
