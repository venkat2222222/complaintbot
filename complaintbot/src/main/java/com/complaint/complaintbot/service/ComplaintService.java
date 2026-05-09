package com.complaint.complaintbot.service;

import com.complaint.complaintbot.dto.ComplaintRequestDto;
import com.complaint.complaintbot.dto.ComplaintResponseDto;
import com.complaint.complaintbot.entity.ComplaintStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ComplaintService {
    ComplaintResponseDto createComplaint(ComplaintRequestDto request, MultipartFile image);
    List<ComplaintResponseDto> getAllComplaints();
    ComplaintResponseDto getComplaintById(Long id);
    ComplaintResponseDto updateComplaintStatus(Long id, ComplaintStatus status);
}
