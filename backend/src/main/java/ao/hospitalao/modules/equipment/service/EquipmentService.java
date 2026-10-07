package ao.hospitalao.modules.equipment.service;
 
import ao.hospitalao.modules.auth.repository.UserRepository;
import ao.hospitalao.modules.equipment.dto.EquipmentDtos.*;
import ao.hospitalao.modules.equipment.entity.Equipment;
import ao.hospitalao.modules.equipment.entity.Equipment.EquipmentStatus;
import ao.hospitalao.modules.equipment.entity.MaintenanceRecord;
import ao.hospitalao.modules.equipment.repository.EquipmentRepository;
import ao.hospitalao.modules.equipment.repository.MaintenanceRepository;
import ao.hospitalao.modules.hospitals.repository.HospitalRepository;
import ao.hospitalao.modules.inpatient.repository.WardRepository;
import ao.hospitalao.security.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
 
@Slf4j
@Service
@RequiredArgsConstructor
public class EquipmentService {
 
    private final EquipmentRepository  equipmentRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final HospitalRepository   hospitalRepository;
    private final WardRepository       wardRepository;
    private final UserRepository       userRepository;
 
    @Transactional(readOnly = true)
    public EquipmentStatsDto getStats() {
        UUID hospitalId = TenantContext.getCurrentHospital();
        LocalDate today = LocalDate.now();
        return EquipmentStatsDto.builder()
            .totalEquipment(equipmentRepository.countByHospitalIdAndActiveTrue(hospitalId))
            .activeEquipment(equipmentRepository.countByHospitalIdAndStatus(hospitalId, EquipmentStatus.ACTIVE))
            .inMaintenance(equipmentRepository.countByHospitalIdAndStatus(hospitalId, EquipmentStatus.MAINTENANCE))
            .maintenanceDue(equipmentRepository.findMaintenanceDue(hospitalId, today).size())
            .calibrationDue(equipmentRepository.findCalibrationDue(hospitalId, today).size())
            .build();
    }
 
