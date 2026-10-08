package ao.hospitalao.modules.notifications.application.event;

import java.util.UUID;

public record LabResultsAvailableEvent(UUID hospitalId, UUID labRequestId, UUID recipientUserId) {}
