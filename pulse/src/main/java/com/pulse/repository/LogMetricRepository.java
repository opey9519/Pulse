package com.pulse.repository;

import com.pulse.model.LogMetric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogMetricRepository
        extends JpaRepository<LogMetric, Long> {
}