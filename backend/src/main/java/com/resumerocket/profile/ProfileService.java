package com.resumerocket.profile;

import com.resumerocket.common.ApiException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

  private final MasterProfileRepository masterProfileRepository;
  private final EducationEntryRepository educationEntryRepository;
  private final WorkExperienceEntryRepository workExperienceEntryRepository;
  private final SkillRepository skillRepository;

  public ProfileService(
      MasterProfileRepository masterProfileRepository,
      EducationEntryRepository educationEntryRepository,
      WorkExperienceEntryRepository workExperienceEntryRepository,
      SkillRepository skillRepository) {
    this.masterProfileRepository = masterProfileRepository;
    this.educationEntryRepository = educationEntryRepository;
    this.workExperienceEntryRepository = workExperienceEntryRepository;
    this.skillRepository = skillRepository;
  }

  @Transactional
  public MasterProfile getOrCreateProfile(Long userAccountId) {
    return masterProfileRepository
        .findByUserAccountId(userAccountId)
        .orElseGet(() -> masterProfileRepository.save(new MasterProfile(userAccountId)));
  }

  public ProfileResponse getProfileView(Long userAccountId) {
    return toResponse(getOrCreateProfile(userAccountId));
  }

  @Transactional
  public ProfileResponse updateContactInfo(Long userAccountId, ContactInfoRequest request) {
    MasterProfile profile = getOrCreateProfile(userAccountId);
    profile.setFullName(request.fullName());
    profile.setEmail(request.email());
    profile.setPhone(request.phone());
    profile.setLocation(request.location());
    profile.setLinks(request.links());
    return toResponse(profile);
  }

  @Transactional
  public EducationEntryResponse addEducation(Long userAccountId, EducationEntryRequest request) {
    MasterProfile profile = getOrCreateProfile(userAccountId);
    EducationEntry entry = new EducationEntry(profile);
    applyEducation(entry, request);
    return EducationEntryResponse.from(educationEntryRepository.save(entry));
  }

  @Transactional
  public EducationEntryResponse updateEducation(
      Long userAccountId, Long entryId, EducationEntryRequest request) {
    EducationEntry entry = requireOwnedEducationEntry(userAccountId, entryId);
    applyEducation(entry, request);
    return EducationEntryResponse.from(entry);
  }

  @Transactional
  public void deleteEducation(Long userAccountId, Long entryId) {
    EducationEntry entry = requireOwnedEducationEntry(userAccountId, entryId);
    educationEntryRepository.delete(entry);
  }

  @Transactional
  public WorkExperienceEntryResponse addWorkExperience(
      Long userAccountId, WorkExperienceEntryRequest request) {
    MasterProfile profile = getOrCreateProfile(userAccountId);
    WorkExperienceEntry entry = new WorkExperienceEntry(profile);
    applyWorkExperience(entry, request);
    return WorkExperienceEntryResponse.from(workExperienceEntryRepository.save(entry));
  }

  @Transactional
  public WorkExperienceEntryResponse updateWorkExperience(
      Long userAccountId, Long entryId, WorkExperienceEntryRequest request) {
    WorkExperienceEntry entry = requireOwnedWorkExperienceEntry(userAccountId, entryId);
    applyWorkExperience(entry, request);
    return WorkExperienceEntryResponse.from(entry);
  }

  @Transactional
  public void deleteWorkExperience(Long userAccountId, Long entryId) {
    WorkExperienceEntry entry = requireOwnedWorkExperienceEntry(userAccountId, entryId);
    workExperienceEntryRepository.delete(entry);
  }

  @Transactional
  public SkillResponse addSkill(Long userAccountId, SkillRequest request) {
    MasterProfile profile = getOrCreateProfile(userAccountId);
    Skill skill = new Skill(profile, request.name(), request.category());
    return SkillResponse.from(skillRepository.save(skill));
  }

  @Transactional
  public void deleteSkill(Long userAccountId, Long skillId) {
    Skill skill =
        skillRepository.findById(skillId).orElseThrow(() -> ApiException.notFound("Skill not found"));
    requireOwnership(userAccountId, skill.getMasterProfile());
    skillRepository.delete(skill);
  }

  private void applyEducation(EducationEntry entry, EducationEntryRequest request) {
    entry.setInstitution(request.institution());
    entry.setCredential(request.credential());
    entry.setFieldOfStudy(request.fieldOfStudy());
    entry.setStartDate(request.startDate());
    entry.setEndDate(request.endDate());
    entry.setDescription(request.description());
    entry.setDisplayOrder(request.displayOrder());
  }

  private void applyWorkExperience(WorkExperienceEntry entry, WorkExperienceEntryRequest request) {
    entry.setCompany(request.company());
    entry.setTitle(request.title());
    entry.setStartDate(request.startDate());
    entry.setEndDate(request.endDate());
    entry.setDescription(request.description());
    entry.setDisplayOrder(request.displayOrder());
  }

  private EducationEntry requireOwnedEducationEntry(Long userAccountId, Long entryId) {
    EducationEntry entry =
        educationEntryRepository
            .findById(entryId)
            .orElseThrow(() -> ApiException.notFound("Education entry not found"));
    requireOwnership(userAccountId, entry.getMasterProfile());
    return entry;
  }

  private WorkExperienceEntry requireOwnedWorkExperienceEntry(Long userAccountId, Long entryId) {
    WorkExperienceEntry entry =
        workExperienceEntryRepository
            .findById(entryId)
            .orElseThrow(() -> ApiException.notFound("Work experience entry not found"));
    requireOwnership(userAccountId, entry.getMasterProfile());
    return entry;
  }

  private void requireOwnership(Long userAccountId, MasterProfile profile) {
    if (!profile.getUserAccountId().equals(userAccountId)) {
      throw ApiException.forbidden("This resource does not belong to the current user");
    }
  }

  private ProfileResponse toResponse(MasterProfile profile) {
    List<EducationEntryResponse> education =
        educationEntryRepository.findByMasterProfileIdOrderByDisplayOrderAsc(profile.getId())
            .stream()
            .map(EducationEntryResponse::from)
            .toList();
    List<WorkExperienceEntryResponse> workExperience =
        workExperienceEntryRepository
            .findByMasterProfileIdOrderByDisplayOrderAsc(profile.getId())
            .stream()
            .map(WorkExperienceEntryResponse::from)
            .toList();
    List<SkillResponse> skills =
        skillRepository.findByMasterProfileId(profile.getId()).stream()
            .map(SkillResponse::from)
            .sorted(Comparator.comparing(SkillResponse::name))
            .toList();
    return new ProfileResponse(
        profile.getId(),
        profile.getFullName(),
        profile.getEmail(),
        profile.getPhone(),
        profile.getLocation(),
        profile.getLinks(),
        education,
        workExperience,
        skills);
  }
}
