package com.complaint.complaintbot.service;

import com.complaint.complaintbot.dto.ComplaintForm;
import com.complaint.complaintbot.entity.Complaint;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ComplaintService {
    Complaint saveComplaint(ComplaintForm form);
    List<Complaint> getAllComplaints();
    Complaint getComplaintById(Long id);
}
