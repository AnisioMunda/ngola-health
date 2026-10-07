package ao.hospitalao.modules.episodes.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class UpdateEpisodeRequest {

  private UUID doctorId;
  private String reason;
  private String symptoms;
  private String diagnosis;
  private String prescription;
  private String notes;

  // Vitals
  private String bloodPressure;
  private Integer heartRate;
  private BigDecimal temperature;
  private BigDecimal weightKg;
}
