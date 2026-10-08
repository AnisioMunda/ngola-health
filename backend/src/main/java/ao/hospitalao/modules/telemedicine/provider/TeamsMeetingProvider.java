package ao.hospitalao.modules.telemedicine.provider;

import java.time.OffsetDateTime;

public interface TeamsMeetingProvider {

  boolean isConfigured();

  TeamsMeeting createMeeting(OffsetDateTime scheduledAt, int durationMinutes, String organizerId);

  void deleteMeeting(String meetingId, String organizerId);

  record TeamsMeeting(String id, String joinUrl) {}
}
