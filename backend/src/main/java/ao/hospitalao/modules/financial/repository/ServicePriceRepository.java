package ao.hospitalao.modules.financial.repository;

import ao.hospitalao.modules.financial.entity.ServicePrice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServicePriceRepository extends JpaRepository<ServicePrice, UUID> {

  List<ServicePrice> findByHospitalIdAndActiveTrue(UUID hospitalId);

  Optional<ServicePrice> findByHospitalIdAndIdAndActiveTrue(UUID hospitalId, UUID id);

  Page<ServicePrice> findByHospitalIdAndActiveTrue(UUID hospitalId, Pageable pageable);

  boolean existsByHospitalIdAndCode(UUID hospitalId, String code);
}
