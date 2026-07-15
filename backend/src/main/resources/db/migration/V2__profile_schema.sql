CREATE TABLE master_profile (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_account_id BIGINT NOT NULL,
    full_name      VARCHAR(255),
    email          VARCHAR(255),
    phone          VARCHAR(50),
    location       VARCHAR(255),
    links          VARCHAR(1000),
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_master_profile_user UNIQUE (user_account_id),
    CONSTRAINT fk_master_profile_user FOREIGN KEY (user_account_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE education_entry (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    master_profile_id  BIGINT NOT NULL,
    institution        VARCHAR(255) NOT NULL,
    credential         VARCHAR(255),
    field_of_study     VARCHAR(255),
    start_date         DATE,
    end_date           DATE,
    description        TEXT,
    display_order      INT NOT NULL DEFAULT 0,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_education_entry_profile FOREIGN KEY (master_profile_id) REFERENCES master_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE work_experience_entry (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    master_profile_id  BIGINT NOT NULL,
    company            VARCHAR(255) NOT NULL,
    title              VARCHAR(255) NOT NULL,
    start_date         DATE NOT NULL,
    end_date           DATE NULL,
    description        TEXT NOT NULL,
    display_order      INT NOT NULL DEFAULT 0,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_work_experience_entry_profile FOREIGN KEY (master_profile_id) REFERENCES master_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE skill (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    master_profile_id  BIGINT NOT NULL,
    name               VARCHAR(255) NOT NULL,
    category           VARCHAR(100),
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_skill_profile FOREIGN KEY (master_profile_id) REFERENCES master_profile (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
