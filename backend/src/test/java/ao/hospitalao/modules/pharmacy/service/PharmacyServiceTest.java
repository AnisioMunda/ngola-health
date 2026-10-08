package ao.hospitalao.modules.pharmacy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ao.hospitalao.modules.auth.entity.User;
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.episodes.application.EpisodeApplicationService;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.patients.repository.PatientRepository;
import ao.hospitalao.modules.pharmacy.dto.PharmacyDtos.DispenseRequest;
import ao.hospitalao.modules.pharmacy.entity.Medication;
import ao.hospitalao.modules.pharmacy.entity.StockBatch;
import ao.hospitalao.modules.pharmacy.repository.MedicationRepository;
import ao.hospitalao.modules.pharmacy.repository.StockBatchRepository;
import ao.hospitalao.modules.pharmacy.repository.StockMovementRepository;
import ao.hospitalao.security.tenant.TenantContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PharmacyServiceTest {

  private static final ZoneId ANGOLA_ZONE = ZoneId.of("Africa/Luanda");

  @Mock private MedicationRepository medicationRepository;
  @Mock private StockBatchRepository batchRepository;
  @Mock private StockMovementRepository movementRepository;
  @Mock private PatientRepository patientRepository;
  @Mock private EpisodeApplicationService episodeApplicationService;
  @Mock private UserRepository userRepository;
  @Mock private HospitalRepository hospitalRepository;

  @InjectMocks private PharmacyService pharmacyService;

  private final UUID hospitalId = UUID.randomUUID();
  private final UUID medicationId = UUID.randomUUID();
  private Medication medication;
  private User pharmacist;

  @BeforeEach
  void setUp() {
    medication =
        Medication.builder().id(medicationId).name("Medicamento de teste").active(true).build();
    pharmacist = User.builder().username("pharmacist").build();
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("pharmacist", "test", List.of()));
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void dispensesByFefoAndRecordsOnlyTheConsumedQuantities() {
    LocalDate today = LocalDate.now(ANGOLA_ZONE);
    StockBatch firstBatch = batch(today.plusDays(2), 2);
    StockBatch secondBatch = batch(today.plusDays(10), 5);
    when(medicationRepository.findByHospitalIdAndId(hospitalId, medicationId))
        .thenReturn(Optional.of(medication));
    when(batchRepository.findAvailableBatchesFefo(any(), any()))
        .thenReturn(List.of(firstBatch, secondBatch));
    when(batchRepository.getTotalAvailableQuantity(eq(medicationId), any())).thenReturn(4);
    when(userRepository.findByUsername("pharmacist")).thenReturn(Optional.of(pharmacist));

    try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

      pharmacyService.dispense(request(3));
    }

    assertThat(firstBatch.getQuantityAvailable()).isZero();
    assertThat(secondBatch.getQuantityAvailable()).isEqualTo(4);
    verify(batchRepository).findAvailableBatchesFefo(medicationId, today);
    verify(batchRepository).save(firstBatch);
    verify(batchRepository).save(secondBatch);
    verify(movementRepository, org.mockito.Mockito.times(2)).save(any());
  }

  @Test
  void rejectsInsufficientStockBeforeChangingAnyBatch() {
    StockBatch batch = batch(LocalDate.now(ANGOLA_ZONE).plusDays(2), 2);
    when(medicationRepository.findByHospitalIdAndId(hospitalId, medicationId))
        .thenReturn(Optional.of(medication));
    when(batchRepository.findAvailableBatchesFefo(any(), any())).thenReturn(List.of(batch));

    try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

      assertThatThrownBy(() -> pharmacyService.dispense(request(3)))
          .isInstanceOf(ResponseStatusException.class)
          .extracting(error -> ((ResponseStatusException) error).getStatusCode())
          .isEqualTo(HttpStatus.CONFLICT);
    }

    assertThat(batch.getQuantityAvailable()).isEqualTo(2);
    verify(batchRepository, never()).save(any());
    verifyNoInteractions(movementRepository);
  }

  @Test
  void scopesMedicationLookupsToTheCurrentHospital() {
    when(medicationRepository.findByHospitalIdAndId(hospitalId, medicationId))
        .thenReturn(Optional.empty());

    try (MockedStatic<TenantContext> tenantContext = mockStatic(TenantContext.class)) {
      tenantContext.when(TenantContext::getCurrentHospital).thenReturn(hospitalId);

      assertThatThrownBy(() -> pharmacyService.findBatchesByMedication(medicationId))
          .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    verify(batchRepository, never()).findByMedicationIdAndHospitalId(any(), any());
  }

  private StockBatch batch(LocalDate expiryDate, int quantity) {
    return StockBatch.builder()
        .id(UUID.randomUUID())
        .medication(medication)
        .batchNumber(UUID.randomUUID().toString())
        .expiryDate(expiryDate)
        .quantityReceived(quantity)
        .quantityAvailable(quantity)
        .build();
  }

  private DispenseRequest request(int quantity) {
    var request = new DispenseRequest();
    request.setMedicationId(medicationId);
    request.setQuantity(quantity);
    return request;
  }
}
