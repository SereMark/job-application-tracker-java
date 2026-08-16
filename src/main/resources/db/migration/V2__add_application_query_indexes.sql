CREATE INDEX ix_job_applications_updated_at
    ON job_applications (updated_at DESC);

CREATE INDEX ix_job_applications_source_updated_at
    ON job_applications (source, updated_at DESC);

CREATE INDEX ix_job_applications_applied_on
    ON job_applications (applied_on);
