package com.sunmeat.contactlist.repository;

import com.sunmeat.contactlist.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
}