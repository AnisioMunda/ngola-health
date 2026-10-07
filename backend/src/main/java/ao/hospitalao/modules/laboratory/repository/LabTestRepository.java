package ao.hospitalao.modules.laboratory.repository;

import ao.hospitalao.modules.laboratory.entity.LabTest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabTestRepository extends JpaRepository<LabTest, UUID> {

  List<LabTest> findByHospitalIdAndActiveTrue(UUID hospitalId);

  Page<LabTest> findByHospitalIdAndActiveTrue(UUID hospitalId, Pageable pageable);

  boolean existsByHospitalIdAndCode(UUID hospitalId, String code);
}
