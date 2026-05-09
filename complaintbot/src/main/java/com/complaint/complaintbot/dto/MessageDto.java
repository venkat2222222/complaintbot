package com.complaint.complaintbot.dto;

import com.complaint.complaintbot.entity.Message;

import java.time.LocalDateTime;

public record MessageDto(Long id, String role, String content, LocalDateTime createdAt) {

    public static MessageDto from(Message message) {
        return new MessageDto(
                message.getId(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
