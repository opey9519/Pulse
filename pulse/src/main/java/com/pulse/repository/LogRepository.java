package com.pulse.repository;

import com.pulse.model.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository extends JpaRepository<LogEvent, Long> {
}