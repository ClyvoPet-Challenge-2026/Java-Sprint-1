CREATE OR REPLACE PACKAGE PKG_AUDITORIA AS
    PROCEDURE LOG_ERROR(
        p_origin  IN VARCHAR2,
        p_code    IN NUMBER,
        p_message IN VARCHAR2
    );

    PROCEDURE AUDIT_SUBSCRIPTION(
        p_operation IN VARCHAR2,
        p_old       IN TB_CAD_SUBSCRIPTION%ROWTYPE,
        p_new       IN TB_CAD_SUBSCRIPTION%ROWTYPE
    );
END PKG_AUDITORIA;
/

CREATE OR REPLACE PACKAGE BODY PKG_AUDITORIA AS

    FUNCTION FORMAT_SUBSCRIPTION(p_row IN TB_CAD_SUBSCRIPTION%ROWTYPE)
        RETURN VARCHAR2
    IS
    BEGIN
        RETURN 'ID='                || p_row.ID
            || ';PET_ID='           || p_row.PET_ID
            || ';PLAN_ID='          || p_row.PLAN_ID
            || ';STATUS='           || p_row.STATUS
            || ';PAYMENT_METHOD='   || p_row.PAYMENT_METHOD
            || ';CONTRACTED_VALUE=' || TO_CHAR(p_row.CONTRACTED_VALUE, 'FM999990.00')
            || ';START_DATE='       || TO_CHAR(p_row.START_DATE, 'YYYY-MM-DD');
    END FORMAT_SUBSCRIPTION;

    PROCEDURE LOG_ERROR(
        p_origin  IN VARCHAR2,
        p_code    IN NUMBER,
        p_message IN VARCHAR2
    ) IS
        PRAGMA AUTONOMOUS_TRANSACTION;
        e_origin_null EXCEPTION;
        e_too_large   EXCEPTION;
        PRAGMA EXCEPTION_INIT(e_too_large, -12899);
    BEGIN
        IF p_origin IS NULL THEN
            RAISE e_origin_null;
        END IF;

        INSERT INTO TB_HEA_ERROR_LOG (PROCEDURE_NAME, USER_NAME, ERROR_CODE, ERROR_MESSAGE)
        VALUES (p_origin, USER, TO_CHAR(p_code), p_message);
        COMMIT;

    EXCEPTION
        WHEN e_origin_null THEN
            LOG_ERROR('ORIGEM NAO INFORMADA', p_code, p_message);
        WHEN e_too_large THEN
            LOG_ERROR(SUBSTRB(p_origin, 1, 100), p_code, SUBSTRB(p_message, 1, 2000));
        WHEN OTHERS THEN
            ROLLBACK;
    END LOG_ERROR;

    PROCEDURE AUDIT_SUBSCRIPTION(
        p_operation IN VARCHAR2,
        p_old       IN TB_CAD_SUBSCRIPTION%ROWTYPE,
        p_new       IN TB_CAD_SUBSCRIPTION%ROWTYPE
    ) IS
        e_invalid_operation EXCEPTION;
        e_missing_row       EXCEPTION;
        v_old  VARCHAR2(4000);
        v_new  VARCHAR2(4000);
        v_code NUMBER;
        v_msg  VARCHAR2(2000);
    BEGIN
        IF p_operation IS NULL OR p_operation NOT IN ('INSERT','UPDATE','DELETE') THEN
            RAISE e_invalid_operation;
        END IF;
        IF (p_operation IN ('INSERT','UPDATE') AND p_new.ID IS NULL)
        OR (p_operation IN ('UPDATE','DELETE') AND p_old.ID IS NULL) THEN
            RAISE e_missing_row;
        END IF;

        IF p_operation IN ('INSERT','UPDATE') THEN
            v_new := SUBSTRB(FORMAT_SUBSCRIPTION(p_new), 1, 4000);
        END IF;
        IF p_operation IN ('UPDATE','DELETE') THEN
            v_old := SUBSTRB(FORMAT_SUBSCRIPTION(p_old), 1, 4000);
        END IF;

        INSERT INTO TB_HEA_AUDIT_LOG (TABLE_NAME, OPERATION, USER_NAME, ROW_PK, OLD_VALUES, NEW_VALUES)
        VALUES ('TB_CAD_SUBSCRIPTION', p_operation, USER, NVL(p_new.ID, p_old.ID), v_old, v_new);

    EXCEPTION
        WHEN e_invalid_operation THEN
            v_msg := 'Operacao de auditoria invalida: ' || NVL(p_operation, 'NULL');
            LOG_ERROR('PKG_AUDITORIA.AUDIT_SUBSCRIPTION', -20090, v_msg);
            RAISE_APPLICATION_ERROR(-20090, v_msg);
        WHEN e_missing_row THEN
            v_msg := 'Linha ausente para a operacao de auditoria ' || p_operation;
            LOG_ERROR('PKG_AUDITORIA.AUDIT_SUBSCRIPTION', -20090, v_msg);
            RAISE_APPLICATION_ERROR(-20090, v_msg);
        WHEN OTHERS THEN
            v_code := SQLCODE;
            v_msg  := SUBSTRB('Falha ao auditar TB_CAD_SUBSCRIPTION: ' || SQLERRM, 1, 2000);
            LOG_ERROR('PKG_AUDITORIA.AUDIT_SUBSCRIPTION', v_code, v_msg);
            RAISE_APPLICATION_ERROR(-20090, v_msg);
    END AUDIT_SUBSCRIPTION;

