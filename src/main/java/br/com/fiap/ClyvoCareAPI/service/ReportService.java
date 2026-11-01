package br.com.fiap.ClyvoCareAPI.service;

import br.com.fiap.ClyvoCareAPI.dto.RevenueReportResponse;
import br.com.fiap.ClyvoCareAPI.dto.SubscriptionReportResponse;
import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import br.com.fiap.ClyvoCareAPI.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reports;

    public SubscriptionReportResponse subscriptions(SubscriptionStatus status) {
        try {
            return reports.subscriptions(status);
        } catch (DataAccessException | IllegalStateException | JacksonException exception) {
            throw unavailable("subscriptions", exception);
        }
    }

    public RevenueReportResponse revenue(BigDecimal minValue) {
        try {
            return reports.revenue(minValue);
        } catch (DataAccessException | IllegalStateException exception) {
            throw unavailable("revenue", exception);
        }
    }

    private ResponseStatusException unavailable(String report, RuntimeException exception) {
        log.error("Cannot generate {} report; failure type: {}", report, exception.getClass().getSimpleName());
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Database report is unavailable");
    }
}
