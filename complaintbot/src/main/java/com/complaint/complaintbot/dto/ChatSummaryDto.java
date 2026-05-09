package com.complaint.complaintbot.dto;

import com.complaint.complaintbot.entity.Chat;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatSummaryDto(UUID id, String title, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static ChatSummaryDto from(Chat chat) {
        return new ChatSummaryDto(chat.getId(), chat.getTitle(), chat.getCreatedAt(), chat.getUpdatedAt());
    }
}
