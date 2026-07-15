CREATE TABLE job_description (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_account_id        BIGINT NOT NULL,
    raw_text               LONGTEXT NOT NULL,
    extracted_requirements LONGTEXT,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_description_user FOREIGN KEY (user_account_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tailored_resume (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_account_id          BIGINT NOT NULL,
    job_description_id       BIGINT NULL,
    source_profile_snapshot  LONGTEXT NOT NULL,
    resume_content           LONGTEXT NOT NULL,
    unmatched_requirements   LONGTEXT,
    name                     VARCHAR(255) NOT NULL,
    company                  VARCHAR(255),
    job_title                VARCHAR(255),
    status                   VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    cloned_from_id           BIGINT NULL,
    regenerated_from_id      BIGINT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tailored_resume_user FOREIGN KEY (user_account_id) REFERENCES user_account (id),
    CONSTRAINT fk_tailored_resume_job_description FOREIGN KEY (job_description_id) REFERENCES job_description (id),
    CONSTRAINT fk_tailored_resume_cloned_from FOREIGN KEY (cloned_from_id) REFERENCES tailored_resume (id),
    CONSTRAINT fk_tailored_resume_regenerated_from FOREIGN KEY (regenerated_from_id) REFERENCES tailored_resume (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_tailored_resume_user ON tailored_resume (user_account_id);

CREATE TABLE ai_suggestion (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tailored_resume_id  BIGINT NOT NULL,
    target_section      VARCHAR(50) NOT NULL,
    suggestion_type     VARCHAR(50) NOT NULL,
    original_text       TEXT,
    suggested_text      TEXT NOT NULL,
    final_text          TEXT,
    review_state        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_suggestion_resume FOREIGN KEY (tailored_resume_id) REFERENCES tailored_resume (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
