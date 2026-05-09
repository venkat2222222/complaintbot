package com.complaint.complaintbot.controller;

import com.complaint.complaintbot.dto.ApiResponse;
import com.complaint.complaintbot.dto.ComplaintRequestDto;
import com.complaint.complaintbot.dto.ComplaintResponseDto;
import com.complaint.complaintbot.entity.ComplaintStatus;
import com.complaint.complaintbot.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintRestController {

    private final ComplaintService complaintService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ComplaintResponseDto>> createComplaint(
            @Valid @ModelAttribute ComplaintRequestDto request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        ComplaintResponseDto response = complaintService.createComplaint(request, image);
        ApiResponse<ComplaintResponseDto> apiResponse = new ApiResponse<>(true, "Complaint created successfully", response);
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ComplaintResponseDto>>> getAllComplaints() {
        List<ComplaintResponseDto> complaints = complaintService.getAllComplaints();
        ApiResponse<List<ComplaintResponseDto>> response = new ApiResponse<>(true, "Fetched all complaints", complaints);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComplaintResponseDto>> getComplaintById(@PathVariable Long id) {
        ComplaintResponseDto complaint = complaintService.getComplaintById(id);
        ApiResponse<ComplaintResponseDto> response = new ApiResponse<>(true, "Fetched complaint details", complaint);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ComplaintResponseDto>> updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam ComplaintStatus status) {
        ComplaintResponseDto updatedComplaint = complaintService.updateComplaintStatus(id, status);
        ApiResponse<ComplaintResponseDto> response = new ApiResponse<>(true, "Complaint status updated", updatedComplaint);
        return ResponseEntity.ok(response);
    }
}
