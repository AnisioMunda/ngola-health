package ao.hospitalao.modules.equipment.repository;

import ao.hospitalao.modules.equipment.entity.MaintenanceRecord;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, UUID> {
  List<MaintenanceRecord> findByEquipmentIdOrderByPerformedAtDesc(UUID equipmentId);
}
