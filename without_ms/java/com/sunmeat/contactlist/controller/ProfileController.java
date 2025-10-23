package com.sunmeat.contactlist.controller;

import com.sunmeat.contactlist.model.Profile;
import com.sunmeat.contactlist.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.*;
import java.nio.file.*;
import java.util.Optional;

@Controller
@RequestMapping("/profiles")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    // шлях для збереження завантажених файлів
    private static final String UPLOAD_DIR = "uploads/";

    @GetMapping
    public String listProfiles(Model model) { // список профілів
        model.addAttribute("profiles", profileService.getAllProfiles());
        return "profile-list";
    }

    @GetMapping("/add") // форма додавання профілю
    public String addProfileForm(Model model) {
        model.addAttribute("profile", new Profile());
        return "profile-form";
    }

    @PostMapping("/add") // обробка додавання профілю
    public String addProfile(
            @ModelAttribute Profile profile,
            @RequestParam("avatar") MultipartFile avatar,
            RedirectAttributes redirectAttributes) throws IOException {

        // обробка файлу аватара
        if (!avatar.isEmpty()) {
            // створення директорії, якщо її немає
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdir();
            }

            // визначення шляху до файлу і збереження
            String filename = System.currentTimeMillis() + "_" + StringUtils.cleanPath(avatar.getOriginalFilename());
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Files.write(filePath, avatar.getBytes());

            // збереження URL аватара
            profile.setAvatarUrl("/uploads/" + filename);
        }

        profileService.saveProfile(profile);
        redirectAttributes.addFlashAttribute("message", "Профіль успішно додано!");
        return "redirect:/profiles";
    }

    @GetMapping("/edit/{id}") // форма редагування профілю
    public String editProfileForm(@PathVariable("id") Long id, Model model) {
        Optional<Profile> profile = profileService.getProfileById(id);
        if (profile.isPresent()) {
            model.addAttribute("profile", profile.get());
            return "profile-form";
        } else {
            return "redirect:/profiles";
        }
    }

    @PostMapping("/edit/{id}") // обробка редагування профілю
    public String editProfile(
            @PathVariable("id") Long id,
            @ModelAttribute Profile profile,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            RedirectAttributes redirectAttributes) throws IOException {

        profile.setId(id);

        // обробка файлу аватара
        if (avatar != null && !avatar.isEmpty()) {
            // створення директорії, якщо її немає
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdir();
            }

            // визначення шляху до файлу і збереження
            String filename = System.currentTimeMillis() + "_" + StringUtils.cleanPath(avatar.getOriginalFilename());
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Files.write(filePath, avatar.getBytes());

            profile.setAvatarUrl("/uploads/" + filename);
        }

        profileService.saveProfile(profile);
        redirectAttributes.addFlashAttribute("message", "Профіль успішно оновлено!");
        return "redirect:/profiles";
    }

    @GetMapping("/delete/{id}") // видалення профілю
    public String deleteProfile(@PathVariable("id") Long id) {
        Optional<Profile> profileOptional = profileService.getProfileById(id);
        if (profileOptional.isPresent()) {
            Profile profile = profileOptional.get();
            // видалення файлу аватара
            String avatarUrl = profile.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String filename = avatarUrl.substring(avatarUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(UPLOAD_DIR + filename);
                try {
                    Files.delete(filePath);
                } catch (IOException e) {
                    e.printStackTrace(); // обробка помилки видалення файлу
                }
            }
            // видалення профілю з бази даних
            profileService.deleteProfile(id);
        }
        return "redirect:/profiles";
    }

    @GetMapping("/uploads/{filename:.+}") // обслуговування завантажених файлів
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Path file = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
        Resource resource;
        try {
            resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                if (contentType == null) {
                    contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/feedback/{id}") // форма відправки відгуку
    public String showFeedbackForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("profileId", id);
        return "send-feedback";
    }
    
    @PostMapping("/feedback") // обробка відправки відгуку
    public String sendFeedback(
            @RequestParam(name = "profileId") Long profileId,
            @RequestParam(name = "message") String message,
            RedirectAttributes redirectAttributes) {

        // екранування лапок та нових рядків у повідомленні
        String escapedMessage = message.replace("\"", "\\\"").replace("\n", "\\n");
        String feedbackMessage = String.format("Ваш отзыв о профиле ID %d был отправлен: \"%s\"", profileId, escapedMessage);
        
        // додання повідомлення у RedirectAttributes, щоб показати його після перенаправлення
        redirectAttributes.addFlashAttribute("feedbackMessage", feedbackMessage);

        // перенаправлення назад до списку профілів
        return "redirect:/profiles";
    }
}