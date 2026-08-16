CREATE TABLE job_applications (
    id uuid PRIMARY KEY,
    company_name varchar(200) NOT NULL,
    position_title varchar(200) NOT NULL,
    job_posting_url varchar(2048),
    source varchar(100),
    location varchar(200),
    applied_on date,
    notes varchar(4000),
    next_action_description varchar(500),
    next_action_due_at timestamp(6) with time zone,
    status varchar(20) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT ck_job_applications_status CHECK (
        status IN ('SAVED', 'APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN')
    ),
    CONSTRAINT ck_job_applications_next_action_pair CHECK (
        (next_action_description IS NULL AND next_action_due_at IS NULL)
        OR (next_action_description IS NOT NULL AND next_action_due_at IS NOT NULL)
    )
);

CREATE TABLE status_changes (
    id uuid PRIMARY KEY,
    job_application_id uuid NOT NULL,
    previous_status varchar(20),
    new_status varchar(20) NOT NULL,
    changed_at timestamp(6) with time zone NOT NULL,
    note varchar(500),
    CONSTRAINT ck_status_changes_previous_status CHECK (
        previous_status IS NULL
        OR previous_status IN ('SAVED', 'APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN')
    ),
    CONSTRAINT ck_status_changes_new_status CHECK (
        new_status IN ('SAVED', 'APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN')
    ),
    CONSTRAINT ck_status_changes_different_statuses CHECK (
        previous_status IS NULL OR previous_status <> new_status
    ),
    CONSTRAINT fk_status_changes_job_application FOREIGN KEY (job_application_id)
        REFERENCES job_applications (id) ON DELETE CASCADE
);

CREATE INDEX ix_job_applications_status_updated_at
    ON job_applications (status, updated_at DESC);

CREATE INDEX ix_job_applications_next_action_due_at
    ON job_applications (next_action_due_at);

CREATE INDEX ix_status_changes_job_application_id_changed_at
    ON status_changes (job_application_id, changed_at);
