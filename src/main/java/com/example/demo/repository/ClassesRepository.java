package com.example.demo.repository;

import com.example.demo.entity.Classes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassesRepository extends JpaRepository<Classes, String> {
}
