-- Replace the old achievement/radar form sections with unrestricted intro text.
ALTER TABLE experience
    ADD COLUMN company_intro MEDIUMTEXT NULL AFTER end_date;

-- TiDB does not allow the second ADD clause in the same ALTER to reference
-- a column introduced by the first clause. Keep this as a separate DDL job.
ALTER TABLE experience
    ADD COLUMN project_intro MEDIUMTEXT NULL AFTER company_intro;