END PKG_AUDITORIA;
/

CREATE OR REPLACE PACKAGE PKG_JSON AS
    FUNCTION ESCAPE_TEXT(p_text IN VARCHAR2) RETURN VARCHAR2;

    FUNCTION NUMBER_TO_JSON(p_value IN NUMBER) RETURN VARCHAR2;

    FUNCTION SUBSCRIPTION_TO_JSON(p_subscription_id IN NUMBER) RETURN CLOB;
END PKG_JSON;
/

CREATE OR REPLACE PACKAGE BODY PKG_JSON AS

    c_max_escape_bytes CONSTANT PLS_INTEGER := 16000;
    c_truncate_chars   CONSTANT PLS_INTEGER := 4000;

    FUNCTION ESCAPE_TEXT(p_text IN VARCHAR2)
        RETURN VARCHAR2
    IS
        e_text_too_long EXCEPTION;
        v_out VARCHAR2(32767);
    BEGIN
        IF p_text IS NULL THEN
            RETURN '';
        END IF;
        IF LENGTHB(p_text) > c_max_escape_bytes THEN
            RAISE e_text_too_long;
        END IF;

        v_out := p_text;
        v_out := REPLACE(v_out, '\', '\\');
        v_out := REPLACE(v_out, '"', '\"');
        v_out := REPLACE(v_out, CHR(13), '\r');
        v_out := REPLACE(v_out, CHR(10), '\n');
        v_out := REPLACE(v_out, CHR(9),  '\t');
        RETURN v_out;

    EXCEPTION
        WHEN e_text_too_long THEN
            PKG_AUDITORIA.LOG_ERROR('PKG_JSON.ESCAPE_TEXT', -20011,
                'Texto truncado em ' || c_truncate_chars || ' caracteres para o JSON');
            RETURN ESCAPE_TEXT(SUBSTR(p_text, 1, c_truncate_chars));
        WHEN VALUE_ERROR THEN
            RETURN ESCAPE_TEXT(SUBSTR(p_text, 1, c_truncate_chars));
        WHEN OTHERS THEN
            PKG_AUDITORIA.LOG_ERROR('PKG_JSON.ESCAPE_TEXT', SQLCODE, SQLERRM);
            RETURN '';
    END ESCAPE_TEXT;

    FUNCTION NUMBER_TO_JSON(p_value IN NUMBER)
        RETURN VARCHAR2
    IS
        e_out_of_range EXCEPTION;
    BEGIN
        IF p_value IS NULL THEN
            RETURN 'null';
        END IF;
        IF ABS(p_value) >= 1E11 THEN
            RAISE e_out_of_range;
        END IF;

        RETURN REPLACE(TRIM(TO_CHAR(p_value, 'FM99999999990.00')), ',', '.');

    EXCEPTION
        WHEN e_out_of_range THEN
            PKG_AUDITORIA.LOG_ERROR('PKG_JSON.NUMBER_TO_JSON', -20012,
                'Numero fora da faixa suportada pelo JSON: ' || TO_CHAR(p_value));
            RETURN 'null';
        WHEN VALUE_ERROR THEN
            PKG_AUDITORIA.LOG_ERROR('PKG_JSON.NUMBER_TO_JSON', SQLCODE, SQLERRM);
            RETURN 'null';
        WHEN OTHERS THEN
            PKG_AUDITORIA.LOG_ERROR('PKG_JSON.NUMBER_TO_JSON', SQLCODE, SQLERRM);
            RETURN 'null';
    END NUMBER_TO_JSON;

    FUNCTION SUBSCRIPTION_TO_JSON(p_subscription_id IN NUMBER)
        RETURN CLOB
    IS
        e_null_id EXCEPTION;
        v_json    CLOB;
    BEGIN
        IF p_subscription_id IS NULL THEN
            RAISE e_null_id;
        END IF;

        SELECT  '{'
             || '"subscription_id":' || s.ID || ','
             || '"start_date":"'      || TO_CHAR(s.START_DATE,'YYYY-MM-DD') || '",'
             || '"contracted_value":' || NUMBER_TO_JSON(s.CONTRACTED_VALUE) || ','
             || '"pet":{'
                  || '"id":'       || p.ID || ','
                  || '"name":"'    || ESCAPE_TEXT(p.NAME)   || '",'
                  || '"species":"' || ESCAPE_TEXT(esp.NAME) || '"'
                || '},'
             || '"owner":{'
                  || '"id":'    || o.ID || ','
                  || '"name":"' || ESCAPE_TEXT(o.NAME) || '",'
                  || '"cpf":"'  || ESCAPE_TEXT(o.CPF)  || '",'
                  || '"city":"' || ESCAPE_TEXT(c.NAME) || '/' || uf.UF || '"'
                || '},'
             || '"plan":{'
                  || '"name":"'         || ESCAPE_TEXT(pl.NAME) || '",'
                  || '"monthly_value":' || NUMBER_TO_JSON(pl.MONTHLY_VALUE)
                || '},'
             || '"status":"'         || ESCAPE_TEXT(s.STATUS) || '",'
             || '"payment_method":"' || ESCAPE_TEXT(s.PAYMENT_METHOD) || '"'
             || '}'
          INTO v_json
          FROM TB_CAD_SUBSCRIPTION    s
          JOIN TB_CAD_PET             p   ON p.ID   = s.PET_ID
          JOIN TB_CAD_SPECIES        esp  ON esp.ID = p.SPECIES_ID
          JOIN TB_CAD_OWNER           o   ON o.ID   = p.OWNER_ID
          JOIN TB_CAD_CITY            c   ON c.ID   = o.CITY_ID
          JOIN TB_CAD_STATE          uf   ON uf.ID  = c.STATE_ID
          JOIN TB_CAD_PLAN           pl   ON pl.ID  = s.PLAN_ID
         WHERE s.ID = p_subscription_id;

        RETURN v_json;

    EXCEPTION
        WHEN e_null_id THEN
            PKG_AUDITORIA.LOG_ERROR('PKG_JSON.SUBSCRIPTION_TO_JSON', -20013,
                'ID da assinatura nao informado');
            RETURN '{"erro":"ID da assinatura nao informado"}';
        WHEN NO_DATA_FOUND THEN
            PKG_AUDITORIA.LOG_ERROR('PKG_JSON.SUBSCRIPTION_TO_JSON', 100,
                'Assinatura ' || p_subscription_id || ' nao encontrada');
            RETURN '{"erro":"Assinatura ' || p_subscription_id || ' nao encontrada"}';
        WHEN OTHERS THEN
            PKG_AUDITORIA.LOG_ERROR('PKG_JSON.SUBSCRIPTION_TO_JSON', SQLCODE, SQLERRM);
            RETURN '{"erro":"Falha inesperada: ' || ESCAPE_TEXT(SQLERRM) || '"}';
    END SUBSCRIPTION_TO_JSON;

END PKG_JSON;
/

CREATE OR REPLACE PACKAGE PKG_FINANCEIRO AS
    FUNCTION CALCULATE_CONTRACT_VALUE(
        p_plan_id        IN NUMBER,
        p_payment_method IN VARCHAR2
    ) RETURN NUMBER;

    FUNCTION TOTAL_REVENUE(
        p_status    IN VARCHAR2 DEFAULT NULL,
        p_min_value IN NUMBER   DEFAULT 0
    ) RETURN NUMBER;
END PKG_FINANCEIRO;
/

CREATE OR REPLACE PACKAGE BODY PKG_FINANCEIRO AS

    c_pix_discount   CONSTANT NUMBER := 0.05;
    c_debit_discount CONSTANT NUMBER := 0.03;

    FUNCTION CALCULATE_CONTRACT_VALUE(
        p_plan_id        IN NUMBER,
        p_payment_method IN VARCHAR2
    ) RETURN NUMBER
    IS
        e_null_param     EXCEPTION;
        e_invalid_method EXCEPTION;
        v_base_value TB_CAD_PLAN.MONTHLY_VALUE%TYPE;
        v_discount   NUMBER := 0;
        v_code       NUMBER;
        v_msg        VARCHAR2(2000);
    BEGIN
        IF p_plan_id IS NULL OR p_payment_method IS NULL THEN
            RAISE e_null_param;
        END IF;

        SELECT MONTHLY_VALUE INTO v_base_value
          FROM TB_CAD_PLAN WHERE ID = p_plan_id;

        IF p_payment_method NOT IN ('CREDIT_CARD','DEBIT_CARD','BOLETO','PIX') THEN
            RAISE e_invalid_method;
        END IF;

        v_discount := CASE p_payment_method
                        WHEN 'PIX'        THEN c_pix_discount
                        WHEN 'DEBIT_CARD' THEN c_debit_discount
                        ELSE 0
                      END;

        RETURN ROUND(v_base_value * (1 - v_discount), 2);

    EXCEPTION
        WHEN e_null_param THEN
            v_msg := 'Plano e metodo de pagamento sao obrigatorios.';
            PKG_AUDITORIA.LOG_ERROR('PKG_FINANCEIRO.CALCULATE_CONTRACT_VALUE', -20021, v_msg);
            RAISE_APPLICATION_ERROR(-20021, v_msg);
        WHEN NO_DATA_FOUND THEN
            v_msg := 'Plano inexistente: ' || p_plan_id;
            PKG_AUDITORIA.LOG_ERROR('PKG_FINANCEIRO.CALCULATE_CONTRACT_VALUE', -20022, v_msg);
            RAISE_APPLICATION_ERROR(-20022, v_msg);
        WHEN e_invalid_method THEN
            v_msg := 'Metodo de pagamento invalido: ' || p_payment_method;
            PKG_AUDITORIA.LOG_ERROR('PKG_FINANCEIRO.CALCULATE_CONTRACT_VALUE', -20023, v_msg);
            RAISE_APPLICATION_ERROR(-20023, v_msg);
        WHEN OTHERS THEN
            v_code := SQLCODE;
            v_msg  := SUBSTRB('Falha ao calcular valor do contrato: ' || SQLERRM, 1, 2000);
            PKG_AUDITORIA.LOG_ERROR('PKG_FINANCEIRO.CALCULATE_CONTRACT_VALUE', v_code, v_msg);
            RAISE_APPLICATION_ERROR(-20029, v_msg);
    END CALCULATE_CONTRACT_VALUE;

    FUNCTION TOTAL_REVENUE(
        p_status    IN VARCHAR2 DEFAULT NULL,
        p_min_value IN NUMBER   DEFAULT 0
    ) RETURN NUMBER
    IS
        e_invalid_status EXCEPTION;
        v_total NUMBER;
        v_code  NUMBER;
        v_msg   VARCHAR2(2000);
    BEGIN
        IF NVL(p_min_value, 0) < 0 THEN
            RAISE VALUE_ERROR;
        END IF;
        IF p_status IS NOT NULL AND p_status NOT IN ('ACTIVE','INACTIVE','PENDING') THEN
            RAISE e_invalid_status;
        END IF;

        SELECT NVL(SUM(CONTRACTED_VALUE), 0) INTO v_total
          FROM TB_CAD_SUBSCRIPTION
         WHERE CONTRACTED_VALUE >= NVL(p_min_value, 0)
           AND (p_status IS NULL OR STATUS = p_status);

        RETURN v_total;

    EXCEPTION
        WHEN VALUE_ERROR THEN
            v_msg := 'Valor minimo invalido: ' || p_min_value;
            PKG_AUDITORIA.LOG_ERROR('PKG_FINANCEIRO.TOTAL_REVENUE', -20042, v_msg);
            RAISE_APPLICATION_ERROR(-20042, v_msg);
        WHEN e_invalid_status THEN
            v_msg := 'Status informado invalido: ' || p_status;
            PKG_AUDITORIA.LOG_ERROR('PKG_FINANCEIRO.TOTAL_REVENUE', -20043, v_msg);
            RAISE_APPLICATION_ERROR(-20043, v_msg);
        WHEN OTHERS THEN
            v_code := SQLCODE;
            v_msg  := SUBSTRB('Falha ao calcular receita total: ' || SQLERRM, 1, 2000);
            PKG_AUDITORIA.LOG_ERROR('PKG_FINANCEIRO.TOTAL_REVENUE', v_code, v_msg);
            RAISE_APPLICATION_ERROR(-20049, v_msg);
    END TOTAL_REVENUE;

END PKG_FINANCEIRO;
/

CREATE OR REPLACE PACKAGE PKG_RELATORIOS AS
    PROCEDURE REPORT_SUBSCRIPTIONS(
        p_status IN  VARCHAR2 DEFAULT NULL,
        p_json   OUT CLOB,
        p_total  OUT NUMBER
    );

    PROCEDURE REPORT_REVENUE(
        p_min_value   IN  NUMBER DEFAULT 0,
        p_result      OUT SYS_REFCURSOR,
        p_grand_total OUT NUMBER
    );
END PKG_RELATORIOS;
/

CREATE OR REPLACE PACKAGE BODY PKG_RELATORIOS AS

    PROCEDURE FREE_CLOB(p_lob IN OUT CLOB) IS
    BEGIN
        IF p_lob IS NOT NULL AND DBMS_LOB.ISTEMPORARY(p_lob) = 1 THEN
            DBMS_LOB.FREETEMPORARY(p_lob);
        END IF;
        p_lob := NULL;
    END FREE_CLOB;

    PROCEDURE REPORT_SUBSCRIPTIONS(
        p_status IN  VARCHAR2 DEFAULT NULL,
        p_json   OUT CLOB,
        p_total  OUT NUMBER
    ) IS
        e_invalid_status   EXCEPTION;
        e_no_subscriptions EXCEPTION;
        v_row  CLOB;
        v_code NUMBER;
        v_msg  VARCHAR2(2000);
    BEGIN
        p_total := 0;
        IF p_status IS NOT NULL AND p_status NOT IN ('ACTIVE','INACTIVE','PENDING') THEN
            RAISE e_invalid_status;
        END IF;

        DBMS_LOB.CREATETEMPORARY(p_json, TRUE);
        DBMS_LOB.WRITEAPPEND(p_json, 1, '[');

        FOR r IN (
            SELECT s.ID
              FROM TB_CAD_SUBSCRIPTION s
             WHERE p_status IS NULL OR s.STATUS = p_status
             ORDER BY s.ID
        ) LOOP
            IF p_total > 0 THEN
                DBMS_LOB.WRITEAPPEND(p_json, 1, ',');
            END IF;
            v_row := PKG_JSON.SUBSCRIPTION_TO_JSON(r.ID);
            DBMS_LOB.APPEND(p_json, v_row);
            p_total := p_total + 1;
        END LOOP;

        DBMS_LOB.WRITEAPPEND(p_json, 1, ']');

        IF p_total = 0 THEN
            RAISE e_no_subscriptions;
        END IF;

    EXCEPTION
        WHEN e_invalid_status THEN
            v_msg := 'Status informado invalido: ' || p_status;
            PKG_AUDITORIA.LOG_ERROR('PKG_RELATORIOS.REPORT_SUBSCRIPTIONS', -20030, v_msg);
            RAISE_APPLICATION_ERROR(-20030, v_msg);
        WHEN e_no_subscriptions THEN
            FREE_CLOB(p_json);
            v_msg := 'Nenhuma assinatura para o filtro: status=' || NVL(p_status, 'TODOS');
            PKG_AUDITORIA.LOG_ERROR('PKG_RELATORIOS.REPORT_SUBSCRIPTIONS', -20031, v_msg);
            RAISE_APPLICATION_ERROR(-20031, v_msg);
        WHEN OTHERS THEN
            v_code := SQLCODE;
            v_msg  := SUBSTRB('Falha ao gerar relatorio de assinaturas: ' || SQLERRM, 1, 2000);
            FREE_CLOB(p_json);
            PKG_AUDITORIA.LOG_ERROR('PKG_RELATORIOS.REPORT_SUBSCRIPTIONS', v_code, v_msg);
            RAISE_APPLICATION_ERROR(-20039, v_msg);
    END REPORT_SUBSCRIPTIONS;

    PROCEDURE REPORT_REVENUE(
        p_min_value   IN  NUMBER DEFAULT 0,
        p_result      OUT SYS_REFCURSOR,
        p_grand_total OUT NUMBER
    ) IS
        e_empty_fact        EXCEPTION;
        e_invalid_min_value EXCEPTION;
        PRAGMA EXCEPTION_INIT(e_invalid_min_value, -20042);
        v_min  NUMBER := NVL(p_min_value, 0);
        v_code NUMBER;
        v_msg  VARCHAR2(2000);
    BEGIN
        p_grand_total := PKG_FINANCEIRO.TOTAL_REVENUE(NULL, v_min);

        IF p_grand_total = 0 THEN
            RAISE e_empty_fact;
        END IF;

        OPEN p_result FOR
            WITH base AS (
                SELECT pl.NAME AS plan_name,
                       s.STATUS AS status_name,
                       COUNT(*) AS qty,
                       SUM(s.CONTRACTED_VALUE) AS amount
                  FROM TB_CAD_SUBSCRIPTION s
                  JOIN TB_CAD_PLAN        pl ON pl.ID = s.PLAN_ID
                 WHERE s.CONTRACTED_VALUE >= v_min
                 GROUP BY pl.NAME, s.STATUS
            )
            SELECT line_type, plan_name, status_name, qty, amount
              FROM (
                    SELECT 'DETAIL' AS line_type, plan_name, status_name, qty, amount, 0 AS sort_key
                      FROM base
                    UNION ALL
                    SELECT 'SUBTOTAL', plan_name, NULL, SUM(qty), SUM(amount), 1
                      FROM base
                     GROUP BY plan_name
                   )
             ORDER BY plan_name, sort_key, status_name;

    EXCEPTION
        WHEN e_invalid_min_value THEN
            RAISE;
        WHEN e_empty_fact THEN
            v_msg := 'Tabela de fatos vazia para o filtro: ' || v_min;
            PKG_AUDITORIA.LOG_ERROR('PKG_RELATORIOS.REPORT_REVENUE', -20041, v_msg);
            RAISE_APPLICATION_ERROR(-20041, v_msg);
        WHEN OTHERS THEN
            v_code := SQLCODE;
            v_msg  := SUBSTRB('Falha ao gerar fato de receita: ' || SQLERRM, 1, 2000);
            IF p_result%ISOPEN THEN
                CLOSE p_result;
            END IF;
            PKG_AUDITORIA.LOG_ERROR('PKG_RELATORIOS.REPORT_REVENUE', v_code, v_msg);
            RAISE_APPLICATION_ERROR(-20045, v_msg);
    END REPORT_REVENUE;

END PKG_RELATORIOS;
/

CREATE OR REPLACE TRIGGER TRG_SUBSCRIPTION_AUDIT
    AFTER INSERT OR UPDATE OR DELETE ON TB_CAD_SUBSCRIPTION
    FOR EACH ROW
DECLARE
    e_audit_failed EXCEPTION;
    PRAGMA EXCEPTION_INIT(e_audit_failed, -20090);
    v_op   VARCHAR2(10);
    v_old  TB_CAD_SUBSCRIPTION%ROWTYPE;
    v_new  TB_CAD_SUBSCRIPTION%ROWTYPE;
    v_code NUMBER;
    v_msg  VARCHAR2(2000);
BEGIN
    IF INSERTING THEN
        v_op := 'INSERT';
    ELSIF UPDATING THEN
        v_op := 'UPDATE';
    ELSE
        v_op := 'DELETE';
    END IF;

    IF INSERTING OR UPDATING THEN
        v_new.ID               := :NEW.ID;
        v_new.PET_ID           := :NEW.PET_ID;
        v_new.PLAN_ID          := :NEW.PLAN_ID;
        v_new.STATUS           := :NEW.STATUS;
        v_new.PAYMENT_METHOD   := :NEW.PAYMENT_METHOD;
        v_new.START_DATE       := :NEW.START_DATE;
        v_new.CONTRACTED_VALUE := :NEW.CONTRACTED_VALUE;
    END IF;
    IF UPDATING OR DELETING THEN
        v_old.ID               := :OLD.ID;
        v_old.PET_ID           := :OLD.PET_ID;
        v_old.PLAN_ID          := :OLD.PLAN_ID;
        v_old.STATUS           := :OLD.STATUS;
        v_old.PAYMENT_METHOD   := :OLD.PAYMENT_METHOD;
        v_old.START_DATE       := :OLD.START_DATE;
        v_old.CONTRACTED_VALUE := :OLD.CONTRACTED_VALUE;
    END IF;

    PKG_AUDITORIA.AUDIT_SUBSCRIPTION(v_op, v_old, v_new);

EXCEPTION
    WHEN e_audit_failed THEN
        RAISE;
    WHEN VALUE_ERROR THEN
        v_msg := 'Valor invalido ao montar a auditoria de TB_CAD_SUBSCRIPTION';
        PKG_AUDITORIA.LOG_ERROR('TRG_SUBSCRIPTION_AUDIT', -20090, v_msg);
        RAISE_APPLICATION_ERROR(-20090, v_msg);
    WHEN OTHERS THEN
        v_code := SQLCODE;
        v_msg  := SUBSTRB('Falha ao auditar TB_CAD_SUBSCRIPTION: ' || SQLERRM, 1, 2000);
        PKG_AUDITORIA.LOG_ERROR('TRG_SUBSCRIPTION_AUDIT', v_code, v_msg);
        RAISE_APPLICATION_ERROR(-20090, v_msg);
END TRG_SUBSCRIPTION_AUDIT;
/

BEGIN
    FOR o IN (
        SELECT OBJECT_TYPE, OBJECT_NAME FROM USER_OBJECTS
         WHERE OBJECT_TYPE IN ('PROCEDURE','FUNCTION')
           AND OBJECT_NAME IN ('SP_REPORT_SUBSCRIPTIONS_JSON','SP_REPORT_REVENUE_FACT',
                               'FN_SUBSCRIPTION_TO_JSON','FN_CALCULATE_CONTRACT_VALUE',
                               'FN_JSON_ESCAPE','FN_JSON_NUMBER')
    ) LOOP
        EXECUTE IMMEDIATE 'DROP ' || o.OBJECT_TYPE || ' ' || o.OBJECT_NAME;
    END LOOP;
END;
/

DECLARE
    v_errors  PLS_INTEGER;
    v_invalid PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_errors FROM USER_ERRORS
     WHERE ATTRIBUTE = 'ERROR'
       AND NAME IN ('PKG_AUDITORIA','PKG_JSON','PKG_FINANCEIRO','PKG_RELATORIOS',
                    'TRG_SUBSCRIPTION_AUDIT');
    SELECT COUNT(*) INTO v_invalid FROM USER_OBJECTS
     WHERE STATUS <> 'VALID'
       AND OBJECT_NAME IN ('PKG_AUDITORIA','PKG_JSON','PKG_FINANCEIRO','PKG_RELATORIOS',
                           'TRG_SUBSCRIPTION_AUDIT');
    IF v_errors > 0 OR v_invalid > 0 THEN
        RAISE_APPLICATION_ERROR(-20055, 'Falha ao compilar packages ou trigger de auditoria. Consulte USER_ERRORS.');
    END IF;
END;
/
