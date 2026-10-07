package ao.hospitalao.modules.hospitals.repository;

import ao.hospitalao.modules.hospitals.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    List<Hospital> findByActiveTrue();

    Optional<Hospital> findByCode(String code);

    boolean existsByCode(String code);
}