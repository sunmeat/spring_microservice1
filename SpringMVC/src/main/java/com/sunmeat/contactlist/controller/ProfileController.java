package com.sunmeat.contactlist.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunmeat.contactlist.model.FeedbackMessage;
import com.sunmeat.contactlist.model.Profile;
import com.sunmeat.contactlist.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.amqp.core.AmqpTemplate; // RabbitMQ

@Controller
@RequestMapping("/profiles")
public class ProfileController {

	@Autowired
    private AmqpTemplate amqpTemplate; // RabbitMQ
	
    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private ObjectMapper objectMapper;

    // путь для хранения загруженных файлов
    private static final String UPLOAD_DIR = "uploads/";

    @GetMapping
    public String listProfiles(Model model) {
        model.addAttribute("profiles", profileService.getAllProfiles());
        return "profile-list";
    }

    @GetMapping("/add")
    public String addProfileForm(Model model) {
        model.addAttribute("profile", new Profile());
        return "profile-form";
    }

    @PostMapping("/add")
    public String addProfile(
            @ModelAttribute Profile profile,
            @RequestParam("avatar") MultipartFile avatar,
            RedirectAttributes redirectAttributes) throws IOException {

        // обработка файла аватара
        if (!avatar.isEmpty()) {
            // создание директории, если её нет
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdir();
            }

            // определение пути к файлу и сохранение
            String filename = System.currentTimeMillis() + "_" + StringUtils.cleanPath(avatar.getOriginalFilename());
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Files.write(filePath, avatar.getBytes());

            // сохранение URL аватара
            profile.setAvatarUrl("/uploads/" + filename);
        }

        profileService.saveProfile(profile);
        redirectAttributes.addFlashAttribute("message", "Profile added successfully!");
        return "redirect:/profiles";
    }

    @GetMapping("/edit/{id}")
    public String editProfileForm(@PathVariable("id") Long id, Model model) {
        Optional<Profile> profile = profileService.getProfileById(id);
        if (profile.isPresent()) {
            model.addAttribute("profile", profile.get());
            return "profile-form";
        } else {
            return "redirect:/profiles";
        }
    }

    @PostMapping("/edit/{id}")
    public String editProfile(
            @PathVariable("id") Long id,
            @ModelAttribute Profile profile,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            RedirectAttributes redirectAttributes) throws IOException {

        profile.setId(id);

        // обработка нового файла аватара
        if (avatar != null && !avatar.isEmpty()) {
            // создание директории, если её нет
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdir();
            }

            // определение пути к файлу и сохранение
            String filename = System.currentTimeMillis() + "_" + StringUtils.cleanPath(avatar.getOriginalFilename());
            Path filePath = Paths.get(UPLOAD_DIR + filename);
            Files.write(filePath, avatar.getBytes());

            // сохранение URL аватара
            profile.setAvatarUrl("/uploads/" + filename);
        }

        profileService.saveProfile(profile);
        redirectAttributes.addFlashAttribute("message", "Профиль успешно сохранён!");
        return "redirect:/profiles";
    }

    @GetMapping("/delete/{id}")
    public String deleteProfile(@PathVariable("id") Long id) {
        Optional<Profile> profileOptional = profileService.getProfileById(id);
        if (profileOptional.isPresent()) {
            Profile profile = profileOptional.get();
            // удаление аватара, если он есть
            String avatarUrl = profile.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                String filename = avatarUrl.substring(avatarUrl.lastIndexOf("/") + 1);
                Path filePath = Paths.get(UPLOAD_DIR + filename);
                try {
                    Files.delete(filePath);
                } catch (IOException e) {
                    e.printStackTrace(); // обработка ошибки удаления файла
                }
            }
            // удаление профиля из базы данных
            profileService.deleteProfile(id);
        }
        return "redirect:/profiles";
    }

    @GetMapping("/uploads/{filename:.+}")
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
    
    @GetMapping("/feedback/{id}")
    public String showFeedbackForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("profileId", id);
        return "send-feedback";
    }
    
    @PostMapping("/feedback")
    public String sendFeedback(
            @RequestParam(name = "id") Long id,
            @RequestParam(name = "message") String message,
            RedirectAttributes redirectAttributes) {

        // проверка полученных параметров
        System.out.println("Received feedback:");
        System.out.println("id: " + id);
        System.out.println("message: " + message);

        // извлечение профиля из базы данных по ID
        Optional<Profile> profileOptional = profileService.getProfileById(id);
        if (!profileOptional.isPresent()) {
            redirectAttributes.addFlashAttribute("feedbackMessage", "Профиль не найден!");
            return "redirect:/profiles";
        }

        Profile profile = profileOptional.get();

        // создание объекта отзыва с полными данными профиля
        FeedbackMessage feedbackMessage = new FeedbackMessage(
                profile.getId(),
                profile.getNickname(),
                profile.getAvatarUrl(),
                profile.getGender(),
                profile.getAge(),
                profile.getCity(),
                message // текст отзыва
        );

        // преобразование объекта в JSON
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(feedbackMessage);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            jsonString = "{}"; // fallback
        }

        System.out.println(jsonString);
        
        // отправка JSON-сообщения в RabbitMQ
        amqpTemplate.convertAndSend("feedbackExchange", "feedback.key", jsonString);

        // установка сообщения для отображения после перенаправления
        redirectAttributes.addFlashAttribute("feedbackMessage", "Отзыв успешно отправлен!");
        redirectAttributes.addFlashAttribute("feedbackJson", jsonString);
        
        return "redirect:/profiles";
    }
}