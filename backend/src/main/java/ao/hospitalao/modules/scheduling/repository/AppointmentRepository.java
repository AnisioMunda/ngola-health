// ============================================================
// AppointmentRepository.java
// ============================================================
package ao.hospitalao.modules.scheduling.repository;

import ao.hospitalao.modules.scheduling.entity.Appointment;
import ao.hospitalao.modules.scheduling.entity.Appointment.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

  // Agendamentos por médico e data
  List<Appointment> findByDoctorIdAndAppointmentDateOrderByStartTime(UUID doctorId, LocalDate date);

  // Agendamentos por hospital e intervalo de datas (para calendário)
  @Query(
      """
        SELECT a FROM Appointment a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.doctor
        WHERE a.hospital.id = :hospitalId
        AND a.appointmentDate BETWEEN :from AND :to
        ORDER BY a.appointmentDate, a.startTime
    """)
  List<Appointment> findByHospitalAndDateRange(
      @Param("hospitalId") UUID hospitalId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  // Agendamentos por médico e intervalo
  @Query(
      """
        SELECT a FROM Appointment a
        LEFT JOIN FETCH a.patient
        WHERE a.doctor.id = :doctorId
        AND a.appointmentDate BETWEEN :from AND :to
        ORDER BY a.appointmentDate, a.startTime
    """)
  List<Appointment> findByDoctorAndDateRange(
      @Param("doctorId") UUID doctorId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  // Agendamentos por paciente
  Page<Appointment> findByPatientIdOrderByAppointmentDateDescStartTimeDesc(
      UUID patientId, Pageable pageable);

  // Consultas futuras do paciente — Portal
  @Query(
      """
        SELECT a
        FROM Appointment a
        LEFT JOIN FETCH a.doctor
        WHERE a.patient.id = :patientId
        AND (
            a.appointmentDate > :today
            OR (
                a.appointmentDate = :today
                AND a.startTime >= :currentTime
            )
        )
        AND a.status IN ('SCHEDULED', 'CONFIRMED')
        ORDER BY a.appointmentDate ASC, a.startTime ASC
    """)
  List<Appointment> findUpcomingByPatient(
      @Param("patientId") UUID patientId,
      @Param("today") LocalDate today,
      @Param("currentTime") LocalTime currentTime,
      Pageable pageable);

  // Lugares ocupados num slot; a agenda bloqueada serializa as reservas concorrentes.
  @Query(
      """
        SELECT a.slotPosition FROM Appointment a
        WHERE a.doctor.id = :doctorId
        AND a.appointmentDate = :date
        AND a.startTime = :startTime
        AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
        ORDER BY a.slotPosition
    """)
  List<Integer> findOccupiedSlotPositions(
      @Param("doctorId") UUID doctorId,
      @Param("date") LocalDate date,
      @Param("startTime") LocalTime startTime);

  @Query(
      """
        SELECT COUNT(a) > 0 FROM Appointment a
        WHERE a.patient.id = :patientId
        AND a.doctor.id = :doctorId
        AND a.appointmentDate = :date
        AND a.startTime = :startTime
        AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
    """)
  boolean existsActiveAppointmentForPatientInSlot(
      @Param("patientId") UUID patientId,
      @Param("doctorId") UUID doctorId,
      @Param("date") LocalDate date,
      @Param("startTime") LocalTime startTime);

  // Filtros avançados para lista
  @Query(
      """
        SELECT a FROM Appointment a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.doctor
        WHERE a.hospital.id = :hospitalId
        AND (:doctorId  IS NULL OR a.doctor.id  = :doctorId)
        AND (:patientId IS NULL OR a.patient.id = :patientId)
        AND (:status    IS NULL OR a.status      = :status)
        AND (:date      IS NULL OR a.appointmentDate = :date)
        ORDER BY a.appointmentDate DESC, a.startTime
    """)
  Page<Appointment> findWithFilters(
      @Param("hospitalId") UUID hospitalId,
      @Param("doctorId") UUID doctorId,
      @Param("patientId") UUID patientId,
      @Param("status") AppointmentStatus status,
      @Param("date") LocalDate date,
      Pageable pageable);

  // Hoje — para o dashboard
  @Query(
      """
        SELECT COUNT(a) FROM Appointment a
        WHERE a.hospital.id = :hospitalId
        AND a.appointmentDate = :today
        AND a.status IN ('SCHEDULED', 'CONFIRMED')
    """)
  long countTodayPending(@Param("hospitalId") UUID hospitalId, @Param("today") LocalDate today);
}
