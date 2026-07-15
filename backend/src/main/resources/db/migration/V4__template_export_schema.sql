CREATE TABLE template (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    layout_descriptor LONGTEXT NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE export (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    tailored_resume_id  BIGINT NOT NULL,
    template_id         BIGINT NOT NULL,
    format              VARCHAR(10) NOT NULL,
    file_reference       VARCHAR(500) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_export_resume FOREIGN KEY (tailored_resume_id) REFERENCES tailored_resume (id) ON DELETE CASCADE,
    CONSTRAINT fk_export_template FOREIGN KEY (template_id) REFERENCES template (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO template (name, layout_descriptor) VALUES
    ('Modern', JSON_OBJECT('style', 'modern', 'accentColor', '#2563eb', 'fontFamily', 'sans-serif', 'sectionOrder', JSON_ARRAY('summary', 'experience', 'education', 'skills'))),
    ('Classic', JSON_OBJECT('style', 'classic', 'accentColor', '#1f2937', 'fontFamily', 'serif', 'sectionOrder', JSON_ARRAY('summary', 'experience', 'education', 'skills'))),
    ('Technical', JSON_OBJECT('style', 'technical', 'accentColor', '#0f766e', 'fontFamily', 'monospace', 'sectionOrder', JSON_ARRAY('summary', 'skills', 'experience', 'education')));
