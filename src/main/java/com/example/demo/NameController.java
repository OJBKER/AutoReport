package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/names")
public class NameController {
    @Autowired
    private NameRepository nameRepository;

    @GetMapping
    public List<String> getNames() {
        return nameRepository.findAll().stream().map(Name::getName).toList();
    }

    @PostMapping
    public List<String> addName(@RequestBody Name name) {
        nameRepository.save(new Name(name.getName()));
        return nameRepository.findAll().stream().map(Name::getName).toList();
    }
}
