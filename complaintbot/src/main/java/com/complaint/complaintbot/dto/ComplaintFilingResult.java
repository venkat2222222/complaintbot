package com.complaint.complaintbot.dto;

import java.util.List;

public record ComplaintFilingResult(
        boolean success,
        boolean awaitingOtp,
        String referenceNumber,
        String message,
        List<String> stepSummaries
) {
    public static ComplaintFilingResult success(String refNum, List<String> steps) {
        return new ComplaintFilingResult(true, false, refNum,
                "Complaint filed successfully! Reference: " + refNum, steps);
    }

    public static ComplaintFilingResult awaitingOtp(List<String> steps) {
        return new ComplaintFilingResult(false, true, null,
                "OTP sent to your registered mobile. Please reply with the OTP to continue.", steps);
    }

    public static ComplaintFilingResult failed(String msg, List<String> steps) {
        return new ComplaintFilingResult(false, false, null, msg, steps);
    }

    public static ComplaintFilingResult notComplaint() {
        return new ComplaintFilingResult(false, false, null,
                "This doesn't look like a complaint. Please describe your grievance clearly.", List.of());
    }
}
