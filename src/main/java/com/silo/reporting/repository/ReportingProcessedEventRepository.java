package com.silo.reporting.repository;

import com.silo.reporting.entity.ReportingProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportingProcessedEventRepository extends JpaRepository<ReportingProcessedEvent, UUID> {
}
