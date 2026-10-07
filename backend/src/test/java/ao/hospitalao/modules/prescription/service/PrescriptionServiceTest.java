package ao.hospitalao.modules.prescription.service;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.repository.EpisodeRepository;
import ao.hospitalao.modules.hospitals.entity.Hospital;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.inpatient.repository.AdmissionRepository;
import ao.hospitalao.modules.patients.entity.Patient;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.pharmacy.entity.Medication;
import ao.hospitalao.modules.pharmacy.entity.StockBatch;
import ao.hospitalao.modules.pharmacy.repository.MedicationRepository;
import ao.hospitalao.modules.pharmacy.repository.StockBatchRepository;
import ao.hospitalao.modules.prescription.dto.PrescriptionDtos.*;
import ao.hospitalao.modules.prescription.entity.Prescription;
import ao.hospitalao.modules.prescription.entity.Prescription.PrescriptionStatus;
import ao.hospitalao.modules.prescription.entity.PrescriptionItem;
import ao.hospitalao.modules.prescription.entity.PrescriptionItem.ItemStatus;
import ao.hospitalao.modules.prescription.entity.Dispensation;
import ao.hospitalao.modules.prescription.repository.DispensationRepository;
import ao.hospitalao.modules.prescription.repository.PrescriptionItemRepository;
import ao.hospitalao.modules.prescription.repository.PrescriptionRepository;
import ao.hospitalao.modules.prescription.service.PrescriptionService;
import ao.hospitalao.security.tenant.TenantContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionService — Testes Unitários")
class PrescriptionServiceTest {

    @Mock PrescriptionRepository     prescriptionRepository;
    @Mock PrescriptionItemRepository itemRepository;
    @Mock DispensationRepository     dispensationRepository;
    @Mock MedicationRepository       medicationRepository;
    @Mock StockBatchRepository       stockBatchRepository;
    @Mock PatientRepository          patientRepository;
    @Mock UserRepository             userRepository;
    @Mock HospitalRepository         hospitalRepository;
    @Mock EpisodeRepository          episodeRepository;
    @Mock AdmissionRepository        admissionRepository;

    @InjectMocks PrescriptionService prescriptionService;

    private UUID hospitalId;
    private UUID patientId;
    private UUID medicationId;
    private UUID doctorId;

