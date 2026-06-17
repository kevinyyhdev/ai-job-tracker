CREATE TABLE job_applications (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users (id),
    company_name    VARCHAR(255) NOT NULL,
    job_title       VARCHAR(255) NOT NULL,
    job_link        TEXT,
    location        VARCHAR(255),
    employment_type VARCHAR(50),
    status          VARCHAR(50)  NOT NULL DEFAULT 'SAVED',
    source          VARCHAR(100),
    notes           TEXT,
    applied_at      TIMESTAMP WITH TIME ZONE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_job_applications_user_status  ON job_applications (user_id, status);
CREATE INDEX idx_job_applications_user_created ON job_applications (user_id, created_at);
