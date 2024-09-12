package com.sunmeat.contactlist.service;

import com.sunmeat.contactlist.model.Profile;
import com.sunmeat.contactlist.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/*
 сервис используется в контексте JPA и инкапсулирует логику доступа
 к данным, предоставляет методы для выполнения операций над профилями.
 это помогает разделить бизнес-логику и логику работы с базой данных,
 улучшая поддерживаемость и расширяемость кода
 */
@Service
public class ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    public List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    public Optional<Profile> getProfileById(Long id) {
        return profileRepository.findById(id);
    }

    public Profile saveProfile(Profile profile) {
        return profileRepository.save(profile);
    }

    public void deleteProfile(Long id) {
        profileRepository.deleteById(id);
    }
}