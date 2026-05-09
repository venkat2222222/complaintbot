package com.complaint.complaintbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ComplaintRequestDto {
    @NotBlank(message = "Username is required")
    private String userName;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Issue type is required")
    private String issueType;

    @NotBlank(message = "Issue description is required")
    private String issueDescription;
}
