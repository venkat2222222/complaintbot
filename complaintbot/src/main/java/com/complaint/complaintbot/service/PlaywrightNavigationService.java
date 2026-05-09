package com.complaint.complaintbot.service;

import com.complaint.complaintbot.dto.NavigationAction;
import com.complaint.complaintbot.entity.BrowserSession;
import com.complaint.complaintbot.repository.BrowserSessionRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaywrightNavigationService {

    private final BrowserSessionRepository sessionRepository;

    @Value("${playwright.headless:true}")
    private boolean headless;

    private Playwright playwright;
    private Browser browser;

    private final Map<UUID, BrowserContext> contexts = new ConcurrentHashMap<>();
    private final Map<UUID, Page> pages = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(headless)
        );
        log.info("Playwright browser started (headless={})", headless);
    }

    @PreDestroy
    public void cleanup() {
        pages.values().forEach(p -> { try { p.close(); } catch (Exception ignored) {} });
        contexts.values().forEach(c -> { try { c.close(); } catch (Exception ignored) {} });
        try { if (browser != null) browser.close(); } catch (Exception ignored) {}
        try { if (playwright != null) playwright.close(); } catch (Exception ignored) {}
        log.info("Playwright browser closed");
    }

    /** Get existing page or create a new one, restoring storage state from DB if available. */
    public Page getOrCreatePage(UUID chatId) {
        if (pages.containsKey(chatId)) {
            return pages.get(chatId);
        }

        BrowserContext context;
        BrowserSession session = sessionRepository.findById(chatId).orElse(null);

        if (session != null && session.getStorageStateJson() != null) {
            try {
                Path tmp = Files.createTempFile("pw-session-" + chatId, ".json");
                Files.writeString(tmp, session.getStorageStateJson());
                context = browser.newContext(new Browser.NewContextOptions().setStorageStatePath(tmp));
                tmp.toFile().deleteOnExit();
                log.info("Restored browser context from DB for chat {}", chatId);
            } catch (Exception e) {
                log.warn("Could not restore session, creating fresh context: {}", e.getMessage());
                context = browser.newContext();
            }
        } else {
            context = browser.newContext();
        }

        Page page = context.newPage();
        contexts.put(chatId, context);
        pages.put(chatId, page);
        return page;
    }

    /** Navigate to URL and return full page HTML. */
    public String navigateTo(UUID chatId, String url) {
        Page page = getOrCreatePage(chatId);
        page.navigate(url);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        return page.content();
    }

    /** Execute a NavigationAction and return resulting page HTML. */
    public String executeAction(UUID chatId, NavigationAction action) {
        Page page = pages.get(chatId);
        if (page == null) throw new IllegalStateException("No active page for chat: " + chatId);

        switch (action.actionType()) {
            case "FILL" -> page.fill(action.selector(), action.value() != null ? action.value() : "");
            case "CLICK", "SUBMIT" -> {
                page.click(action.selector());
                page.waitForLoadState(LoadState.NETWORKIDLE);
            }
            case "SELECT" -> page.selectOption(action.selector(), action.value());
            case "NAVIGATE" -> {
                page.navigate(action.navigateUrl());
                page.waitForLoadState(LoadState.NETWORKIDLE);
            }
            default -> log.warn("Unknown actionType: {}", action.actionType());
        }

        return page.content();
    }

    /** Get current page URL. */
    public String getCurrentUrl(UUID chatId) {
        Page page = pages.get(chatId);
        return page != null ? page.url() : null;
    }

    /** Get current page HTML without navigating. */
    public String getCurrentHtml(UUID chatId) {
        Page page = pages.get(chatId);
        return page != null ? page.content() : "";
    }

    /** Save browser storage state (cookies, localStorage) to DB. */
    public void saveSession(UUID chatId) {
        BrowserContext ctx = contexts.get(chatId);
        if (ctx == null) return;
        try {
            String state = ctx.storageState();
            sessionRepository.findById(chatId).ifPresent(s -> {
                s.setStorageStateJson(state);
                s.setCurrentUrl(getCurrentUrl(chatId));
                sessionRepository.save(s);
            });
        } catch (Exception e) {
            log.warn("Could not save session for chat {}: {}", chatId, e.getMessage());
        }
    }

    /** Close and remove a page for a chat. */
    public void closePage(UUID chatId) {
        try { pages.remove(chatId); } catch (Exception ignored) {}
        BrowserContext ctx = contexts.remove(chatId);
        if (ctx != null) try { ctx.close(); } catch (Exception ignored) {}
    }

    /** Trim HTML to a manageable size — send body content only. */
    public static String trimHtml(String html, int maxChars) {
        if (html == null) return "";
        int bodyStart = html.indexOf("<body");
        int bodyEnd = html.lastIndexOf("</body>");
        if (bodyStart >= 0 && bodyEnd > bodyStart) {
            html = html.substring(bodyStart, bodyEnd + 7);
        }
        if (html.length() > maxChars) {
            html = html.substring(0, maxChars) + "\n<!-- HTML TRIMMED -->";
        }
        return html;
    }
}
