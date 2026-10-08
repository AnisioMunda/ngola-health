package ao.hospitalao.modules.financial.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class FinancialAmounts {

  private static final int CURRENCY_SCALE = 2;
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  private FinancialAmounts() {}

  public static BigDecimal round(BigDecimal amount) {
    return Objects.requireNonNull(amount, "amount").setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
  }

  public static BigDecimal percentage(BigDecimal amount, BigDecimal percentage) {
    return round(
        Objects.requireNonNull(amount, "amount")
            .multiply(Objects.requireNonNull(percentage, "percentage"))
            .divide(ONE_HUNDRED, CURRENCY_SCALE, RoundingMode.HALF_UP));
  }
}
