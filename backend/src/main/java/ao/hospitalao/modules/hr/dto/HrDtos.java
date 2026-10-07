package ao.hospitalao.modules.hr.dto;

import ao.hospitalao.modules.hr.entity.AttendanceRecord.AttendanceStatus;
import ao.hospitalao.modules.hr.entity.LeaveRequest.LeaveStatus;
import ao.hospitalao.modules.hr.entity.LeaveRequest.LeaveType;
import ao.hospitalao.modules.hr.entity.Shift.ShiftStatus;
import ao.hospitalao.modules.hr.entity.Shift.ShiftType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class HrDtos {

    // ============================================================
    // Shift DTOs
    // ============================================================

    @Data @Builder
    public static class ShiftResponse {
        private UUID        id;
        private UUID        userId;
        private String      userFullName;
        private String      userRole;
        private ShiftType   shiftType;
        private String      shiftTypeLabel;
        private LocalDate   shiftDate;
        private LocalTime   startTime;
        private LocalTime   endTime;
        private double      durationHours;
        private String      department;
        private UUID        wardId;
        private String      wardName;
        private ShiftStatus status;
        private String      statusLabel;
        private String      notes;
        private OffsetDateTime createdAt;
    }

    @Data
    public static class CreateShiftRequest {
        private UUID      userId;
        private ShiftType shiftType;
        private LocalDate shiftDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String    department;
        private UUID      wardId;
        private String    notes;
    }

    @Data
    public static class UpdateShiftStatusRequest {
        private ShiftStatus status;
        private String      notes;
    }

    // ============================================================
    // Leave Request DTOs
    // ============================================================

    @Data @Builder
    public static class LeaveRequestResponse {
        private UUID        id;
        private UUID        userId;
        private String      userFullName;
        private LeaveType   leaveType;
        private String      leaveTypeLabel;
        private LocalDate   startDate;
        private LocalDate   endDate;
        private int         totalDays;
        private String      reason;
        private LeaveStatus status;
        private String      statusLabel;
        private String      approvedByName;
        private OffsetDateTime approvedAt;
        private String      rejectionReason;
        private OffsetDateTime createdAt;
    }

    @Data
    public static class CreateLeaveRequest {
        private LeaveType leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String    reason;
    }

    @Data
    public static class ApproveLeaveRequest {
        private boolean approved;
        private String  rejectionReason;
    }

    // ============================================================
    // Attendance DTOs
    // ============================================================

    @Data @Builder
    public static class AttendanceResponse {
        private UUID             id;
        private UUID             userId;
        private String           userFullName;
        private LocalDate        workDate;
        private OffsetDateTime   checkIn;
        private OffsetDateTime   checkOut;
        private Integer          minutesWorked;
        private String           hoursWorked;
        private Integer          overtimeMinutes;
        private AttendanceStatus status;
        private String           statusLabel;
        private String           notes;
        private OffsetDateTime   createdAt;
    }

    @Data
    public static class CheckInRequest {
        private UUID   shiftId;
        private String notes;
    }

    @Data
    public static class CheckOutRequest {
        private String notes;
    }

    // ============================================================
    // Stats / Dashboard RH
    // ============================================================

    @Data @Builder
    public static class HrStatsDto {
        private long totalStaff;
        private long shiftsToday;
        private long pendingLeaves;
        private long presentToday;
        private long absentToday;
        private long onLeaveToday;
        private List<ShiftResponse>        todayShifts;
        private List<LeaveRequestResponse> pendingLeaveRequests;
    }

    // ============================================================
    // Escala semanal (para o calendário de turnos)
    // ============================================================

    @Data @Builder
    public static class WeeklyScheduleResponse {
        private LocalDate             weekStart;
        private LocalDate             weekEnd;
        private List<DaySchedule>     days;
    }

    @Data @Builder
    public static class DaySchedule {
        private LocalDate             date;
        private String                dayLabel;
        private List<ShiftResponse>   shifts;
        private int                   totalShifts;
        private int                   confirmedShifts;
    }
}