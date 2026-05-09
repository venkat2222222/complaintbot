package com.complaint.complaintbot.repository;

import com.complaint.complaintbot.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
    List<Chat> findAllByOrderByUpdatedAtDesc();
}
