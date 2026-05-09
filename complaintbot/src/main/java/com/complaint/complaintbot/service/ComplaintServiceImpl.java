package com.complaint.complaintbot.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import com.complaint.complaintbot.dto.ComplaintForm;
import com.complaint.complaintbot.entity.Complaint;
import com.complaint.complaintbot.entity.ComplaintStatus;
import com.complaint.complaintbot.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ChatLanguageModel chatLanguageModel;
    
    // Use an absolute path or relative to working directory. 
    // Here we use a relative directory named "uploads" in the project root.
    private final String UPLOAD_DIR = "uploads/";

    @Override
    public Complaint saveComplaint(ComplaintForm form) {
        Complaint complaint = new Complaint();
        complaint.setUserName(form.getUserName());
        complaint.setTitle(form.getTitle());
        complaint.setLocation(form.getLocation());
        complaint.setIssueType(form.getIssueType());
        
        String description = form.getIssueDescription();

        // Gemini AI Suggestion Logic
        try {
            String prompt = String.format(
                "Given a complaint in location '%s' about issue type '%s' with description '%s'. " +
                "What is the official website or authority where this should be reported? " +
                "Provide a very brief response with just the name of the authority and the URL.",
                form.getLocation(), form.getIssueType(), description
            );
            
            String aiSuggestion = chatLanguageModel.generate(prompt);
            description += "\n\n--- Official Site Suggestion ---\n" + aiSuggestion;
        } catch (Exception e) {
            description += "\n\n[Error generating AI suggestion: " + e.getMessage() + "]";
        }
        
        complaint.setIssueDescription(description);
        complaint.setComplaintStatus(ComplaintStatus.OPEN); // Default status

        if (form.getImage() != null && !form.getImage().isEmpty()) {
            String imagePath = saveImage(form.getImage());
            complaint.setImagePath(imagePath);
        }

        return complaintRepository.save(complaint);
    }

    @Override
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    @Override
    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(id).orElse(null);
    }

    private String saveImage(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = "";
            int i = originalFilename.lastIndexOf('.');
            if (i > 0) {
                extension = originalFilename.substring(i);
            }

            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(filename);
            
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Return relative path to be served via URL
            return "uploads/" + filename;
            
        } catch (IOException e) {
            throw new RuntimeException("Could not store image file", e);
        }
    }
}
