package com.complaint.complaintbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NavigationAction(
        String actionType,   // CLICK | FILL | SELECT | NAVIGATE | SUBMIT | AWAIT_OTP | DONE
        String selector,     // CSS selector derived from HTML class/id
        String value,        // value for FILL / SELECT
        String navigateUrl,  // for NAVIGATE
        String summary,      // what AI is doing this step
        boolean done,        // complaint filed successfully
        boolean awaitingOtp, // OTP is needed from user
        String referenceNumber // reference number on success page
) {
    public NavigationAction {
        if (actionType == null) actionType = "UNKNOWN";
        if (summary == null) summary = "";
    }
}
