package com.complaint.complaintbot.service;

import com.complaint.complaintbot.dto.ComplaintRequestDto;
import com.complaint.complaintbot.dto.ComplaintResponseDto;
import com.complaint.complaintbot.entity.Complaint;
import com.complaint.complaintbot.entity.ComplaintStatus;
import com.complaint.complaintbot.exception.ResourceNotFoundException;
import com.complaint.complaintbot.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png");

    @Override
    public ComplaintResponseDto createComplaint(ComplaintRequestDto request, MultipartFile image) {
        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            if (!ALLOWED_IMAGE_TYPES.contains(image.getContentType())) {
                throw new IllegalArgumentException("Invalid file type. Only JPG, JPEG, and PNG are allowed.");
            }
            try {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                String fileName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(image.getInputStream(), filePath);
                imagePath = filePath.toString();
            } catch (IOException e) {
                throw new RuntimeException("Could not store image file: " + e.getMessage());
            }
        }

        Complaint complaint = Complaint.builder()
                .userName(request.getUserName())
                .location(request.getLocation())
                .issueType(request.getIssueType())
                .issueDescription(request.getIssueDescription())
                .imagePath(imagePath)
                .complaintStatus(ComplaintStatus.IN_PROCESS)
                .build();

        Complaint savedComplaint = complaintRepository.save(complaint);
        return mapToResponse(savedComplaint);
    }

    @Override
    public List<ComplaintResponseDto> getAllComplaints() {
        return complaintRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ComplaintResponseDto getComplaintById(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));
        return mapToResponse(complaint);
    }

    @Override
    public ComplaintResponseDto updateComplaintStatus(Long id, ComplaintStatus status) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + id));
        complaint.setComplaintStatus(status);
        Complaint updatedComplaint = complaintRepository.save(complaint);
        return mapToResponse(updatedComplaint);
    }

    private ComplaintResponseDto mapToResponse(Complaint complaint) {
        return ComplaintResponseDto.builder()
                .id(complaint.getId())
                .userName(complaint.getUserName())
                .location(complaint.getLocation())
                .issueType(complaint.getIssueType())
                .issueDescription(complaint.getIssueDescription())
                .imagePath(complaint.getImagePath())
                .complaintStatus(complaint.getComplaintStatus())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }
}
