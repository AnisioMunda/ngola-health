package ao.hospitalao.modules.notifications.event;

import java.util.UUID;

public record LabResultsAvailableEvent(UUID hospitalId, UUID labRequestId, UUID recipientUserId) {}