    @Transactional(readOnly = true)
    public Page<EquipmentResponse> findAll(String query, Pageable pageable) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        Page<Equipment> page = (query != null && !query.isBlank())
            ? equipmentRepository.search(hospitalId, query, pageable)
            : equipmentRepository.findByHospitalIdAndActiveTrue(hospitalId, pageable);
        return page.map(e -> toResponse(e, false));
    }
 
    @Transactional(readOnly = true)
    public EquipmentResponse findById(UUID id) {
        return toResponse(getOrThrow(id), true);
    }
 
    @Transactional(readOnly = true)
    public List<EquipmentResponse> findMaintenanceDue() {
        UUID hospitalId = TenantContext.getCurrentHospital();
        return equipmentRepository.findMaintenanceDue(hospitalId, LocalDate.now().plusDays(7))
            .stream().map(e -> toResponse(e, false)).toList();
    }
 
    @Transactional(readOnly = true)
    public List<EquipmentResponse> findCalibrationDue() {
        UUID hospitalId = TenantContext.getCurrentHospital();
        return equipmentRepository.findCalibrationDue(hospitalId, LocalDate.now().plusDays(7))
            .stream().map(e -> toResponse(e, false)).toList();
    }
 
    @Transactional
    public EquipmentResponse create(CreateEquipmentRequest req) {
        UUID hospitalId = TenantContext.getCurrentHospital();
        Equipment e = Equipment.builder()
            .hospital(hospitalRepository.getReferenceById(hospitalId))
            .name(req.getName()).code(req.getCode())
            .brand(req.getBrand()).model(req.getModel())
            .serialNumber(req.getSerialNumber())
            .category(req.getCategory() != null ? req.getCategory() :
                Equipment.EquipmentCategory.OTHER)
            .location(req.getLocation())
            .purchaseDate(req.getPurchaseDate())
            .purchasePrice(req.getPurchasePrice())
            .warrantyExpiry(req.getWarrantyExpiry())
            .nextMaintenanceDate(req.getNextMaintenanceDate())
            .maintenanceIntervalDays(req.getMaintenanceIntervalDays() != null ?
                req.getMaintenanceIntervalDays() : 365)
            .nextCalibrationDate(req.getNextCalibrationDate())
            .notes(req.getNotes())
            .build();
        if (req.getWardId() != null)
            e.setWard(wardRepository.getReferenceById(req.getWardId()));
        return toResponse(equipmentRepository.save(e), false);
    }
 
    @Transactional
    public EquipmentResponse updateStatus(UUID id, UpdateStatusRequest req) {
        Equipment e = getOrThrow(id);
        e.setStatus(req.getStatus());
        if (req.getNotes() != null) e.setNotes(req.getNotes());
        return toResponse(equipmentRepository.save(e), false);
    }
 
    @Transactional
    public EquipmentResponse addMaintenance(UUID equipmentId, CreateMaintenanceRequest req) {
        Equipment e = getOrThrow(equipmentId);
        var user = getCurrentUser();
 
        MaintenanceRecord record = MaintenanceRecord.builder()
            .equipment(e).type(req.getType())
            .performedBy(user)
            .description(req.getDescription())
            .cost(req.getCost())
            .nextMaintenanceDate(req.getNextMaintenanceDate())
            .partsReplaced(req.getPartsReplaced())
            .result(req.getResult() != null ? req.getResult() : "OK")
            .build();
 
        maintenanceRepository.save(record);
 
        // Actualizar próxima manutenção no equipamento
        if (req.getNextMaintenanceDate() != null) {
            e.setNextMaintenanceDate(req.getNextMaintenanceDate());
        } else if (e.getMaintenanceIntervalDays() != null) {
            e.setNextMaintenanceDate(
                LocalDate.now().plusDays(e.getMaintenanceIntervalDays()));
        }
 
        // Se estava em manutenção, voltar a activo
        if (e.getStatus() == EquipmentStatus.MAINTENANCE
            && req.getType() == MaintenanceRecord.MaintenanceType.PREVENTIVE) {
            e.setStatus(EquipmentStatus.ACTIVE);
        }
 
        equipmentRepository.save(e);
        log.info("Maintenance {} recorded for equipment {}", req.getType(), e.getCode());
        return toResponse(e, true);
    }
 
    // Job diário para alertas de manutenção
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional(readOnly = true)
    public void checkMaintenanceAlerts() {
        log.info("Checking equipment maintenance alerts...");
        hospitalRepository.findAll().forEach(h -> {
            long due = equipmentRepository
                .findMaintenanceDue(h.getId(), LocalDate.now()).size();
            if (due > 0) log.warn("Hospital {}: {} equipment need maintenance", h.getName(), due);
        });
    }
 
    private Equipment getOrThrow(UUID id) {
        return equipmentRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Equipamento não encontrado"));
    }
 
    private ao.hospitalao.modules.auth.entity.User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }
 
    private EquipmentResponse toResponse(Equipment e, boolean withHistory) {
        List<MaintenanceResponse> history = withHistory
            ? maintenanceRepository.findByEquipmentIdOrderByPerformedAtDesc(e.getId())
                .stream().map(m -> MaintenanceResponse.builder()
                    .id(m.getId())
                    .type(m.getType())
                    .typeLabel(typeLabel(m.getType()))
                    .performedByName(m.getPerformedBy() != null ?
                        m.getPerformedBy().getFullName() : "Externo")
                    .performedAt(m.getPerformedAt())
                    .description(m.getDescription())
                    .cost(m.getCost())
                    .nextMaintenanceDate(m.getNextMaintenanceDate())
                    .partsReplaced(m.getPartsReplaced())
                    .result(m.getResult())
                    .build()).toList()
            : List.of();
 
        return EquipmentResponse.builder()
            .id(e.getId()).name(e.getName()).code(e.getCode())
            .brand(e.getBrand()).model(e.getModel()).serialNumber(e.getSerialNumber())
            .category(e.getCategory()).categoryLabel(categoryLabel(e.getCategory()))
            .location(e.getLocation())
            .wardName(e.getWard() != null ? e.getWard().getName() : null)
            .status(e.getStatus()).statusLabel(statusLabel(e.getStatus()))
            .purchaseDate(e.getPurchaseDate()).purchasePrice(e.getPurchasePrice())
            .warrantyExpiry(e.getWarrantyExpiry()).warrantyExpired(e.isWarrantyExpired())
            .nextMaintenanceDate(e.getNextMaintenanceDate())
            .maintenanceDue(e.isMaintenanceDue())
            .nextCalibrationDate(e.getNextCalibrationDate())
            .maintenanceIntervalDays(e.getMaintenanceIntervalDays())
            .notes(e.getNotes())
            .maintenanceHistory(history)
            .createdAt(e.getCreatedAt())
            .build();
    }
 
    private String statusLabel(EquipmentStatus s) {
        return switch (s) {
            case ACTIVE      -> "Activo";
            case MAINTENANCE -> "Em Manutenção";
            case REPAIR      -> "Em Reparação";
            case RETIRED     -> "Abatido";
            case RESERVED    -> "Reservado";
        };
    }
 
    private String categoryLabel(Equipment.EquipmentCategory c) {
        return switch (c) {
            case DIAGNOSTIC  -> "Diagnóstico";
            case THERAPEUTIC -> "Terapêutico";
            case SURGICAL    -> "Cirúrgico";
            case MONITORING  -> "Monitorização";
            case MOBILITY    -> "Mobilidade";
            case IT          -> "Informático";
            case OTHER       -> "Outro";
        };
    }
 
    private String typeLabel(MaintenanceRecord.MaintenanceType t) {
        return switch (t) {
            case PREVENTIVE  -> "Preventiva";
            case CORRECTIVE  -> "Correctiva";
            case CALIBRATION -> "Calibração";
            case INSPECTION  -> "Inspecção";
        };
    }
}