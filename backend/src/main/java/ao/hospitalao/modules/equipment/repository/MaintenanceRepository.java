package ao.hospitalao.modules.equipment.repository;
 
import ao.hospitalao.modules.equipment.entity.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.UUID;
 
@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, UUID> {
    List<MaintenanceRecord> findByEquipmentIdOrderByPerformedAtDesc(UUID equipmentId);
}