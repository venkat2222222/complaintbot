package com.complaint.complaintbot.dto;

import com.complaint.complaintbot.entity.Chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ChatResponse(UUID id, String title, LocalDateTime createdAt, LocalDateTime updatedAt, List<MessageDto> messages) {

    public static ChatResponse from(Chat chat, List<MessageDto> messages) {
        return new ChatResponse(chat.getId(), chat.getTitle(), chat.getCreatedAt(), chat.getUpdatedAt(), messages);
    }
}
