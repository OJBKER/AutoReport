package com.example.demo.repository;

import com.example.demo.entity.Tasks;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TasksRepository extends JpaRepository<Tasks, Long> {
	// 按班级ID查找任务（现在使用className作为主键）
	java.util.List<Tasks> findByClasses_ClassName(String className);
}
