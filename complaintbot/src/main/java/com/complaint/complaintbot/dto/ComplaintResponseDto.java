package com.complaint.complaintbot.dto;

import com.complaint.complaintbot.entity.ComplaintStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ComplaintResponseDto {
    private Long id;
    private String userName;
    private String location;
    private String issueType;
    private String issueDescription;
    private String imagePath;
    private ComplaintStatus complaintStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
