package com.ucc.attendance.controller;

import com.ucc.attendance.domain.Department;
import com.ucc.attendance.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    @GetMapping
    public List<Map<String, String>> list() {
        return departmentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    private Map<String, String> toDto(Department d) {
        return Map.of("code", d.getCode(), "name", d.getName());
    }
}
