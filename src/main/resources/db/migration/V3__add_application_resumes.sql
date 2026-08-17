CREATE TABLE application_resumes (
    job_application_id uuid PRIMARY KEY,
    file_name varchar(255) NOT NULL,
    content_type varchar(100) NOT NULL,
    content bytea NOT NULL,
    uploaded_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT ck_application_resumes_content_length CHECK (
        octet_length(content) BETWEEN 1 AND 5242880
    ),
    CONSTRAINT fk_application_resumes_job_application FOREIGN KEY (job_application_id)
        REFERENCES job_applications (id) ON DELETE CASCADE
);
