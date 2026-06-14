package com.ucc.attendance.config;

import com.ucc.attendance.repository.LecturerRepository;
import com.ucc.attendance.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaleSessionReconciler implements ApplicationRunner {

    private final SessionService sessionService;
    private final LecturerRepository lecturerRepository;

    @Override
    public void run(ApplicationArguments args) {
        lecturerRepository.findAll()
                .forEach(lecturer -> sessionService.reconcileStaleActiveSessions(lecturer.getId()));
    }
}
