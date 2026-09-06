package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.entity.PaymentMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class ContractPricingService {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal PIX_DISCOUNT_RATE = new BigDecimal("0.05");
    private static final BigDecimal DEBIT_CARD_DISCOUNT_RATE = new BigDecimal("0.03");
    private static final Map<PaymentMethod, BigDecimal> DISCOUNT_RATES = Map.of(
            PaymentMethod.PIX, PIX_DISCOUNT_RATE,
            PaymentMethod.DEBIT_CARD, DEBIT_CARD_DISCOUNT_RATE
    );

    public PriceCalculation calculate(BigDecimal baseValue, PaymentMethod paymentMethod) {
        validate(baseValue, paymentMethod);

        BigDecimal normalizedBaseValue = baseValue.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal discountRate = DISCOUNT_RATES.getOrDefault(
                paymentMethod,
                BigDecimal.ZERO
        );
        BigDecimal finalValue = normalizedBaseValue
                .multiply(BigDecimal.ONE.subtract(discountRate))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal discountAmount = normalizedBaseValue
                .subtract(finalValue)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return new PriceCalculation(
                normalizedBaseValue,
                discountRate,
                discountAmount,
                finalValue
        );
    }

    private void validate(BigDecimal baseValue, PaymentMethod paymentMethod) {
        if (baseValue == null) {
            throw new IllegalArgumentException("Base value is mandatory");
        }
        if (baseValue.signum() <= 0) {
            throw new IllegalArgumentException("Base value must be positive");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method is mandatory");
        }
    }

    public record PriceCalculation(
            BigDecimal baseValue,
            BigDecimal discountRate,
            BigDecimal discountAmount,
            BigDecimal finalValue
    ) {
    }
}
