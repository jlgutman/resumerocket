package com.resumerocket.resumeimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.resumerocket.auth.AuthResponse;
import com.resumerocket.resumeimport.dto.ContactInfoCandidate;
import com.resumerocket.resumeimport.dto.ResumeImportResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * End-to-end integration test for {@code POST /profile/resume-import} against a real Testcontainers
 * MySQL instance and Flyway-migrated schema (CLAUDE.md testing conventions). {@link
 * AiResumeExtractionService} is mocked so this never calls OpenAI and doesn't require
 * OPENAI_API_KEY. Uses a plain {@link RestTemplate} against the random server port —
 * {@code TestRestTemplate} was removed in Spring Boot 4.
 */
@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ResumeImportControllerIT {

  @Container
  static MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.4")
          .withDatabaseName("resumerocket_test")
          .withUsername("resumerocket")
          .withPassword("resumerocket");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
  }

  @LocalServerPort private int port;

  @MockitoBean private AiResumeExtractionService aiResumeExtractionService;

  private final RestTemplate restTemplate = lenientRestTemplate();

  @Test
  void extractsCandidateDataForAnAuthenticatedUpload() {
    String token = registerAndGetToken("resume-import-happy@example.com");
    when(aiResumeExtractionService.extract(anyString()))
        .thenReturn(
            new ResumeImportResult(
                null,
                new ContactInfoCandidate(
                    "Jane Doe", "jane@example.com", null, null, null, List.of()),
                List.of(),
                List.of(),
                List.of(),
                List.of()));

    ResponseEntity<ResumeImportResult> response =
        restTemplate.postForEntity(
            url("/profile/resume-import"),
            multipartRequest(token, "resume.pdf", MediaType.APPLICATION_PDF, "%PDF-1.4 ...".getBytes()),
            ResumeImportResult.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().sourceFileName()).isEqualTo("resume.pdf");
    assertThat(response.getBody().contactInfo().fullName()).isEqualTo("Jane Doe");
  }

  @Test
  void rejectsNonPdfUploads() {
    String token = registerAndGetToken("resume-import-badtype@example.com");

    ResponseEntity<Map> response =
        restTemplate.postForEntity(
            url("/profile/resume-import"),
            multipartRequest(token, "resume.txt", MediaType.TEXT_PLAIN, "not a pdf".getBytes()),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsUnauthenticatedRequests() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(
        "file", filePart("resume.pdf", "%PDF-1.4 ...".getBytes(), MediaType.APPLICATION_PDF));

    ResponseEntity<String> response =
        restTemplate.postForEntity(url("/profile/resume-import"), new HttpEntity<>(body, headers), String.class);

    assertThat(response.getStatusCode().is4xxClientError()).isTrue();
  }

  private String url(String path) {
    return "http://localhost:" + port + "/api/v1" + path;
  }

  private HttpEntity<MultiValueMap<String, Object>> multipartRequest(
      String token, String filename, MediaType contentType, byte[] content) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", filePart(filename, content, contentType));
    return new HttpEntity<>(body, headers);
  }

  /** Wraps the resource with its own part-level Content-Type header (server reads it via
   * {@code MultipartFile#getContentType()}). */
  private HttpEntity<ByteArrayResource> filePart(String filename, byte[] content, MediaType contentType) {
    HttpHeaders partHeaders = new HttpHeaders();
    partHeaders.setContentType(contentType);
    return new HttpEntity<>(new NamedByteArrayResource(filename, content), partHeaders);
  }

  private String registerAndGetToken(String email) {
    Map<String, String> registerBody =
        Map.of("email", email, "password", "SuperSecret123", "fullName", "Test User");
    ResponseEntity<AuthResponse> response =
        restTemplate.postForEntity(url("/auth/register"), registerBody, AuthResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody().token();
  }

  /** Never throws on 4xx/5xx, so tests can assert on the returned status like {@code
   * TestRestTemplate} used to. */
  private static RestTemplate lenientRestTemplate() {
    RestTemplate template = new RestTemplate();
    template.setErrorHandler(
        new DefaultResponseErrorHandler() {
          @Override
          public boolean hasError(ClientHttpResponse response) {
            return false;
          }
        });
    return template;
  }

  /** {@link ByteArrayResource} whose filename RestTemplate uses to build the multipart part. */
  private static class NamedByteArrayResource extends ByteArrayResource {
    private final String filename;

    NamedByteArrayResource(String filename, byte[] content) {
      super(content);
      this.filename = filename;
    }

    @Override
    public String getFilename() {
      return filename;
    }
  }
}
