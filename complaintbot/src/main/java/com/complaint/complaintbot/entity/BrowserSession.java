package com.complaint.complaintbot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "browser_sessions")
@Getter
@Setter
@NoArgsConstructor
public class BrowserSession {

    @Id
    private UUID chatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionState state = SessionState.NAVIGATING;

    @Column(columnDefinition = "TEXT")
    private String storageStateJson;

    @Column(length = 2000)
    private String currentUrl;

    @Column(columnDefinition = "TEXT")
    private String complaintDetailsJson;

    @Column(columnDefinition = "TEXT")
    private String stepSummariesJson;

    @Column(length = 200)
    private String referenceNumber;

    private int stepCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
