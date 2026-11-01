package br.com.fiap.ClyvoCareAPI.repository;

import br.com.fiap.ClyvoCareAPI.entity.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportRepositoryTests {
    @Mock private DataSource source;
    @Mock private Connection connection;
    @Mock private CallableStatement statement;
    @Mock private Clob clob;
    @Mock private ResultSet rows;
    private ReportRepository reports;

    @BeforeEach
    void configureJdbc() throws Exception {
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareCall(anyString())).thenReturn(statement);
        reports = new ReportRepository(new JdbcTemplate(source), JsonMapper.builder().build());
    }

    @Test
    void subscriptionsBindsStatusAndReadsJsonWithoutDoubleEncoding() throws Exception {
        when(statement.getClob(2)).thenReturn(clob);
        when(clob.getCharacterStream()).thenReturn(new StringReader("[{\"subscription_id\":1,\"pet\":{\"name\":\"A\\\"B\"}}]"));
        when(statement.getLong(3)).thenReturn(1L);

        var result = reports.subscriptions(SubscriptionStatus.ACTIVE);

        assertEquals(1, result.totalCount());
        assertEquals("A\"B", result.items().get(0).get("pet").get("name").asString());
        verify(connection).prepareCall("{call PKG_RELATORIOS.REPORT_SUBSCRIPTIONS(?, ?, ?)}");
        verify(statement).setString(1, "ACTIVE");
        verify(statement).registerOutParameter(2, Types.CLOB);
        verify(statement).registerOutParameter(3, Types.NUMERIC);
        verify(clob).free();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void subscriptionsWithoutMatchesReturnsAnEmptyArray() throws Exception {
        when(statement.execute()).thenThrow(new SQLException("No matching subscriptions", "72000", 20031));

        var result = reports.subscriptions(null);

        assertEquals(0, result.totalCount());
        assertTrue(result.items().isArray());
        assertEquals(0, result.items().size());
        verify(statement).setString(1, null);
        verify(statement).close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[{\"erro\":\"failed\"}]", "invalid JSON", "[1]"})
    void failedOrMalformedJsonIsRejectedAndClobIsFreed(String json) throws Exception {
        when(statement.getClob(2)).thenReturn(clob);
        when(clob.getCharacterStream()).thenReturn(new StringReader(json));

        assertThrows(RuntimeException.class, () -> reports.subscriptions(null));

        verify(clob).free();
        verify(statement).close();
    }

    @Test
    void inconsistentSubscriptionCountIsRejected() throws Exception {
        when(statement.getClob(2)).thenReturn(clob);
        when(clob.getCharacterStream()).thenReturn(new StringReader("[]"));
        when(statement.getLong(3)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> reports.subscriptions(null));
        verify(clob).free();
    }

    @Test
    void missingSubscriptionClobIsRejected() throws Exception {
        assertThrows(IllegalStateException.class, () -> reports.subscriptions(null));
        verify(statement).close();
    }

    @Test
    void revenueReadsDetailAndSubtotalWithExactDecimalAmounts() throws Exception {
        when(statement.getObject(2)).thenReturn(rows);
        when(statement.getBigDecimal(3)).thenReturn(new BigDecimal("123.45"));
        when(rows.next()).thenReturn(true, true, false);
        when(rows.getString("LINE_TYPE")).thenReturn("DETAIL", "SUBTOTAL");
        when(rows.getString("PLAN_NAME")).thenReturn("Basic");
        when(rows.getString("STATUS_NAME")).thenReturn("ACTIVE", (String) null);
        when(rows.getLong("QTY")).thenReturn(1L);
        when(rows.getBigDecimal("AMOUNT")).thenReturn(new BigDecimal("123.45"));

        var result = reports.revenue(new BigDecimal("10.50"));

        assertEquals(new BigDecimal("123.45"), result.grandTotal());
        assertEquals(2, result.items().size());
        assertEquals("SUBTOTAL", result.items().get(1).lineType());
        assertNull(result.items().get(1).status());
        assertEquals(new BigDecimal("123.45"), result.items().get(0).amount());
        verify(connection).prepareCall("{call PKG_RELATORIOS.REPORT_REVENUE(?, ?, ?)}");
        verify(statement).setBigDecimal(1, new BigDecimal("10.50"));
        verify(statement).registerOutParameter(2, Types.REF_CURSOR);
        verify(rows).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void revenueWithoutMatchesReturnsZeroAndNoRows() throws Exception {
        when(statement.execute()).thenThrow(new SQLException("No matching revenue", "72000", 20041));
        var result = reports.revenue(BigDecimal.ZERO);
        assertTrue(result.items().isEmpty());
        assertEquals(BigDecimal.ZERO, result.grandTotal());
    }

    @Test
    void missingRevenueCursorIsRejected() throws Exception {
        assertThrows(IllegalStateException.class, () -> reports.revenue(BigDecimal.ZERO));
        verify(statement).close();
    }

    @Test
    void unexpectedDatabaseErrorsArePropagatedAndResourcesAreClosed() throws Exception {
        when(statement.execute()).thenThrow(new SQLException("Package unavailable", "42000", 6550));
        assertThrows(DataAccessException.class, () -> reports.revenue(BigDecimal.ZERO));
        verify(statement).close();
        verify(connection).close();
    }
}
