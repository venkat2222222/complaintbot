package com.complaint.complaintbot.repository;

import com.complaint.complaintbot.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
}
