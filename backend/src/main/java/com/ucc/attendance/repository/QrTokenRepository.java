package com.ucc.attendance.repository;

import com.ucc.attendance.domain.QrToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QrTokenRepository extends JpaRepository<QrToken, Long> {
    Optional<QrToken> findByNonce(String nonce);
}
