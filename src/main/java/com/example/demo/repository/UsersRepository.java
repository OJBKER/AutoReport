package com.example.demo.repository;

import com.example.demo.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
	Optional<Users> findByGithubId(String githubId);
	Optional<Users> findByStudentNumberAndPassword(Long studentNumber, String password);
	
	// 按班级统计学生数量
	Long countByClasses_ClassName(String className);
}
