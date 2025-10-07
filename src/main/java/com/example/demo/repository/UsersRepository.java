package com.example.demo.repository;

import com.example.demo.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
	Optional<Users> findByGithubId(String githubId);
	Optional<Users> findByStudentNumberAndPassword(Long studentNumber, String password);
	// 仅按学号查找（用于管理员单独分配任务等场景）
	Optional<Users> findByStudentNumber(Long studentNumber);
	
	// 按班级统计学生数量
	Long countByClasses_ClassName(String className);

	// 获取同一班级全部用户（用于管理员查看同班级学生）
	java.util.List<Users> findByClasses_ClassNameOrderByStudentNumberAsc(String className);
}
