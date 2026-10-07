package ao.hospitalao.modules.audit.repository;

import ao.hospitalao.modules.audit.entity.AuditLog;
import ao.hospitalao.modules.audit.entity.AuditLog.AuditAction;
import ao.hospitalao.modules.audit.entity.AuditLog.EntityType;
import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public class AuditSpecification {

  public static Specification<AuditLog> withFilters(
      UUID hospitalId,
      UUID userId,
      AuditAction action,
      EntityType entityType,
      OffsetDateTime from,
      OffsetDateTime to) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (hospitalId != null) {
        predicates.add(cb.equal(root.get("hospital").get("id"), hospitalId));
      }
      if (userId != null) {
        predicates.add(cb.equal(root.get("user").get("id"), userId));
      }
      if (action != null) {
        predicates.add(cb.equal(root.get("action"), action));
      }
      if (entityType != null) {
        predicates.add(cb.equal(root.get("entityType"), entityType));
      }
      if (from != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
      }
      if (to != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
      }

      query.orderBy(cb.desc(root.get("createdAt")));

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
