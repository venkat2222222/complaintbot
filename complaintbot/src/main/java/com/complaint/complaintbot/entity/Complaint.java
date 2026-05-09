package com.complaint.complaintbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userName;

    @Column(name = "title")
    private String title;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String issueType;

    @Column(nullable = false, length = 5000)
    private String issueDescription;

    @Column(name = "image_path")
    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus complaintStatus;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
