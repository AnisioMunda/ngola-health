package ao.hospitalao.modules.hr.service;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.hr.dto.HrDtos.*;
import ao.hospitalao.modules.hr.entity.AttendanceRecord;
import ao.hospitalao.modules.hr.entity.AttendanceRecord.AttendanceStatus;
import ao.hospitalao.modules.hr.entity.LeaveRequest;
import ao.hospitalao.modules.hr.entity.LeaveRequest.LeaveStatus;
import ao.hospitalao.modules.hr.entity.Shift;
import ao.hospitalao.modules.hr.entity.Shift.ShiftStatus;
import ao.hospitalao.modules.hr.repository.AttendanceRepository;
import ao.hospitalao.modules.hr.repository.LeaveRequestRepository;
import ao.hospitalao.modules.hr.repository.ShiftRepository;
import ao.hospitalao.modules.inpatient.repository.WardRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HrService {

    private final ShiftRepository      shiftRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository       userRepository;
    private final HospitalRepository   hospitalRepository;
    private final WardRepository       wardRepository;

    // ------------------------------------------------
    // Dashboard RH
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public HrStatsDto getStats() {
        UUID hospitalId = TenantContext.getCurrentHospital();
        LocalDate today = LocalDate.now();

        long totalStaff   = userRepository.countByHospitalIdAndActiveTrue(hospitalId);
        long shiftsToday  = shiftRepository
            .findByHospitalAndPeriod(hospitalId, today, today).size();
        long pendingLeaves = leaveRequestRepository
            .findByHospitalAndStatus(hospitalId, LeaveStatus.PENDING).size();
        long presentToday  = attendanceRepository
            .countByStatus(hospitalId, today, today, AttendanceStatus.PRESENT);
        long absentToday   = attendanceRepository
            .countByStatus(hospitalId, today, today, AttendanceStatus.ABSENT);
        long onLeaveToday  = attendanceRepository
            .countByStatus(hospitalId, today, today, AttendanceStatus.ON_LEAVE);

        List<ShiftResponse> todayShifts = shiftRepository
            .findByHospitalAndPeriod(hospitalId, today, today)
            .stream().map(this::toShiftResponse).toList();

        List<LeaveRequestResponse> pendingLeaveRequests = leaveRequestRepository
            .findByHospitalAndStatus(hospitalId, LeaveStatus.PENDING)
            .stream().map(this::toLeaveResponse).toList();

        return HrStatsDto.builder()
            .totalStaff(totalStaff).shiftsToday(shiftsToday)
            .pendingLeaves(pendingLeaves).presentToday(presentToday)
            .absentToday(absentToday).onLeaveToday(onLeaveToday)
            .todayShifts(todayShifts)
            .pendingLeaveRequests(pendingLeaveRequests)
            .build();
    }

    // ------------------------------------------------
    // Turnos
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public WeeklyScheduleResponse getWeeklySchedule(LocalDate weekStart) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Shift> shifts = shiftRepository.findByHospitalAndPeriod(
            hospitalId, weekStart, weekEnd);

        List<DaySchedule> days = weekStart.datesUntil(weekEnd.plusDays(1))
            .map(date -> {
                List<ShiftResponse> dayShifts = shifts.stream()
                    .filter(s -> s.getShiftDate().equals(date))
                    .map(this::toShiftResponse)
                    .collect(Collectors.toList());

                long confirmed = dayShifts.stream()
                    .filter(s -> s.getStatus() == ShiftStatus.CONFIRMED
                              || s.getStatus() == ShiftStatus.COMPLETED)
                    .count();

                return DaySchedule.builder()
                    .date(date)
                    .dayLabel(date.getDayOfWeek()
                        .getDisplayName(TextStyle.FULL, new Locale("pt", "PT")))
                    .shifts(dayShifts)
                    .totalShifts(dayShifts.size())
                    .confirmedShifts((int) confirmed)
                    .build();
            }).collect(Collectors.toList());

        return WeeklyScheduleResponse.builder()
            .weekStart(weekStart).weekEnd(weekEnd).days(days).build();
    }

    @Transactional(readOnly = true)
    public List<ShiftResponse> getMyShifts(UUID userId, LocalDate from, LocalDate to) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        return shiftRepository.findByUserAndPeriod(hospitalId, userId, from, to)
            .stream().map(this::toShiftResponse).toList();
    }

    @Transactional
    public ShiftResponse createShift(CreateShiftRequest req) {
        UUID hospitalId = TenantContext.getCurrentHospital();

        // Verificar conflito
        UUID excludeId = UUID.randomUUID(); // UUID fictício para excluir (novo registo)
        if (shiftRepository.hasConflict(req.getUserId(), req.getShiftDate(), excludeId)) {
            throw new IllegalStateException(
                "Este funcionário já tem um turno agendado para " + req.getShiftDate());
        }

        Shift shift = Shift.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .user(userRepository.getReferenceById(req.getUserId()))
            .shiftType(req.getShiftType() != null ? req.getShiftType() : Shift.ShiftType.MORNING)
            .shiftDate(req.getShiftDate())
            .startTime(req.getStartTime())
            .endTime(req.getEndTime())
            .department(req.getDepartment())
            .notes(req.getNotes())
            .createdBy(getCurrentUser())
            .build();

        if (req.getWardId() != null) {
            shift.setWard(wardRepository.getReferenceById(req.getWardId()));
        }

        return toShiftResponse(shiftRepository.save(shift));
    }

    @Transactional
    public ShiftResponse updateShiftStatus(UUID id, UpdateShiftStatusRequest req) {
        Shift shift = shiftRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Turno não encontrado"));
        shift.setStatus(req.getStatus());
        if (req.getNotes() != null) shift.setNotes(req.getNotes());
        return toShiftResponse(shiftRepository.save(shift));
    }

    @Transactional
    public void deleteShift(UUID id) {
        Shift shift = shiftRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Turno não encontrado"));
        if (shift.getStatus() == ShiftStatus.COMPLETED) {
            throw new IllegalStateException("Não é possível eliminar um turno já concluído.");
        }
        shiftRepository.delete(shift);
    }

    // ------------------------------------------------
    // Pedidos de Folga
    // ------------------------------------------------

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getPendingLeaves() {
        UUID hospitalId = TenantContext.getCurrentHospital();
        return leaveRequestRepository.findByHospitalAndStatus(hospitalId, LeaveStatus.PENDING)
            .stream().map(this::toLeaveResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> getMyLeaves(UUID userId, Pageable pageable) {
        return leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(this::toLeaveResponse);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getApprovedInPeriod(LocalDate from, LocalDate to) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        return leaveRequestRepository.findApprovedInPeriod(hospitalId, from, to)
            .stream().map(this::toLeaveResponse).toList();
    }

    @Transactional
    public LeaveRequestResponse requestLeave(UUID userId, CreateLeaveRequest req) {
        UUID hospitalId = TenantContext.getCurrentHospital();

        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new IllegalArgumentException("Data de fim não pode ser anterior à data de início.");
        }

        // Verificar sobreposição
        UUID excludeId = UUID.randomUUID();
        if (leaveRequestRepository.hasOverlap(
            userId, req.getStartDate(), req.getEndDate(), excludeId)) {
            throw new IllegalStateException(
                "Já existe um pedido de ausência para este período.");
        }

        int totalDays = (int) (req.getEndDate().toEpochDay()
            - req.getStartDate().toEpochDay() + 1);

        LeaveRequest leave = LeaveRequest.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .user(userRepository.getReferenceById(userId))
            .leaveType(req.getLeaveType())
            .startDate(req.getStartDate())
            .endDate(req.getEndDate())
            .totalDays(totalDays)
            .reason(req.getReason())
            .build();

        return toLeaveResponse(leaveRequestRepository.save(leave));
    }

    @Transactional
    public LeaveRequestResponse approveLeave(UUID id, ApproveLeaveRequest req) {
        LeaveRequest leave = leaveRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Pedido de folga não encontrado"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Este pedido já foi processado.");
        }

        if (req.isApproved()) {
            leave.setStatus(LeaveStatus.APPROVED);
            leave.setApprovedBy(getCurrentUser());
            leave.setApprovedAt(OffsetDateTime.now());
        } else {
            leave.setStatus(LeaveStatus.REJECTED);
            leave.setRejectionReason(req.getRejectionReason());
        }

        return toLeaveResponse(leaveRequestRepository.save(leave));
    }

    @Transactional
    public LeaveRequestResponse cancelLeave(UUID id) {
        LeaveRequest leave = leaveRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Pedido de folga não encontrado"));

        if (leave.getStatus() == LeaveStatus.APPROVED
            && leave.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException(
                "Não é possível cancelar uma licença já iniciada.");
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        return toLeaveResponse(leaveRequestRepository.save(leave));
    }

    // ------------------------------------------------
    // Controlo de Ponto
    // ------------------------------------------------

    @Transactional
    public AttendanceResponse checkIn(UUID userId, CheckInRequest req) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        LocalDate today = LocalDate.now();

        // Verificar se já fez check-in hoje
        attendanceRepository.findByUserIdAndWorkDate(userId, today).ifPresent(a -> {
            throw new IllegalStateException("Check-in já registado para hoje.");
        });

        AttendanceRecord record = AttendanceRecord.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .user(userRepository.getReferenceById(userId))
            .workDate(today)
            .checkIn(OffsetDateTime.now())
            .status(AttendanceStatus.PRESENT)
            .notes(req.getNotes())
            .build();

        if (req.getShiftId() != null) {
            record.setShift(shiftRepository.getReferenceById(req.getShiftId()));
        }

        return toAttendanceResponse(attendanceRepository.save(record));
    }

    @Transactional
    public AttendanceResponse checkOut(UUID userId, CheckOutRequest req) {
        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRepository
            .findByUserIdAndWorkDate(userId, today)
            .orElseThrow(() -> new IllegalStateException(
                "Sem check-in registado para hoje. Faça check-in primeiro."));

        if (record.getCheckOut() != null) {
            throw new IllegalStateException("Check-out já registado para hoje.");
        }

        record.setCheckOut(OffsetDateTime.now());
        record.calculateMinutesWorked();

        // Calcular horas extra (se trabalhou mais de 8h)
        int standardMinutes = 8 * 60;
        if (record.getMinutesWorked() > standardMinutes) {
            record.setOvertimeMinutes(record.getMinutesWorked() - standardMinutes);
        }

        if (req.getNotes() != null) record.setNotes(req.getNotes());

        return toAttendanceResponse(attendanceRepository.save(record));
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByPeriod(
        UUID userId, LocalDate from, LocalDate to) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        return attendanceRepository.findByUserAndPeriod(hospitalId, userId, from, to)
            .stream().map(this::toAttendanceResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getDailyAttendance(LocalDate date) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        return attendanceRepository.findByHospitalAndDate(hospitalId, date)
            .stream().map(this::toAttendanceResponse).toList();
    }

    // ------------------------------------------------
    // Helpers
    // ------------------------------------------------

    private ao.hospitalao.modules.auth.entity.User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    // ------------------------------------------------
    // Mappers
    // ------------------------------------------------

    private ShiftResponse toShiftResponse(Shift s) {
        return ShiftResponse.builder()
            .id(s.getId())
            .userId(s.getUser().getId())
            .userFullName(s.getUser().getFullName())
            .userRole(s.getUser().getRoles() != null && !s.getUser().getRoles().isEmpty()
                ? s.getUser().getRoles().iterator().next().toString() : null)
            .shiftType(s.getShiftType())
            .shiftTypeLabel(shiftTypeLabel(s.getShiftType()))
            .shiftDate(s.getShiftDate())
            .startTime(s.getStartTime())
            .endTime(s.getEndTime())
            .durationHours(s.getDurationHours())
            .department(s.getDepartment())
            .wardId(s.getWard() != null ? s.getWard().getId() : null)
            .wardName(s.getWard() != null ? s.getWard().getName() : null)
            .status(s.getStatus())
            .statusLabel(shiftStatusLabel(s.getStatus()))
            .notes(s.getNotes())
            .createdAt(s.getCreatedAt())
            .build();
    }

    private LeaveRequestResponse toLeaveResponse(LeaveRequest l) {
        return LeaveRequestResponse.builder()
            .id(l.getId())
            .userId(l.getUser().getId())
            .userFullName(l.getUser().getFullName())
            .leaveType(l.getLeaveType())
            .leaveTypeLabel(leaveTypeLabel(l.getLeaveType()))
            .startDate(l.getStartDate())
            .endDate(l.getEndDate())
            .totalDays(l.getTotalDays())
            .reason(l.getReason())
            .status(l.getStatus())
            .statusLabel(leaveStatusLabel(l.getStatus()))
            .approvedByName(l.getApprovedBy() != null
                ? l.getApprovedBy().getFullName() : null)
            .approvedAt(l.getApprovedAt())
            .rejectionReason(l.getRejectionReason())
            .createdAt(l.getCreatedAt())
            .build();
    }

    private AttendanceResponse toAttendanceResponse(AttendanceRecord a) {
        return AttendanceResponse.builder()
            .id(a.getId())
            .userId(a.getUser().getId())
            .userFullName(a.getUser().getFullName())
            .workDate(a.getWorkDate())
            .checkIn(a.getCheckIn())
            .checkOut(a.getCheckOut())
            .minutesWorked(a.getMinutesWorked())
            .hoursWorked(a.getHoursWorked())
            .overtimeMinutes(a.getOvertimeMinutes())
            .status(a.getStatus())
            .statusLabel(attendanceStatusLabel(a.getStatus()))
            .notes(a.getNotes())
            .createdAt(a.getCreatedAt())
            .build();
    }

    private String shiftTypeLabel(Shift.ShiftType t) {
        return switch (t) {
            case MORNING   -> "Manhã";
            case AFTERNOON -> "Tarde";
            case NIGHT     -> "Noite";
            case FULL_DAY  -> "Dia Completo";
            case ON_CALL   -> "Chamada";
        };
    }

    private String shiftStatusLabel(ShiftStatus s) {
        return switch (s) {
            case SCHEDULED -> "Agendado";
            case CONFIRMED -> "Confirmado";
            case COMPLETED -> "Concluído";
            case CANCELLED -> "Cancelado";
            case SWAPPED   -> "Trocado";
        };
    }

    private String leaveTypeLabel(LeaveRequest.LeaveType t) {
        return switch (t) {
            case VACATION     -> "Férias";
            case SICK_LEAVE   -> "Baixa Médica";
            case PERSONAL     -> "Pessoal";
            case MATERNITY    -> "Maternidade";
            case PATERNITY    -> "Paternidade";
            case BEREAVEMENT  -> "Luto";
            case UNPAID       -> "Sem Vencimento";
            case COMPENSATORY -> "Compensatória";
        };
    }

    private String leaveStatusLabel(LeaveStatus s) {
        return switch (s) {
            case PENDING   -> "Pendente";
            case APPROVED  -> "Aprovado";
            case REJECTED  -> "Rejeitado";
            case CANCELLED -> "Cancelado";
        };
    }

    private String attendanceStatusLabel(AttendanceStatus s) {
        return switch (s) {
            case PRESENT  -> "Presente";
            case ABSENT   -> "Ausente";
            case LATE     -> "Atrasado";
            case ON_LEAVE -> "Em Licença";
            case HOLIDAY  -> "Feriado";
        };
    }
}