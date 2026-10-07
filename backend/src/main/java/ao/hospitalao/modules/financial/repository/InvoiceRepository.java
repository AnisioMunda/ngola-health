package ao.hospitalao.modules.financial.repository;

import ao.hospitalao.modules.financial.entity.Invoice;
import ao.hospitalao.modules.financial.entity.Invoice.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @Query("""
        SELECT i FROM Invoice i
        WHERE i.hospital.id = :hospitalId
        AND (:patientId IS NULL OR i.patient.id = :patientId)
        AND (:status    IS NULL OR i.status      = :status)
        ORDER BY i.createdAt DESC
    """)
    Page<Invoice> findWithFilters(
        @Param("hospitalId") UUID hospitalId,
        @Param("patientId")  UUID patientId,
        @Param("status")     InvoiceStatus status,
        Pageable pageable
    );

    /**
     * Carrega Invoice com hospital, patient e items para o PDF.
     * Não incluir payments aqui — MultipleBagFetchException.
     * Os payments são carregados separadamente pelo EntityManager.
     */
    @Query("""
        SELECT i FROM Invoice i
        LEFT JOIN FETCH i.hospital
        LEFT JOIN FETCH i.patient
        LEFT JOIN FETCH i.episode
        LEFT JOIN FETCH i.items it
        LEFT JOIN FETCH it.servicePrice
        LEFT JOIN FETCH i.createdBy
        WHERE i.id = :id
    """)
    Optional<Invoice> findByIdWithItems(@Param("id") UUID id);

    /**
     * Segunda query apenas para os payments.
     * Separar as duas listas evita MultipleBagFetchException.
     */
    @Query("""
        SELECT i FROM Invoice i
        LEFT JOIN FETCH i.payments p
        LEFT JOIN FETCH p.receivedBy
        WHERE i.id = :id
    """)
    Optional<Invoice> findByIdWithPayments(@Param("id") UUID id);

    @Query("""
        SELECT i FROM Invoice i
        LEFT JOIN FETCH i.hospital
        LEFT JOIN FETCH i.patient
        WHERE i.agtStatus = 'PENDING'
        AND i.agtRequestId IS NOT NULL
        ORDER BY i.agtSubmittedAt ASC
    """)
    List<Invoice> findByAgtStatusPending(Pageable pageable);

    @Query(value = "SELECT NEXTVAL('seq_' || LOWER(CAST(:docType AS TEXT)))",
           nativeQuery = true)
    Long nextSequence(@Param("docType") String docType);

    // ------------------------------------------------
    // Dashboard avançado — queries financeiras
    // ------------------------------------------------
 
    @Query("""
        SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i
        WHERE i.hospital.id = :hospitalId
        AND i.status = 'PAGO'
        AND i.paidAt >= :from AND i.paidAt < :to
    """)
    BigDecimal sumPaidBetween(
        @Param("hospitalId") UUID hospitalId,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );
 
    @Query("""
        SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i
        WHERE i.hospital.id = :hospitalId
        AND i.status IN ('EMITIDO', 'PAGO_PARCIALMENTE', 'EM_ATRASO')
    """)
    BigDecimal sumPendingByHospital(@Param("hospitalId") UUID hospitalId);
 
    @Query("""
        SELECT COUNT(i) FROM Invoice i
        WHERE i.hospital.id = :hospitalId
        AND i.status IN ('EMITIDO', 'PAGO_PARCIALMENTE', 'EM_ATRASO')
    """)
    long countPendingByHospital(@Param("hospitalId") UUID hospitalId);
 
    @Query("""
        SELECT COUNT(i) FROM Invoice i
        WHERE i.hospital.id = :hospitalId
        AND i.issuedAt >= :from AND i.issuedAt < :to
    """)
    long countByHospitalIdAndIssuedAtBetween(
        @Param("hospitalId") UUID hospitalId,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );
 
    @Query("""
        SELECT COUNT(i) FROM Invoice i
        WHERE i.hospital.id = :hospitalId
        AND i.status = :status
        AND i.paidAt >= :from AND i.paidAt < :to
    """)
    long countByHospitalIdAndStatusAndPaidAtBetween(
        @Param("hospitalId") UUID hospitalId,
        @Param("status") InvoiceStatus status,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to
    );
 
    List<Invoice> findTop5ByHospitalIdOrderByCreatedAtDesc(UUID hospitalId);
 
    @Query(value = """
        SELECT
            TO_CHAR(DATE_TRUNC('month', i.issued_at), 'YYYY-MM')                           AS yearMonth,
            COALESCE(SUM(i.total_amount), 0)                                                AS revenue,
            COALESCE(SUM(CASE WHEN i.status = 'PAGO' THEN i.total_amount ELSE 0 END), 0)  AS paid,
            COUNT(i.id)                                                                     AS invoiceCount
        FROM invoices i
        WHERE i.hospital_id = :hospitalId
        AND i.issued_at >= :since
        GROUP BY DATE_TRUNC('month', i.issued_at)
        ORDER BY DATE_TRUNC('month', i.issued_at)
    """, nativeQuery = true)
    List<Object[]> revenueByMonthRaw(
        @Param("hospitalId") UUID hospitalId,
        @Param("since") OffsetDateTime since
    );

    // Soma de receita num período
    @Query("""
        SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i
        WHERE i.hospital.id  = :hospitalId
        AND   i.issuedAt   >= :from
        AND   i.issuedAt   <= :to
        AND   i.status NOT IN ('CANCELADO', 'ANULADO')
    """)
    BigDecimal sumRevenueByPeriod(
        @Param("hospitalId") UUID hospitalId,
        @Param("from")       LocalDate from,
        @Param("to")         LocalDate to
    );
    
    // Contar facturas num período
    @Query("""
        SELECT COUNT(i) FROM Invoice i
        WHERE i.hospital.id = :hospitalId
        AND   i.issuedAt  >= :from
        AND   i.issuedAt  <= :to
    """)
    long countByHospitalAndPeriod(
        @Param("hospitalId") UUID hospitalId,
        @Param("from")       LocalDate from,
        @Param("to")         LocalDate to
    );

    // ------------------------------------------------
    // Portal do paciente
    // ------------------------------------------------

    List<Invoice> findByPatientIdOrderByIssuedAtDesc(
        UUID patientId,
        Pageable pageable
    );
    
}