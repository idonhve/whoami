-- Replace the old achievement/radar form sections with unrestricted intro text.
ALTER TABLE experience
    ADD COLUMN company_intro MEDIUMTEXT NULL AFTER end_date,
    ADD COLUMN project_intro MEDIUMTEXT NULL AFTER company_intro;
