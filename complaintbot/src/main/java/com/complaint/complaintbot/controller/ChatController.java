package com.complaint.complaintbot.controller;

import com.complaint.complaintbot.dto.ChatRequest;
import com.complaint.complaintbot.dto.ChatResponse;
import com.complaint.complaintbot.dto.ChatSummaryDto;
import com.complaint.complaintbot.dto.ComplaintFilingResult;
import com.complaint.complaintbot.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** GET /api/chats — list all chats */
    @GetMapping
    public List<ChatSummaryDto> listChats() {
        return chatService.listChats();
    }

    /** GET /api/chats/{chatId} — get chat with full message history */
    @GetMapping("/{chatId}")
    public ChatResponse getChat(@PathVariable UUID chatId) {
        return chatService.getChat(chatId);
    }

    /** POST /api/chats — start a new chat */
    @PostMapping
    public ResponseEntity<ChatResponse> createChat(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.createChat(request.message()));
    }

    /**
     * POST /api/chats/{chatId}/messages — continue an existing chat.
     * If the session is AWAITING_OTP, the message is treated as the OTP.
     */
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatResponse> continueChat(
            @PathVariable UUID chatId,
            @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.continueChat(chatId, request.message()));
    }

    /**
     * POST /api/chats/{chatId}/file-complaint
     * Triggers the autonomous Playwright navigation to file the complaint.
     */
    @PostMapping("/{chatId}/file-complaint")
    public ResponseEntity<ComplaintFilingResult> fileComplaint(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.fileComplaint(chatId));
    }
}
