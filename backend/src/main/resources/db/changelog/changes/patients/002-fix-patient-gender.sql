--liquibase formatted sql

-- changeset hospitalao:005-01-fix-patients-gender-type splitStatements:false
ALTER TABLE patients
            ALTER COLUMN gender TYPE VARCHAR(10)
            USING gender::VARCHAR;

--rollback ALTER TABLE patients
                ALTER COLUMN gender TYPE gender_enum
                USING gender::gender_enum;