    @BeforeEach
    void setUp() {
        hospitalId   = UUID.randomUUID();
        patientId    = UUID.randomUUID();
        medicationId = UUID.randomUUID();
        doctorId     = UUID.randomUUID();

        // Mock TenantContext
        try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
            tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);
        }

        // Mock SecurityContext
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("doctor1");
        SecurityContext secCtx = mock(SecurityContext.class);
        lenient().when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------
    // Testes de criação
    // ------------------------------------------------

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Deve criar prescrição com stock suficiente")
        void shouldCreatePrescriptionWithSufficientStock() {
            // Arrange
            var medication = buildMedication();
            var patient    = buildPatient();
            var doctor     = buildDoctor();
            var hospital   = buildHospital();

            when(medicationRepository.findById(medicationId))
                .thenReturn(Optional.of(medication));
            when(stockBatchRepository.getTotalAvailableQuantity(eq(medicationId), any()))
                .thenReturn(100);
            when(patientRepository.getReferenceById(patientId)).thenReturn(patient);
            when(userRepository.findByUsername("doctor1")).thenReturn(Optional.of(doctor));
            when(hospitalRepository.getReferenceById(hospitalId)).thenReturn(hospital);
            when(prescriptionRepository.nextPrescriptionNumber()).thenReturn(1L);
            when(prescriptionRepository.save(any())).thenAnswer(inv -> {
                Prescription p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
                tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

                var req = buildCreateRequest(5);

                // Act
                PrescriptionResponse response = prescriptionService.create(req);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getPrescriptionNumber()).startsWith("RX-");
                assertThat(response.getPatientId()).isEqualTo(patientId);
                verify(prescriptionRepository, times(1)).save(any());
            }
        }

        @Test
        @DisplayName("Deve rejeitar prescrição com stock insuficiente")
        void shouldRejectPrescriptionWithInsufficientStock() {
            // Arrange
            var medication = buildMedication();
            var doctor     = buildDoctor();
            var hospital   = buildHospital();

            when(medicationRepository.findById(medicationId))
                .thenReturn(Optional.of(medication));
            when(stockBatchRepository.getTotalAvailableQuantity(eq(medicationId), any()))
                .thenReturn(2); // Só 2 disponíveis, pedimos 5
            when(patientRepository.getReferenceById(patientId))
                .thenReturn(buildPatient());
            when(userRepository.findByUsername("doctor1"))
                .thenReturn(Optional.of(doctor));
            when(hospitalRepository.getReferenceById(hospitalId))
                .thenReturn(hospital);
            when(prescriptionRepository.nextPrescriptionNumber()).thenReturn(1L);

            try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
                tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

                var req = buildCreateRequest(5);

                // Act & Assert
                assertThatThrownBy(() -> prescriptionService.create(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Stock insuficiente");

                verify(prescriptionRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("Deve rejeitar prescrição sem itens")
        void shouldRejectPrescriptionWithNoItems() {
            try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
                tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

                var req = new CreatePrescriptionRequest();
                req.setPatientId(patientId);
                req.setItems(List.of()); // Lista vazia

                assertThatThrownBy(() -> prescriptionService.create(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pelo menos um medicamento");
            }
        }
    }

    // ------------------------------------------------
    // Testes de dispensa
    // ------------------------------------------------

    @Nested
    @DisplayName("dispense()")
    class DispenseTests {

        @Test
        @DisplayName("Deve dispensar medicamento e actualizar stock FEFO")
        void shouldDispenseAndUpdateStockFefo() {
            // Arrange
            var prescription = buildActivePrescription();
            var item         = buildPrescriptionItem(prescription, 10, 0);
            prescription.setItems(new ArrayList<>(List.of(item)));

            var batch = buildStockBatch(50);
            var doctor = buildDoctor();

            when(prescriptionRepository.findByIdWithRelations(any()))
                .thenReturn(Optional.of(prescription));
            when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));
            when(stockBatchRepository.findAvailableBatchesFefo(any(), any()))
                .thenReturn(List.of(batch));
            when(userRepository.findByUsername("doctor1"))
                .thenReturn(Optional.of(doctor));
            when(prescriptionRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
            when(itemRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
                tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

                var req = new DispenseItemRequest();
                req.setPrescriptionItemId(item.getId());
                req.setQuantityToDispense(5);

                // Act
                prescriptionService.dispense(prescription.getId(), req);

                // Assert
                verify(dispensationRepository, times(1)).save(any());
                verify(stockBatchRepository, times(1)).save(batch);
                assertThat(batch.getQuantityAvailable()).isEqualTo(45); // 50 - 5
            }
        }

        @Test
        @DisplayName("Deve rejeitar dispensa de prescrição cancelada")
        void shouldRejectDispenseOfCancelledPrescription() {
            var prescription = buildActivePrescription();
            prescription.setStatus(PrescriptionStatus.CANCELLED);

            when(prescriptionRepository.findByIdWithRelations(any()))
                .thenReturn(Optional.of(prescription));

            try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
                tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

                var req = new DispenseItemRequest();
                req.setPrescriptionItemId(UUID.randomUUID());
                req.setQuantityToDispense(1);

                assertThatThrownBy(() ->
                    prescriptionService.dispense(prescription.getId(), req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("cancelada");
            }
        }

        @Test
        @DisplayName("Deve rejeitar dispensa de prescrição expirada")
        void shouldRejectDispenseOfExpiredPrescription() {
            var prescription = buildActivePrescription();
            prescription.setExpiryDate(LocalDate.now().minusDays(1)); // Expirou ontem

            when(prescriptionRepository.findByIdWithRelations(any()))
                .thenReturn(Optional.of(prescription));
            when(prescriptionRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
                tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

                var req = new DispenseItemRequest();
                req.setPrescriptionItemId(UUID.randomUUID());
                req.setQuantityToDispense(1);

                assertThatThrownBy(() ->
                    prescriptionService.dispense(prescription.getId(), req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("expirou");
            }
        }

        @Test
        @DisplayName("Deve rejeitar quantidade superior à pendente")
        void shouldRejectQuantityExceedingPending() {
            var prescription = buildActivePrescription();
            var item         = buildPrescriptionItem(prescription, 5, 3); // 5 prescritos, 3 dispensados
            prescription.setItems(new ArrayList<>(List.of(item)));

            when(prescriptionRepository.findByIdWithRelations(any()))
                .thenReturn(Optional.of(prescription));
            when(itemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

            try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
                tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

                var req = new DispenseItemRequest();
                req.setPrescriptionItemId(item.getId());
                req.setQuantityToDispense(5); // Só restam 2

                assertThatThrownBy(() ->
                    prescriptionService.dispense(prescription.getId(), req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("excede a quantidade pendente");
            }
        }
    }

    // ------------------------------------------------
    // Testes de cancelamento
    // ------------------------------------------------

    @Nested
    @DisplayName("cancel()")
    class CancelTests {

        @Test
        @DisplayName("Deve cancelar prescrição activa")
        void shouldCancelActivePrescription() {
            var prescription = buildActivePrescription();
            var item = buildPrescriptionItem(prescription, 5, 0);
            prescription.setItems(new ArrayList<>(List.of(item)));

            when(prescriptionRepository.findByIdWithRelations(any()))
                .thenReturn(Optional.of(prescription));
            when(prescriptionRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByUsername("doctor1"))
                .thenReturn(Optional.of(buildDoctor()));

            try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
                tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

                var req = new CancelPrescriptionRequest();
                req.setReason("Paciente com alergia ao medicamento");

                PrescriptionResponse response =
                    prescriptionService.cancel(prescription.getId(), req);

                assertThat(response.getStatus()).isEqualTo(PrescriptionStatus.CANCELLED);
                assertThat(response.getCancelledReason())
                    .isEqualTo("Paciente com alergia ao medicamento");
            }
        }

        @Test
        @DisplayName("Não deve cancelar prescrição já dispensada")
        void shouldNotCancelDispensedPrescription() {
            var prescription = buildActivePrescription();
            prescription.setStatus(PrescriptionStatus.DISPENSED);

            when(prescriptionRepository.findByIdWithRelations(any()))
                .thenReturn(Optional.of(prescription));

            try (MockedStatic<TenantContext> tc = mockStatic(TenantContext.class)) {
                tc.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

                var req = new CancelPrescriptionRequest();
                req.setReason("Tentativa de cancelamento");

                assertThatThrownBy(() ->
                    prescriptionService.cancel(prescription.getId(), req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("já dispensada");
            }
        }
    }

    // ------------------------------------------------
    // Builders auxiliares
    // ------------------------------------------------

    private Medication buildMedication() {
        Medication m = new Medication();
        m.setId(medicationId);
        m.setName("Amoxicilina 500mg");
        m.setUnit("comprimido");
        return m;
    }

    private Patient buildPatient() {
        Patient p = new Patient();
        p.setId(patientId);
        p.setFullName("João Silva");
        return p;
    }

    private User buildDoctor() {
        User u = new User();
        u.setId(doctorId);
        u.setFullName("Dr. António Costa");
        u.setUsername("doctor1");
        return u;
    }

    private Hospital buildHospital() {
        Hospital h = new Hospital();
        h.setId(hospitalId);
        h.setName("HospitalAO Principal");
        return h;
    }

    private Prescription buildActivePrescription() {
        Prescription p = new Prescription();
        p.setId(UUID.randomUUID());
        p.setPatient(buildPatient());
        p.setDoctor(buildDoctor());
        p.setStatus(PrescriptionStatus.ACTIVE);
        p.setPrescriptionNumber("RX-2026-00001");
        p.setPrescriptionDate(LocalDate.now());
        p.setExpiryDate(LocalDate.now().plusDays(30));
        p.setItems(new ArrayList<>());
        return p;
    }

    private PrescriptionItem buildPrescriptionItem(
        Prescription prescription, int prescribed, int dispensed) {
        PrescriptionItem item = new PrescriptionItem();
        item.setId(UUID.randomUUID());
        item.setPrescription(prescription);
        item.setMedication(buildMedication());
        item.setQuantityPrescribed(prescribed);
        item.setQuantityDispensed(dispensed);
        item.setDosage("1 comprimido 3x ao dia");
        item.setStatus(dispensed >= prescribed ? ItemStatus.DISPENSED : ItemStatus.PENDING);
        return item;
    }

    private StockBatch buildStockBatch(int quantity) {
        StockBatch b = new StockBatch();
        b.setId(UUID.randomUUID());
        b.setBatchNumber("LOTE-2026-001");
        b.setQuantityAvailable(quantity);
        b.setExpiryDate(LocalDate.now().plusMonths(12));
        return b;
    }

    private CreatePrescriptionRequest buildCreateRequest(int quantity) {
        var req = new CreatePrescriptionRequest();
        req.setPatientId(patientId);
        req.setValidityDays(30);

        var itemReq = new CreatePrescriptionItemRequest();
        itemReq.setMedicationId(medicationId);
        itemReq.setQuantityPrescribed(quantity);
        itemReq.setDosage("1 comprimido 3x ao dia");

        req.setItems(List.of(itemReq));
        return req;
    }
}