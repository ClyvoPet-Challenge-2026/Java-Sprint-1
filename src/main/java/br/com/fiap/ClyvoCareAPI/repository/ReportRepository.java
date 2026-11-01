package br.com.fiap.ClyvoCareAPI.repository;

import br.com.fiap.ClyvoCareAPI.dto.RevenueReportResponse;
import br.com.fiap.ClyvoCareAPI.dto.SubscriptionReportResponse;
import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReportRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public SubscriptionReportResponse subscriptions(SubscriptionStatus status) {
        return jdbc.execute((ConnectionCallback<SubscriptionReportResponse>) connection -> {
            try (var statement = connection.prepareCall("{call PKG_RELATORIOS.REPORT_SUBSCRIPTIONS(?, ?, ?)}")) {
                statement.setString(1, status == null ? null : status.name());
                statement.registerOutParameter(2, Types.CLOB);
                statement.registerOutParameter(3, Types.NUMERIC);
                statement.execute();
                var clob = statement.getClob(2);
                if (clob == null) {
                    throw new IllegalStateException("Subscription report returned no document");
                }
                try (var reader = clob.getCharacterStream()) {
                    var items = mapper.readTree(reader);
                    if (items == null || !items.isArray()) {
                        throw new IllegalStateException("Subscription report must return an array");
                    }
                    for (var item : items) {
                        if (!item.isObject() || item.has("erro")) {
                            throw new IllegalStateException("Subscription report contains a failed record");
                        }
                    }
                    long total = statement.getLong(3);
                    if (total != items.size()) {
                        throw new IllegalStateException("Subscription report count does not match its records");
                    }
                    return new SubscriptionReportResponse(items, total);
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot read subscription report", exception);
                } finally {
                    clob.free();
                }
            } catch (SQLException exception) {
                if (exception.getErrorCode() == 20031) {
                    return new SubscriptionReportResponse(mapper.createArrayNode(), 0);
                }
                throw exception;
            }
        });
    }

    public RevenueReportResponse revenue(BigDecimal minValue) {
        return jdbc.execute((ConnectionCallback<RevenueReportResponse>) connection -> {
            try (var statement = connection.prepareCall("{call PKG_RELATORIOS.REPORT_REVENUE(?, ?, ?)}")) {
                statement.setBigDecimal(1, minValue);
                statement.registerOutParameter(2, Types.REF_CURSOR);
                statement.registerOutParameter(3, Types.NUMERIC);
                statement.execute();
                var items = new ArrayList<RevenueReportResponse.Row>();
                try (var rows = (ResultSet) statement.getObject(2)) {
                    if (rows == null) {
                        throw new IllegalStateException("Revenue report returned no cursor");
                    }
                    while (rows.next()) {
                        items.add(new RevenueReportResponse.Row(rows.getString("LINE_TYPE"),
                                rows.getString("PLAN_NAME"), rows.getString("STATUS_NAME"),
                                rows.getLong("QTY"), rows.getBigDecimal("AMOUNT")));
                    }
                }
                return new RevenueReportResponse(items, statement.getBigDecimal(3));
            } catch (SQLException exception) {
                if (exception.getErrorCode() == 20041) {
                    return new RevenueReportResponse(List.of(), BigDecimal.ZERO);
                }
                throw exception;
            }
        });
    }
}
