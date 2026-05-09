package com.complaint.complaintbot.controller;

import com.complaint.complaintbot.dto.ComplaintForm;
import com.complaint.complaintbot.entity.Complaint;
import com.complaint.complaintbot.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @GetMapping({"/", "/chat"})
    public String showChatPage(@RequestParam(required = false) Long id, Model model) {
        if (!model.containsAttribute("complaintForm")) {
            model.addAttribute("complaintForm", new ComplaintForm());
        }
        
        List<Complaint> complaints = complaintService.getAllComplaints();
        model.addAttribute("complaints", complaints);
        
        if (id != null) {
            Complaint selected = complaintService.getComplaintById(id);
            if (selected != null) {
                model.addAttribute("selectedComplaint", selected);
            }
        }
        
        return "chat-dashboard";
    }

    @PostMapping("/chat/submit")
    public String submitComplaint(@Valid @ModelAttribute("complaintForm") ComplaintForm form,
                                  BindingResult result,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.complaintForm", result);
            redirectAttributes.addFlashAttribute("complaintForm", form);
            redirectAttributes.addFlashAttribute("errorMessage", "Please fix the errors below.");
            return "redirect:/chat";
        }

        try {
            complaintService.saveComplaint(form);
            redirectAttributes.addFlashAttribute("successMessage", "Complaint registered successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to register complaint: " + e.getMessage());
            redirectAttributes.addFlashAttribute("complaintForm", form);
        }

        return "redirect:/chat";
    }
}
