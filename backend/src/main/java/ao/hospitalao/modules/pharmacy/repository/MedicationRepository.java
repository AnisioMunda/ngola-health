package ao.hospitalao.modules.pharmacy.repository;

import ao.hospitalao.modules.pharmacy.entity.Medication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, UUID> {

    Page<Medication> findByHospitalIdAndActiveTrue(UUID hospitalId, Pageable pageable);

    @Query("""
        SELECT m FROM Medication m
        WHERE m.hospital.id = :hospitalId AND m.active = true
        AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(m.genericName) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    Page<Medication> search(
        @Param("hospitalId") UUID hospitalId,
        @Param("search") String search,
        Pageable pageable
    );

    List<Medication> findByHospitalIdAndActiveTrue(UUID hospitalId);
}