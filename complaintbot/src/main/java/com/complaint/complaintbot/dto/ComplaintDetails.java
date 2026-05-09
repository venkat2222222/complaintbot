package com.complaint.complaintbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ComplaintDetails(
        boolean isComplaint,
        String category,
        String description,
        String district,
        String applicantName,
        String phone,
        String additionalInfo
) {}
