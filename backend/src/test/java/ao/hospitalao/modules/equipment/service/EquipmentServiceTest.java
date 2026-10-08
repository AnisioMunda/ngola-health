package ao.hospitalao.modules.equipment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.equipment.dto.EquipmentDtos.*;
import ao.hospitalao.modules.equipment.entity.Equipment;
import ao.hospitalao.modules.equipment.entity.Equipment.EquipmentCategory;
import ao.hospitalao.modules.equipment.entity.Equipment.EquipmentStatus;
import ao.hospitalao.modules.equipment.entity.MaintenanceRecord.MaintenanceType;
import ao.hospitalao.modules.equipment.repository.EquipmentRepository;
import ao.hospitalao.modules.equipment.repository.MaintenanceRepository;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.inpatient.repository.WardRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("EquipmentService — Testes Unitários")
class EquipmentServiceTest {

  @Mock EquipmentRepository equipmentRepository;
  @Mock MaintenanceRepository maintenanceRepository;
  @Mock HospitalRepository hospitalRepository;
  @Mock WardRepository wardRepository;
  @Mock UserRepository userRepository;

  @InjectMocks EquipmentService equipmentService;

  private UUID hospitalId;

  @BeforeEach
  void setUp() {
    hospitalId = UUID.randomUUID();

    Authentication auth = mock(Authentication.class);
    lenient().when(auth.getName()).thenReturn("manager1");
    SecurityContext secCtx = mock(SecurityContext.class);
    lenient().when(secCtx.getAuthentication()).thenReturn(auth);
    SecurityContextHolder.setContext(secCtx);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // ------------------------------------------------
  // create()
  // ------------------------------------------------

  @Nested
  @DisplayName("create()")
  class CreateTests {

    @Test
    @DisplayName("Deve criar equipamento com campos obrigatórios")
    void shouldCreateEquipmentWithRequiredFields() {
      var hospital = buildHospital();

      when(hospitalRepository.getReferenceById(hospitalId)).thenReturn(hospital);
      when(equipmentRepository.save(any()))
          .thenAnswer(
              inv -> {
                Equipment e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
              });

      try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
        tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

        var req = new CreateEquipmentRequest();
        req.setName("Ventilador Mecânico");
        req.setCode("EQ-001");
        req.setCategory(EquipmentCategory.THERAPEUTIC);
        req.setMaintenanceIntervalDays(180);
        req.setNextMaintenanceDate(LocalDate.now().plusDays(180));

        EquipmentResponse response = equipmentService.create(req);

        assertThat(response.getName()).isEqualTo("Ventilador Mecânico");
        assertThat(response.getCode()).isEqualTo("EQ-001");
        assertThat(response.getCategoryLabel()).isEqualTo("Terapêutico");
        assertThat(response.getStatusLabel()).isEqualTo("Activo");
        verify(equipmentRepository).save(any());
      }
    }

    @Test
    @DisplayName("Deve usar categoria OTHER por omissão")
    void shouldDefaultToCategoryOther() {
      var hospital = buildHospital();

      when(hospitalRepository.getReferenceById(hospitalId)).thenReturn(hospital);
      when(equipmentRepository.save(any()))
          .thenAnswer(
              inv -> {
                Equipment e = inv.getArgument(0);
                e.setId(UUID.randomUUID());
                return e;
              });

      try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
        tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

        var req = new CreateEquipmentRequest();
        req.setName("Cadeira de Rodas");
        req.setCode("EQ-002");
        // Sem category definida

        EquipmentResponse response = equipmentService.create(req);

        assertThat(response.getCategory()).isEqualTo(EquipmentCategory.OTHER);
      }
    }
  }

  // ------------------------------------------------
  // updateStatus()
  // ------------------------------------------------

  @Nested
  @DisplayName("updateStatus()")
  class UpdateStatusTests {

    @Test
    @DisplayName("Deve actualizar estado para MAINTENANCE")
    void shouldUpdateStatusToMaintenance() {
      var equipment = buildEquipment(EquipmentStatus.ACTIVE);

      when(equipmentRepository.findById(equipment.getId())).thenReturn(Optional.of(equipment));
      when(equipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      var req = new UpdateStatusRequest();
      req.setStatus(EquipmentStatus.MAINTENANCE);
      req.setNotes("Em manutenção preventiva programada");

      EquipmentResponse response = equipmentService.updateStatus(equipment.getId(), req);

      assertThat(response.getStatus()).isEqualTo(EquipmentStatus.MAINTENANCE);
      assertThat(response.getStatusLabel()).isEqualTo("Em Manutenção");
    }

    @Test
    @DisplayName("Deve lançar excepção para equipamento inexistente")
    void shouldThrowForNonExistentEquipment() {
      when(equipmentRepository.findById(any())).thenReturn(Optional.empty());

      var req = new UpdateStatusRequest();
      req.setStatus(EquipmentStatus.REPAIR);

      assertThatThrownBy(() -> equipmentService.updateStatus(UUID.randomUUID(), req))
          .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
          .hasMessageContaining("não encontrado");
    }
  }

  // ------------------------------------------------
  // addMaintenance()
  // ------------------------------------------------

  @Nested
  @DisplayName("addMaintenance()")
  class MaintenanceTests {

    @Test
    @DisplayName("Deve registar manutenção e actualizar próxima data")
    void shouldRecordMaintenanceAndUpdateNextDate() {
      var equipment = buildEquipment(EquipmentStatus.MAINTENANCE);
      var manager = buildManager();
      LocalDate nextDate = LocalDate.now().plusDays(365);

      when(equipmentRepository.findById(equipment.getId())).thenReturn(Optional.of(equipment));
      when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
      when(maintenanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(equipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(maintenanceRepository.findByEquipmentIdOrderByPerformedAtDesc(any()))
          .thenReturn(List.of());

      var req = new CreateMaintenanceRequest();
      req.setType(MaintenanceType.PREVENTIVE);
      req.setDescription("Substituição de filtros e lubrificação");
      req.setNextMaintenanceDate(nextDate);
      req.setResult("OK");

      EquipmentResponse response = equipmentService.addMaintenance(equipment.getId(), req);

      // Deve voltar para ACTIVE após manutenção preventiva
      assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.ACTIVE);
      assertThat(equipment.getNextMaintenanceDate()).isEqualTo(nextDate);
      verify(maintenanceRepository).save(any());
    }

    @Test
    @DisplayName("Deve calcular próxima manutenção pelo intervalo quando data não é fornecida")
    void shouldCalculateNextMaintenanceDateFromInterval() {
      var equipment = buildEquipment(EquipmentStatus.ACTIVE);
      equipment.setMaintenanceIntervalDays(90);
      var manager = buildManager();

      when(equipmentRepository.findById(equipment.getId())).thenReturn(Optional.of(equipment));
      when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
      when(maintenanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(equipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(maintenanceRepository.findByEquipmentIdOrderByPerformedAtDesc(any()))
          .thenReturn(List.of());

      var req = new CreateMaintenanceRequest();
      req.setType(MaintenanceType.INSPECTION);
      req.setDescription("Inspecção de rotina");
      // Sem nextMaintenanceDate

      equipmentService.addMaintenance(equipment.getId(), req);

      // Deve calcular: hoje + 90 dias
      assertThat(equipment.getNextMaintenanceDate()).isEqualTo(LocalDate.now().plusDays(90));
    }

    @Test
    @DisplayName("Não deve voltar para ACTIVE após manutenção correctiva")
    void shouldNotRestoreActiveAfterCorrectiveMaintenance() {
      var equipment = buildEquipment(EquipmentStatus.MAINTENANCE);
      var manager = buildManager();

      when(equipmentRepository.findById(equipment.getId())).thenReturn(Optional.of(equipment));
      when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
      when(maintenanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(equipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
      when(maintenanceRepository.findByEquipmentIdOrderByPerformedAtDesc(any()))
          .thenReturn(List.of());

      var req = new CreateMaintenanceRequest();
      req.setType(MaintenanceType.CORRECTIVE); // Correctiva, não preventiva
      req.setDescription("Reparação da peça X");
      req.setResult("REPAIRED");

      equipmentService.addMaintenance(equipment.getId(), req);

      // Estado deve permanecer MAINTENANCE
      assertThat(equipment.getStatus()).isEqualTo(EquipmentStatus.MAINTENANCE);
    }
  }

  // ------------------------------------------------
  // findMaintenanceDue()
  // ------------------------------------------------

  @Nested
  @DisplayName("findMaintenanceDue()")
  class MaintenanceDueTests {

    @Test
    @DisplayName("Deve retornar equipamentos com manutenção em atraso")
    void shouldReturnEquipmentWithOverdueMaintenance() {
      var overdue = buildEquipment(EquipmentStatus.ACTIVE);
      overdue.setNextMaintenanceDate(LocalDate.now().minusDays(5));

      when(equipmentRepository.findMaintenanceDue(any(), any())).thenReturn(List.of(overdue));

      try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
        tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

        List<EquipmentResponse> result = equipmentService.findMaintenanceDue();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isMaintenanceDue()).isTrue();
      }
    }
  }

  // ------------------------------------------------
  // getStats()
  // ------------------------------------------------

  @Test
  @DisplayName("Deve retornar estatísticas correctas")
  void shouldReturnCorrectStats() {
    when(equipmentRepository.countByHospitalIdAndActiveTrue(hospitalId)).thenReturn(50L);
    when(equipmentRepository.countByHospitalIdAndStatus(hospitalId, EquipmentStatus.ACTIVE))
        .thenReturn(45L);
    when(equipmentRepository.countByHospitalIdAndStatus(hospitalId, EquipmentStatus.MAINTENANCE))
        .thenReturn(3L);
    when(equipmentRepository.countByHospitalIdAndActiveTrueAndWarrantyExpiryBefore(
            eq(hospitalId), any()))
        .thenReturn(7L);
    when(equipmentRepository.findMaintenanceDue(eq(hospitalId), any()))
        .thenReturn(List.of(buildEquipment(EquipmentStatus.ACTIVE)));
    when(equipmentRepository.findCalibrationDue(eq(hospitalId), any())).thenReturn(List.of());

    try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
      tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

      EquipmentStatsDto stats = equipmentService.getStats();

      assertThat(stats.getTotalEquipment()).isEqualTo(50);
      assertThat(stats.getActiveEquipment()).isEqualTo(45);
      assertThat(stats.getInMaintenance()).isEqualTo(3);
      assertThat(stats.getMaintenanceDue()).isEqualTo(1);
      assertThat(stats.getCalibrationDue()).isEqualTo(0);
      assertThat(stats.getWarrantyExpired()).isEqualTo(7);
    }
  }

  // ------------------------------------------------
  // Builders
  // ------------------------------------------------

  private Hospital buildHospital() {
    Hospital h = new Hospital();
    h.setId(hospitalId);
    h.setName("HospitalAO");
    return h;
  }

  private Equipment buildEquipment(EquipmentStatus status) {
    Equipment e = new Equipment();
    e.setId(UUID.randomUUID());
    e.setName("Monitor de Sinais Vitais");
    e.setCode("EQ-MSV-001");
    e.setBrand("Philips");
    e.setModel("IntelliVue MX40");
    e.setCategory(EquipmentCategory.MONITORING);
    e.setStatus(status);
    e.setMaintenanceIntervalDays(365);
    e.setActive(true);
    e.setMaintenanceRecords(new ArrayList<>());
    return e;
  }

  private ao.hospitalao.modules.auth.entity.User buildManager() {
    var u = new ao.hospitalao.modules.auth.entity.User();
    u.setId(UUID.randomUUID());
    u.setFullName("Gestor Silva");
    u.setUsername("manager1");
    return u;
  }
}
