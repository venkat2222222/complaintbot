package com.complaint.complaintbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ComplaintForm {

    @NotBlank(message = "Name is required")
    private String userName;

    private String title;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Issue type is required")
    private String issueType;

    @NotBlank(message = "Issue description is required")
    private String issueDescription;

    private MultipartFile image;
}
