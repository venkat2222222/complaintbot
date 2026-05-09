package com.complaint.complaintbot.repository;

import com.complaint.complaintbot.entity.BrowserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BrowserSessionRepository extends JpaRepository<BrowserSession, UUID> {
}
