--liquibase formatted sql

-- changeset hospitalao:026-01-teams-meeting-details
ALTER TABLE telemedicine_sessions
    ADD COLUMN provider_meeting_id VARCHAR(255),
    ADD COLUMN provider_organizer_id UUID,
    ADD COLUMN room_url TEXT;

CREATE UNIQUE INDEX uq_telemedicine_provider_meeting
    ON telemedicine_sessions(provider_meeting_id)
    WHERE provider_meeting_id IS NOT NULL;

--rollback DROP INDEX uq_telemedicine_provider_meeting;
--rollback ALTER TABLE telemedicine_sessions DROP COLUMN provider_meeting_id, DROP COLUMN provider_organizer_id, DROP COLUMN room_url;
