package com.complaint.complaintbot.service;

public final class PromptTemplates {

    private PromptTemplates() {}

    public static final String CLASSIFICATION_PROMPT = """
            You are a complaint classification assistant for the Tamil Nadu CM Helpline portal.
            Analyze the user's message and extract the following.
            Return ONLY valid JSON, no markdown, no explanation.

            {
              "isComplaint": true,
              "category": "Infrastructure|Water Supply|Electricity|Roads|Sanitation|Other",
              "description": "Clear description of the complaint",
              "district": "District name if mentioned, else null",
              "applicantName": "Name if mentioned, else null",
              "phone": "Phone number if mentioned, else null",
              "additionalInfo": "Any other relevant detail"
            }

            If it is NOT a complaint, return {"isComplaint": false, "category": null, "description": null, "district": null, "applicantName": null, "phone": null, "additionalInfo": null}

            User message: %s
            """;

    public static final String NAVIGATION_PROMPT = """
            You are an autonomous agent navigating the Tamil Nadu CM Helpline portal to file a complaint.

            Complaint details:
            %s

            Steps completed so far:
            %s

            Current page URL: %s

            Current page HTML:
            %s

            Determine the next single action to take to progress filing this complaint.
            If the page is asking for a mobile number, fill it using the applicant's phone or the configured mobile.
            If the page is asking for OTP, set awaitingOtp=true so the user can provide it.
            If the complaint was successfully filed and you see a reference number, set done=true.

            Return ONLY valid JSON, no markdown:
            {
              "actionType": "CLICK|FILL|SELECT|NAVIGATE|SUBMIT|AWAIT_OTP|DONE",
              "selector": "CSS selector using class or id from the HTML (e.g. .submit-btn, #mobileNo)",
              "value": "value to fill or option to select",
              "navigateUrl": "full URL only for NAVIGATE action, else null",
              "summary": "One sentence: what you are doing and why",
              "done": false,
              "awaitingOtp": false,
              "referenceNumber": "reference number string if visible, else null"
            }
            """;

    public static final String OTP_PROMPT = """
            You are an autonomous agent navigating the Tamil Nadu CM Helpline portal.
            The user has provided the OTP: %s

            Current page URL: %s

            Current page HTML:
            %s

            Fill the OTP into the correct input field and submit.
            Return ONLY valid JSON:
            {
              "actionType": "FILL",
              "selector": "CSS selector for the OTP input field",
              "value": "%s",
              "navigateUrl": null,
              "summary": "Filling OTP field",
              "done": false,
              "awaitingOtp": false,
              "referenceNumber": null
            }
            """;
}
