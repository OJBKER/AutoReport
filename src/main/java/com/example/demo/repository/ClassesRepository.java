package com.example.demo.repository;

import com.example.demo.entity.Classes;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ClassesRepository extends JpaRepository<Classes, String> {
    Optional<Classes> findByClassName(String className);
}
