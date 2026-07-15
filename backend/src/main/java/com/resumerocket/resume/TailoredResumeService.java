package com.resumerocket.resume;

import com.resumerocket.common.ApiException;
import com.resumerocket.tailoring.TailoringService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TailoredResumeService {

  private final TailoredResumeRepository tailoredResumeRepository;
  private final TailoringService tailoringService;
  private final ResumeDiffService resumeDiffService;

  public TailoredResumeService(
      TailoredResumeRepository tailoredResumeRepository,
      TailoringService tailoringService,
      ResumeDiffService resumeDiffService) {
    this.tailoredResumeRepository = tailoredResumeRepository;
    this.tailoringService = tailoringService;
    this.resumeDiffService = resumeDiffService;
  }

  public TailoredResumeResponse get(Long userAccountId, Long id) {
    return tailoringService.toResponse(tailoringService.requireOwned(userAccountId, id));
  }

  /** Application history / version listing, optionally filtered by company (FR-016). */
  public List<TailoredResumeSummary> list(Long userAccountId, String company) {
    List<TailoredResume> resumes =
        StringUtils.hasText(company)
            ? tailoredResumeRepository.findByUserAccountIdAndCompanyContainingIgnoreCaseOrderByCreatedAtDesc(
                userAccountId, company)
            : tailoredResumeRepository.findByUserAccountIdOrderByCreatedAtDesc(userAccountId);
    return resumes.stream().map(TailoredResumeSummary::from).toList();
  }

  @Transactional
  public TailoredResumeResponse update(Long userAccountId, Long id, UpdateTailoredResumeRequest request) {
    TailoredResume resume = tailoringService.requireOwned(userAccountId, id);
    if (request.name() != null && request.name().isBlank()) {
      throw ApiException.badRequest("name must not be blank");
    }
    if (request.name() != null) {
      resume.setName(request.name());
    }
    if (request.company() != null) {
      resume.setCompany(request.company());
    }
    if (request.jobTitle() != null) {
      resume.setJobTitle(request.jobTitle());
    }
    if (request.status() != null) {
      resume.setStatus(request.status());
    }
    tailoredResumeRepository.save(resume);
    return tailoringService.toResponse(resume);
  }

  public CompareResponse compare(Long userAccountId, Long leftId, Long rightId) {
    TailoredResume left = tailoringService.requireOwned(userAccountId, leftId);
    TailoredResume right = tailoringService.requireOwned(userAccountId, rightId);
    List<SectionDiff> diff =
        resumeDiffService.compare(
            tailoringService.readResumeContent(left), tailoringService.readResumeContent(right));
    return new CompareResponse(tailoringService.toResponse(left), tailoringService.toResponse(right), diff);
  }
}
