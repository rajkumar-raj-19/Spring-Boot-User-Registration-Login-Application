package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // --- HELPER METHODS ---
    
    private String getEmailFromPrincipal(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return (String) token.getPrincipal().getAttributes().get("email");
        }
        return principal.getName();
    }

    // --- LOGIN & REGISTRATION ---

    @GetMapping("/login")
    public String login() { 
        return "login"; 
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        user.setProvider("LOCAL"); 
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setPhotoUrl("https://ui-avatars.com/api/?name=" + user.getFullName().replace(" ", "+") + "&background=random");
        userRepository.save(user);

        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            System.out.println("Email failed to send: " + e.getMessage());
        }

        return "redirect:/login?success";
    }

    // --- FORGOT PASSWORD ---

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam("email") String email,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       Model model) {
        
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            model.addAttribute("error", "Email address not found.");
            return "forgot-password";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            return "forgot-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return "redirect:/login?resetSuccess";
    }

    // --- DASHBOARD ---

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        String email = getEmailFromPrincipal(principal);
        String googlePhoto = null;

        if (principal instanceof OAuth2AuthenticationToken token) {
            googlePhoto = (String) token.getPrincipal().getAttributes().get("picture");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            model.addAttribute("name", user.getFullName());
            model.addAttribute("email", user.getEmail());
            model.addAttribute("provider", user.getProvider());
            model.addAttribute("photo", (googlePhoto != null) ? googlePhoto : user.getPhotoUrl());
        }

        return "dashboard";
    }

    // --- PROFILE MANAGEMENT ---

    @GetMapping("/edit-profile")
    public String showEditProfile(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        String email = getEmailFromPrincipal(principal);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            model.addAttribute("user", user);
        }

        return "edit-profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@RequestParam("fullName") String fullName,
                                @RequestParam(value = "newPassword", required = false) String newPassword,
                                @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                Principal principal) {
        if (principal == null) return "redirect:/login";

        String email = getEmailFromPrincipal(principal);
        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser != null) {
            existingUser.setFullName(fullName);

            if (newPassword != null && !newPassword.isEmpty()) {
                if (newPassword.equals(confirmPassword)) {
                    existingUser.setPassword(passwordEncoder.encode(newPassword));
                } else {
                    return "redirect:/edit-profile?error=mismatch";
                }
            }
            userRepository.save(existingUser);
        }

        return "redirect:/dashboard?updated";
    }

    // --- NEW: DELETE ACCOUNT API ---

    @PostMapping("/api/user/delete")
    @ResponseBody
    public ResponseEntity<?> deleteUserAccount(@RequestBody Map<String, String> payload, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        String email = getEmailFromPrincipal(principal);
        String providedPassword = payload.get("password");
        
        // Use .orElse(null) to match your current coding style
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        // Security Check: If it's a LOCAL user, verify the password
        if ("LOCAL".equalsIgnoreCase(user.getProvider())) {
            if (providedPassword == null || !passwordEncoder.matches(providedPassword, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
            }
        }

        // Delete from database
        userRepository.delete(user);

        // Clear session and logout
        SecurityContextHolder.clearContext();
        
        return ResponseEntity.ok("Account deleted successfully");
    }
}