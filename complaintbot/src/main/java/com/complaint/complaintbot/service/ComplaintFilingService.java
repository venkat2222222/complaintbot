package com.complaint.complaintbot.service;

import com.complaint.complaintbot.dto.ComplaintDetails;
import com.complaint.complaintbot.dto.ComplaintFilingResult;
import com.complaint.complaintbot.dto.NavigationAction;
import com.complaint.complaintbot.entity.BrowserSession;
import com.complaint.complaintbot.entity.Message;
import com.complaint.complaintbot.entity.SessionState;
import com.complaint.complaintbot.repository.BrowserSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintFilingService {

    private final BrowserSessionRepository sessionRepository;
    private final PlaywrightNavigationService playwrightService;
    private final GeminiChatService geminiChatService;
    private final ObjectMapper objectMapper;

    @Value("${complaint.portal.base-url}")
    private String portalBaseUrl;

    @Value("${complaint.portal.max-navigation-steps:20}")
    private int maxSteps;

    @Value("${complaint.portal.mobile-number}")
    private String configuredMobile;

    private static final int HTML_MAX_CHARS = 80_000;

    // ── Entry point: start filing ──────────────────────────────────────────────

    public ComplaintFilingResult fileComplaint(UUID chatId, String userMessage) {
        ComplaintDetails details = classifyComplaint(userMessage);

        if (!details.isComplaint()) {
            return ComplaintFilingResult.notComplaint();
        }

        // Persist or update session
        BrowserSession session = sessionRepository.findById(chatId).orElseGet(() -> {
            BrowserSession s = new BrowserSession();
            s.setChatId(chatId);
            return s;
        });
        session.setState(SessionState.NAVIGATING);
        session.setComplaintDetailsJson(toJson(details));
        session.setStepSummariesJson(toJson(new ArrayList<>()));
        session.setStepCount(0);
        sessionRepository.save(session);

        // Navigate to portal home
        String html = playwrightService.navigateTo(chatId, portalBaseUrl);
        playwrightService.saveSession(chatId);

        return runNavigationLoop(chatId, details, new ArrayList<>(), html);
    }

    // ── Called when user sends OTP ─────────────────────────────────────────────

    public ComplaintFilingResult submitOtp(UUID chatId, String otp) {
        BrowserSession session = sessionRepository.findById(chatId)
                .orElseThrow(() -> new IllegalStateException("No session for chat: " + chatId));

        if (session.getState() != SessionState.AWAITING_OTP) {
            return ComplaintFilingResult.failed("Session is not awaiting OTP.", List.of());
        }

        ComplaintDetails details = fromJson(session.getComplaintDetailsJson(), ComplaintDetails.class);
        List<String> summaries = fromJsonList(session.getStepSummariesJson());

        // Build OTP fill action using AI
        String html = playwrightService.getCurrentHtml(chatId);
        String trimmed = PlaywrightNavigationService.trimHtml(html, HTML_MAX_CHARS);
        String url = playwrightService.getCurrentUrl(chatId);

        String otpPrompt = PromptTemplates.OTP_PROMPT.formatted(otp, url, trimmed, otp);
        String aiResponse = geminiChatService.chat(List.of(), otpPrompt);
        NavigationAction fillAction = parseAction(aiResponse);

        html = playwrightService.executeAction(chatId, fillAction);
        summaries.add("Filled OTP into " + fillAction.selector());

        // Now click submit if needed — ask AI again
        session.setState(SessionState.NAVIGATING);
        session.setStepSummariesJson(toJson(summaries));
        sessionRepository.save(session);
        playwrightService.saveSession(chatId);

        // Ask AI what to do after OTP fill (usually submit)
        trimmed = PlaywrightNavigationService.trimHtml(html, HTML_MAX_CHARS);
        url = playwrightService.getCurrentUrl(chatId);
        String navPrompt = buildNavPrompt(details, summaries, url, trimmed);
        aiResponse = geminiChatService.chat(List.of(), navPrompt);
        NavigationAction submitAction = parseAction(aiResponse);

        if (!"DONE".equals(submitAction.actionType()) && !submitAction.done()) {
            html = playwrightService.executeAction(chatId, submitAction);
            summaries.add(submitAction.summary());
        }

        playwrightService.saveSession(chatId);
        return runNavigationLoop(chatId, details, summaries, html);
    }

    // ── Main navigation loop ───────────────────────────────────────────────────

    private ComplaintFilingResult runNavigationLoop(UUID chatId, ComplaintDetails details,
                                                     List<String> summaries, String html) {
        BrowserSession session = sessionRepository.findById(chatId).orElseThrow();

        for (int i = session.getStepCount(); i < maxSteps; i++) {
            String trimmed = PlaywrightNavigationService.trimHtml(html, HTML_MAX_CHARS);
            String url = playwrightService.getCurrentUrl(chatId);
            String prompt = buildNavPrompt(details, summaries, url, trimmed);

            String aiResponse = geminiChatService.chat(List.of(), prompt);
            log.info("[Step {}] AI response: {}", i, aiResponse);

            NavigationAction action = parseAction(aiResponse);
            summaries.add(action.summary());

            // Update session
            session.setStepCount(i + 1);
            session.setStepSummariesJson(toJson(summaries));

            if (action.done() || "DONE".equals(action.actionType())) {
                session.setState(SessionState.COMPLETED);
                session.setReferenceNumber(action.referenceNumber());
                sessionRepository.save(session);
                return ComplaintFilingResult.success(action.referenceNumber(), summaries);
            }

            if (action.awaitingOtp() || "AWAIT_OTP".equals(action.actionType())) {
                session.setState(SessionState.AWAITING_OTP);
                sessionRepository.save(session);
                playwrightService.saveSession(chatId);
                return ComplaintFilingResult.awaitingOtp(summaries);
            }

            try {
                html = playwrightService.executeAction(chatId, action);
            } catch (Exception e) {
                log.error("Playwright action failed at step {}: {}", i, e.getMessage());
                session.setState(SessionState.FAILED);
                sessionRepository.save(session);
                return ComplaintFilingResult.failed("Navigation error: " + e.getMessage(), summaries);
            }

            playwrightService.saveSession(chatId);
            sessionRepository.save(session);
        }

        session.setState(SessionState.FAILED);
        sessionRepository.save(session);
        return ComplaintFilingResult.failed("Max navigation steps reached without filing.", summaries);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private ComplaintDetails classifyComplaint(String userMessage) {
        String prompt = PromptTemplates.CLASSIFICATION_PROMPT.formatted(userMessage);
        String response = geminiChatService.chat(List.of(), prompt);
        return fromJson(stripMarkdown(response), ComplaintDetails.class);
    }

    private String buildNavPrompt(ComplaintDetails details, List<String> summaries, String url, String html) {
        // Inject configured mobile if applicant phone is missing
        ComplaintDetails enriched = new ComplaintDetails(
                details.isComplaint(),
                details.category(),
                details.description(),
                details.district(),
                details.applicantName(),
                details.phone() != null ? details.phone() : configuredMobile,
                details.additionalInfo()
        );
        return PromptTemplates.NAVIGATION_PROMPT.formatted(
                toJson(enriched),
                String.join("\n", summaries),
                url,
                html
        );
    }

    private NavigationAction parseAction(String aiResponse) {
        try {
            return objectMapper.readValue(stripMarkdown(aiResponse), NavigationAction.class);
        } catch (Exception e) {
            log.error("Failed to parse AI action response: {}", aiResponse, e);
            return new NavigationAction("UNKNOWN", null, null, null, "Parse error", false, false, null);
        }
    }

    private String stripMarkdown(String s) {
        if (s == null) return "{}";
        s = s.strip();
        if (s.startsWith("```")) {
            s = s.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").strip();
        }
        return s;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "{}"; }
    }

    private <T> T fromJson(String json, Class<T> cls) {
        try { return objectMapper.readValue(json != null ? json : "{}", cls); }
        catch (Exception e) { throw new RuntimeException("JSON parse error", e); }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJsonList(String json) {
        try { return objectMapper.readValue(json != null ? json : "[]", List.class); }
        catch (Exception e) { return new ArrayList<>(); }
    }

    public boolean isAwaitingOtp(UUID chatId) {
        return sessionRepository.findById(chatId)
                .map(s -> s.getState() == SessionState.AWAITING_OTP)
                .orElse(false);
    }
}
