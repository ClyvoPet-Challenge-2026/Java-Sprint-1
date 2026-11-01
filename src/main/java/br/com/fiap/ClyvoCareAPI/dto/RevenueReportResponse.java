package br.com.fiap.ClyvoCareAPI.dto;

import java.math.BigDecimal;
import java.util.List;

public record RevenueReportResponse(List<Row> items, BigDecimal grandTotal) {
    public record Row(String lineType, String planName, String status, long quantity, BigDecimal amount) {
    }
}
