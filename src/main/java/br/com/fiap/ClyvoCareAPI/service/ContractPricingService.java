package br.com.fiap.ClyvoCareAPI.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

@Service
public class ContractPricingService {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal PIX_DISCOUNT_RATE = new BigDecimal("0.05");
    private static final BigDecimal AUTO_DEBIT_DISCOUNT_RATE = new BigDecimal("0.03");
    private static final Map<String, BigDecimal> DISCOUNT_RATES = Map.of(
            "PIX", PIX_DISCOUNT_RATE,
            "AUTO DEBIT", AUTO_DEBIT_DISCOUNT_RATE
    );

    public PriceCalculation calculate(BigDecimal baseValue, String paymentMethodName) {
        validate(baseValue, paymentMethodName);

        BigDecimal normalizedBaseValue = baseValue.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal discountRate = DISCOUNT_RATES.getOrDefault(
                normalizePaymentMethodName(paymentMethodName),
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

    private void validate(BigDecimal baseValue, String paymentMethodName) {
        if (baseValue == null) {
            throw new IllegalArgumentException("Base value is mandatory");
        }
        if (baseValue.signum() <= 0) {
            throw new IllegalArgumentException("Base value must be positive");
        }
        if (paymentMethodName == null || paymentMethodName.isBlank()) {
            throw new IllegalArgumentException("Payment method name is mandatory");
        }
    }

    private String normalizePaymentMethodName(String paymentMethodName) {
        return paymentMethodName.strip().toUpperCase(Locale.ROOT);
    }

    public record PriceCalculation(
            BigDecimal baseValue,
            BigDecimal discountRate,
            BigDecimal discountAmount,
            BigDecimal finalValue
    ) {
    }
}
