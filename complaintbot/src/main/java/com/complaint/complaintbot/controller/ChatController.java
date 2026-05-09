package com.complaint.complaintbot.controller;

import com.complaint.complaintbot.dto.ChatRequest;
import com.complaint.complaintbot.dto.ChatResponse;
import com.complaint.complaintbot.dto.ChatSummaryDto;
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

    /** GET /api/chats — list all chats (no messages) */
    @GetMapping
    public List<ChatSummaryDto> listChats() {
        return chatService.listChats();
    }

    /** GET /api/chats/{chatId} — get a specific chat with full message history */
    @GetMapping("/{chatId}")
    public ChatResponse getChat(@PathVariable UUID chatId) {
        return chatService.getChat(chatId);
    }

    /** POST /api/chats — start a new chat */
    @PostMapping
    public ResponseEntity<ChatResponse> createChat(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.createChat(request.message());
        return ResponseEntity.ok(response);
    }

    /** POST /api/chats/{chatId}/messages — continue an existing chat */
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<ChatResponse> continueChat(
            @PathVariable UUID chatId,
            @RequestBody ChatRequest request) {
        ChatResponse response = chatService.continueChat(chatId, request.message());
        return ResponseEntity.ok(response);
    }
}
