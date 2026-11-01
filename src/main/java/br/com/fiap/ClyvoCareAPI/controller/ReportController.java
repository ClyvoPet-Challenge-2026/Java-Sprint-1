package br.com.fiap.ClyvoCareAPI.controller;

import br.com.fiap.ClyvoCareAPI.dto.RevenueReportResponse;
import br.com.fiap.ClyvoCareAPI.dto.SubscriptionReportResponse;
import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import br.com.fiap.ClyvoCareAPI.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/relatorios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Relatórios administrativos executados pelos packages Oracle")
public class ReportController {
    private final ReportService reports;

    @GetMapping("/contratacoes")
    @Operation(summary = "Consulta contratações via PKG_RELATORIOS.REPORT_SUBSCRIPTIONS")
    public SubscriptionReportResponse subscriptions(@RequestParam(required = false) SubscriptionStatus status) {
        return reports.subscriptions(status);
    }

    @GetMapping("/receita")
    @Operation(summary = "Consulta receita, subtotais por plano e total geral via PKG_RELATORIOS.REPORT_REVENUE")
    public RevenueReportResponse revenue(@RequestParam(defaultValue = "0")
                                        @DecimalMin("0") @Digits(integer = 8, fraction = 2) BigDecimal minValue) {
        return reports.revenue(minValue);
    }
}
