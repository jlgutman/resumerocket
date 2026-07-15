package com.resumerocket.profile;

import com.resumerocket.auth.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
public class ProfileController {

  private final ProfileService profileService;
  private final CurrentUserProvider currentUserProvider;

  public ProfileController(ProfileService profileService, CurrentUserProvider currentUserProvider) {
    this.profileService = profileService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  public ProfileResponse getProfile() {
    return profileService.getProfileView(currentUserProvider.require().id());
  }

  @PutMapping
  public ProfileResponse updateProfile(@Valid @RequestBody ContactInfoRequest request) {
    return profileService.updateContactInfo(currentUserProvider.require().id(), request);
  }

  @PostMapping("/education")
  public ResponseEntity<EducationEntryResponse> addEducation(
      @Valid @RequestBody EducationEntryRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(profileService.addEducation(currentUserProvider.require().id(), request));
  }

  @PutMapping("/education/{id}")
  public EducationEntryResponse updateEducation(
      @PathVariable Long id, @Valid @RequestBody EducationEntryRequest request) {
    return profileService.updateEducation(currentUserProvider.require().id(), id, request);
  }

  @DeleteMapping("/education/{id}")
  public ResponseEntity<Void> deleteEducation(@PathVariable Long id) {
    profileService.deleteEducation(currentUserProvider.require().id(), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/work-experience")
  public ResponseEntity<WorkExperienceEntryResponse> addWorkExperience(
      @Valid @RequestBody WorkExperienceEntryRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(profileService.addWorkExperience(currentUserProvider.require().id(), request));
  }

  @PutMapping("/work-experience/{id}")
  public WorkExperienceEntryResponse updateWorkExperience(
      @PathVariable Long id, @Valid @RequestBody WorkExperienceEntryRequest request) {
    return profileService.updateWorkExperience(currentUserProvider.require().id(), id, request);
  }

  @DeleteMapping("/work-experience/{id}")
  public ResponseEntity<Void> deleteWorkExperience(@PathVariable Long id) {
    profileService.deleteWorkExperience(currentUserProvider.require().id(), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/skills")
  public ResponseEntity<SkillResponse> addSkill(@Valid @RequestBody SkillRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(profileService.addSkill(currentUserProvider.require().id(), request));
  }

  @DeleteMapping("/skills/{id}")
  public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
    profileService.deleteSkill(currentUserProvider.require().id(), id);
    return ResponseEntity.noContent().build();
  }
}
