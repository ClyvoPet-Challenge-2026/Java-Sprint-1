DECLARE
    v_old_status   PLS_INTEGER;
    v_new_status   PLS_INTEGER;
    v_old_payment  PLS_INTEGER;
    v_new_payment  PLS_INTEGER;
    v_count        PLS_INTEGER;
    v_status_value VARCHAR2(4000);
    v_payment_value VARCHAR2(4000);
    v_status_case  VARCHAR2(4000);
    v_payment_case VARCHAR2(4000);
BEGIN
    SELECT COUNT(*) INTO v_old_status FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TB_CAD_SUBSCRIPTION' AND COLUMN_NAME = 'STATUS_ID';
    SELECT COUNT(*) INTO v_new_status FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TB_CAD_SUBSCRIPTION' AND COLUMN_NAME = 'STATUS';
    SELECT COUNT(*) INTO v_old_payment FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TB_CAD_SUBSCRIPTION' AND COLUMN_NAME = 'PAYMENT_METHOD_ID';
    SELECT COUNT(*) INTO v_new_payment FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TB_CAD_SUBSCRIPTION' AND COLUMN_NAME = 'PAYMENT_METHOD';

    IF v_old_status = 1 THEN
        v_status_value := '(SELECT st.NAME FROM TB_CAD_SUB_STATUS st WHERE st.ID = s.STATUS_ID)';
    ELSIF v_new_status = 1 THEN
        v_status_value := 's.STATUS';
    ELSE
        RAISE_APPLICATION_ERROR(-20051, 'Coluna de status da contratacao nao encontrada.');
    END IF;
    IF v_old_payment = 1 THEN
        v_payment_value := '(SELECT pm.NAME FROM TB_CAD_PAYMENT_METHOD pm WHERE pm.ID = s.PAYMENT_METHOD_ID)';
    ELSIF v_new_payment = 1 THEN
        v_payment_value := 's.PAYMENT_METHOD';
    ELSE
        RAISE_APPLICATION_ERROR(-20052, 'Coluna de pagamento da contratacao nao encontrada.');
    END IF;

    v_status_case := 'CASE UPPER(TRIM(' || v_status_value || '))
        WHEN ''ACTIVE'' THEN ''ACTIVE''
        WHEN ''TRIAL'' THEN ''ACTIVE''
        WHEN ''CANCELED'' THEN ''INACTIVE''
        WHEN ''SUSPENDED'' THEN ''INACTIVE''
        WHEN ''INACTIVE'' THEN ''INACTIVE''
        WHEN ''OVERDUE'' THEN ''PENDING''
        WHEN ''PENDING'' THEN ''PENDING'' END';
    v_payment_case := 'CASE UPPER(TRIM(' || v_payment_value || '))
        WHEN ''CARD'' THEN ''CREDIT_CARD''
        WHEN ''CREDIT CARD'' THEN ''CREDIT_CARD''
        WHEN ''CREDIT_CARD'' THEN ''CREDIT_CARD''
        WHEN ''AUTO DEBIT'' THEN ''DEBIT_CARD''
        WHEN ''DEBIT CARD'' THEN ''DEBIT_CARD''
        WHEN ''DEBIT_CARD'' THEN ''DEBIT_CARD''
        WHEN ''BOLETO'' THEN ''BOLETO''
        WHEN ''PIX'' THEN ''PIX'' END';

    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM TB_CAD_SUBSCRIPTION s WHERE '
        || v_status_case || ' IS NULL OR ' || v_payment_case || ' IS NULL' INTO v_count;
    IF v_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20053, 'Existem contratacoes com status ou pagamento sem correspondencia nos enums.');
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TRIGGERS WHERE TRIGGER_NAME = 'TRG_SUBSCRIPTION_AUDIT';
    IF v_count = 1 THEN
        EXECUTE IMMEDIATE 'ALTER TRIGGER TRG_SUBSCRIPTION_AUDIT DISABLE';
    END IF;

    IF v_new_status = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_SUBSCRIPTION ADD (STATUS VARCHAR2(20))';
    END IF;
    IF v_new_payment = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_SUBSCRIPTION ADD (PAYMENT_METHOD VARCHAR2(20))';
    END IF;

    FOR c IN (SELECT CONSTRAINT_NAME FROM USER_CONSTRAINTS
               WHERE TABLE_NAME = 'TB_CAD_SUBSCRIPTION'
                 AND CONSTRAINT_NAME IN ('CK_SUB_STATUS','CK_SUB_PAYMENT')) LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_SUBSCRIPTION DROP CONSTRAINT ' || c.CONSTRAINT_NAME;
    END LOOP;

    EXECUTE IMMEDIATE 'UPDATE TB_CAD_SUBSCRIPTION s SET STATUS = ' || v_status_case
        || ', PAYMENT_METHOD = ' || v_payment_case;

    FOR c IN (SELECT COLUMN_NAME, NULLABLE FROM USER_TAB_COLUMNS
               WHERE TABLE_NAME = 'TB_CAD_SUBSCRIPTION'
                 AND COLUMN_NAME IN ('STATUS','PAYMENT_METHOD')) LOOP
        IF c.NULLABLE = 'Y' THEN
            EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_SUBSCRIPTION MODIFY (' || c.COLUMN_NAME || ' NOT NULL)';
        END IF;
    END LOOP;
    EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_SUBSCRIPTION MODIFY (STATUS DEFAULT ''ACTIVE'')';
    EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_SUBSCRIPTION ADD CONSTRAINT CK_SUB_STATUS
        CHECK (STATUS IN (''ACTIVE'',''INACTIVE'',''PENDING''))';
    EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_SUBSCRIPTION ADD CONSTRAINT CK_SUB_PAYMENT
        CHECK (PAYMENT_METHOD IN (''CREDIT_CARD'',''DEBIT_CARD'',''BOLETO'',''PIX''))';

    IF v_old_status = 1 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_SUBSCRIPTION DROP COLUMN STATUS_ID CASCADE CONSTRAINTS';
    END IF;
    IF v_old_payment = 1 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_SUBSCRIPTION DROP COLUMN PAYMENT_METHOD_ID CASCADE CONSTRAINTS';
    END IF;

    FOR t IN (SELECT TABLE_NAME FROM USER_TABLES
               WHERE TABLE_NAME IN ('TB_CAD_SUB_STATUS','TB_CAD_PAYMENT_METHOD')) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.TABLE_NAME;
    END LOOP;
    FOR p IN (SELECT OBJECT_NAME FROM USER_OBJECTS WHERE OBJECT_TYPE = 'PROCEDURE'
               AND OBJECT_NAME IN ('SP_INSERT_STATUS','SP_INSERT_PAYMENT_METHOD')) LOOP
        EXECUTE IMMEDIATE 'DROP PROCEDURE ' || p.OBJECT_NAME;
    END LOOP;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TB_CAD_OWNER' AND COLUMN_NAME = 'ROLE_NAME';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_OWNER ADD (ROLE_NAME VARCHAR2(20) DEFAULT ''OWNER'' NOT NULL)';
    END IF;
    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TB_CAD_OWNER' AND COLUMN_NAME = 'ENABLED';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_OWNER ADD (ENABLED CHAR(1) DEFAULT ''Y'' NOT NULL)';
    END IF;
    SELECT COUNT(*) INTO v_count FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'TB_CAD_OWNER' AND CONSTRAINT_NAME = 'CK_OWNER_ROLE';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_OWNER ADD CONSTRAINT CK_OWNER_ROLE CHECK (ROLE_NAME IN (''ADMIN'',''OWNER''))';
    END IF;
    SELECT COUNT(*) INTO v_count FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'TB_CAD_OWNER' AND CONSTRAINT_NAME = 'CK_OWNER_ENABLED';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE TB_CAD_OWNER ADD CONSTRAINT CK_OWNER_ENABLED CHECK (ENABLED IN (''Y'',''N''))';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TABLES WHERE TABLE_NAME = 'TB_HEA_AUDIT_LOG';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE q'~CREATE TABLE TB_HEA_AUDIT_LOG (
    ID              NUMBER(19) GENERATED BY DEFAULT ON NULL AS IDENTITY PRIMARY KEY,
    TABLE_NAME      VARCHAR2(30)  NOT NULL,
    OPERATION       VARCHAR2(10)  NOT NULL,
    USER_NAME       VARCHAR2(100),
    OCCURRENCE_DATE TIMESTAMP DEFAULT SYSTIMESTAMP,
    ROW_PK          NUMBER(19),
    OLD_VALUES      VARCHAR2(4000),
    NEW_VALUES      VARCHAR2(4000),
    CONSTRAINT CK_AUDIT_OP CHECK (OPERATION IN ('INSERT','UPDATE','DELETE'))
)~';
    END IF;
END;
/

CREATE OR REPLACE FUNCTION FN_JSON_ESCAPE(p_text IN VARCHAR2)
    RETURN VARCHAR2
IS
    v_out VARCHAR2(32767);
BEGIN
    IF p_text IS NULL THEN
        RETURN '';
    END IF;
    v_out := p_text;
    v_out := REPLACE(v_out, '\', '\\');
    v_out := REPLACE(v_out, '"', '\"');
    v_out := REPLACE(v_out, CHR(13), '\r');
    v_out := REPLACE(v_out, CHR(10), '\n');
    v_out := REPLACE(v_out, CHR(9),  '\t');
    RETURN v_out;
EXCEPTION
    WHEN VALUE_ERROR THEN
        RETURN SUBSTR(p_text, 1, 4000);
    WHEN OTHERS THEN
        RETURN '';
END FN_JSON_ESCAPE;
/

CREATE OR REPLACE FUNCTION FN_JSON_NUMBER(p_value IN NUMBER)
    RETURN VARCHAR2
IS
BEGIN
    IF p_value IS NULL THEN
        RETURN 'null';
    END IF;
    RETURN REPLACE(TRIM(TO_CHAR(p_value, 'FM99999999990.00')), ',', '.');
EXCEPTION
    WHEN OTHERS THEN
        RETURN 'null';
END FN_JSON_NUMBER;
/

CREATE OR REPLACE FUNCTION FN_SUBSCRIPTION_TO_JSON(p_subscription_id IN NUMBER)
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
         || '"contracted_value":' || FN_JSON_NUMBER(s.CONTRACTED_VALUE) || ','
         || '"pet":{'
              || '"id":'       || p.ID || ','
              || '"name":"'    || FN_JSON_ESCAPE(p.NAME)   || '",'
              || '"species":"' || FN_JSON_ESCAPE(esp.NAME) || '"'
            || '},'
         || '"owner":{'
              || '"id":'    || o.ID || ','
              || '"name":"' || FN_JSON_ESCAPE(o.NAME) || '",'
              || '"cpf":"'  || FN_JSON_ESCAPE(o.CPF)  || '",'
              || '"city":"' || FN_JSON_ESCAPE(c.NAME) || '/' || uf.UF || '"'
            || '},'
         || '"plan":{'
              || '"name":"'         || FN_JSON_ESCAPE(pl.NAME) || '",'
              || '"monthly_value":' || FN_JSON_NUMBER(pl.MONTHLY_VALUE)
            || '},'
         || '"status":"'         || FN_JSON_ESCAPE(s.STATUS) || '",'
         || '"payment_method":"' || FN_JSON_ESCAPE(s.PAYMENT_METHOD) || '"'
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
        RETURN '{"erro":"ID da assinatura nao informado"}';
    WHEN NO_DATA_FOUND THEN
        RETURN '{"erro":"Assinatura ' || p_subscription_id || ' nao encontrada"}';
    WHEN OTHERS THEN
        RETURN '{"erro":"Falha inesperada: ' || FN_JSON_ESCAPE(SQLERRM) || '"}';
END FN_SUBSCRIPTION_TO_JSON;
/

CREATE OR REPLACE FUNCTION FN_CALCULATE_CONTRACT_VALUE(
    p_plan_id           IN NUMBER,
    p_payment_method    IN VARCHAR2
) RETURN NUMBER
IS
    e_null_param EXCEPTION;
    e_invalid_method EXCEPTION;
    v_base_value TB_CAD_PLAN.MONTHLY_VALUE%TYPE;
    v_discount   NUMBER := 0;
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
                    WHEN 'PIX'        THEN 0.05
                    WHEN 'DEBIT_CARD' THEN 0.03
                    ELSE 0
                  END;

    RETURN ROUND(v_base_value * (1 - v_discount), 2);

EXCEPTION
    WHEN e_null_param THEN
        RAISE_APPLICATION_ERROR(-20021, 'Plano e metodo de pagamento sao obrigatorios.');
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20022, 'Plano inexistente.');
    WHEN e_invalid_method THEN
        RAISE_APPLICATION_ERROR(-20023, 'Metodo de pagamento invalido.');
    WHEN OTHERS THEN
        RAISE_APPLICATION_ERROR(-20029, 'Falha ao calcular valor do contrato: ' || SQLERRM);
END FN_CALCULATE_CONTRACT_VALUE;
/

CREATE OR REPLACE PROCEDURE SP_REPORT_SUBSCRIPTIONS_JSON(
    p_status IN VARCHAR2 DEFAULT NULL
) IS
    e_no_rows     EXCEPTION;
    v_status_name TB_CAD_SUBSCRIPTION.STATUS%TYPE := 'TODOS';
    v_count       PLS_INTEGER := 0;
    v_first       BOOLEAN := TRUE;
    v_json        CLOB;
    v_code        VARCHAR2(20);
    v_msg         VARCHAR2(2000);

    PROCEDURE print_clob(p_clob IN CLOB) IS
        v_off PLS_INTEGER := 1;
        v_amt PLS_INTEGER := 8000;
    BEGIN
        WHILE v_off <= NVL(DBMS_LOB.GETLENGTH(p_clob), 0) LOOP
            DBMS_OUTPUT.PUT_LINE(DBMS_LOB.SUBSTR(p_clob, v_amt, v_off));
            v_off := v_off + v_amt;
        END LOOP;
    END;
BEGIN
    IF p_status IS NOT NULL THEN
        IF p_status NOT IN ('ACTIVE','INACTIVE','PENDING') THEN
            RAISE NO_DATA_FOUND;
        END IF;
        v_status_name := p_status;
    END IF;

    DBMS_OUTPUT.PUT_LINE('=== RELATORIO DE ASSINATURAS EM JSON | STATUS: ' || v_status_name || ' ===');
    DBMS_OUTPUT.PUT_LINE('[');

    FOR r IN (
        SELECT s.ID
          FROM TB_CAD_SUBSCRIPTION s
          JOIN TB_CAD_PET p ON p.ID = s.PET_ID
          JOIN TB_CAD_OWNER o ON o.ID = p.OWNER_ID
         WHERE p_status IS NULL OR s.STATUS = p_status
         ORDER BY s.ID
    ) LOOP
        IF NOT v_first THEN
            DBMS_OUTPUT.PUT_LINE(',');
        END IF;
        v_json := FN_SUBSCRIPTION_TO_JSON(r.ID);
        print_clob(v_json);
        v_first := FALSE;
        v_count := v_count + 1;
    END LOOP;

    DBMS_OUTPUT.PUT_LINE(']');

    IF v_count = 0 THEN
        RAISE e_no_rows;
    END IF;

    DBMS_OUTPUT.PUT_LINE('Total de assinaturas no relatorio: ' || v_count);

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        v_code := SQLCODE;
        v_msg  := 'Status informado invalido: ' || p_status;
        DBMS_OUTPUT.PUT_LINE('ERRO: ' || v_msg);
        INSERT INTO TB_HEA_ERROR_LOG (PROCEDURE_NAME, USER_NAME, ERROR_CODE, ERROR_MESSAGE)
        VALUES ('SP_REPORT_SUBSCRIPTIONS_JSON', USER, v_code, v_msg);
        COMMIT;
    WHEN e_no_rows THEN
        v_code := '-20031';
        v_msg  := 'Nenhuma assinatura para o filtro: status=' || p_status;
        DBMS_OUTPUT.PUT_LINE('AVISO: ' || v_msg);
        INSERT INTO TB_HEA_ERROR_LOG (PROCEDURE_NAME, USER_NAME, ERROR_CODE, ERROR_MESSAGE)
        VALUES ('SP_REPORT_SUBSCRIPTIONS_JSON', USER, v_code, v_msg);
        COMMIT;
    WHEN OTHERS THEN
        v_code := SQLCODE;
        v_msg  := SUBSTR(SQLERRM, 1, 2000);
        DBMS_OUTPUT.PUT_LINE('ERRO INESPERADO: ' || v_msg);
        INSERT INTO TB_HEA_ERROR_LOG (PROCEDURE_NAME, USER_NAME, ERROR_CODE, ERROR_MESSAGE)
        VALUES ('SP_REPORT_SUBSCRIPTIONS_JSON', USER, v_code, v_msg);
        COMMIT;
END SP_REPORT_SUBSCRIPTIONS_JSON;
/

CREATE OR REPLACE PROCEDURE SP_REPORT_REVENUE_FACT(
    p_min_value IN NUMBER DEFAULT 0
) IS
    e_empty_fact EXCEPTION;
    v_prev_plan  TB_CAD_PLAN.NAME%TYPE := NULL;
    v_subtotal   NUMBER := 0;
    v_grand_total NUMBER := 0;
    v_sub_count  PLS_INTEGER := 0;
    v_total_count PLS_INTEGER := 0;
    v_rows       PLS_INTEGER := 0;
    v_code       VARCHAR2(20);
    v_msg        VARCHAR2(2000);
BEGIN
    IF p_min_value < 0 THEN
        RAISE VALUE_ERROR;
    END IF;

    DBMS_OUTPUT.PUT_LINE('===== TABELA DE FATOS: RECEITA POR PLANO x STATUS =====');
    DBMS_OUTPUT.PUT_LINE('(valor contratado minimo do filtro: ' || p_min_value || ')');
    DBMS_OUTPUT.PUT_LINE(RPAD('PLANO', 14) || RPAD('STATUS', 14) || RPAD('QTD', 6) || 'VALOR (R$)');
    DBMS_OUTPUT.PUT_LINE(RPAD('-', 52, '-'));

    FOR r IN (
        SELECT pl.NAME AS plan_name,
               s.STATUS AS status_name,
               COUNT(*)                AS qty,
               SUM(s.CONTRACTED_VALUE) AS amount
          FROM TB_CAD_SUBSCRIPTION    s
          JOIN TB_CAD_PLAN           pl ON pl.ID = s.PLAN_ID
         WHERE s.CONTRACTED_VALUE >= p_min_value
         GROUP BY pl.NAME, s.STATUS
         ORDER BY pl.NAME, s.STATUS
    ) LOOP
        IF v_prev_plan IS NOT NULL AND r.plan_name <> v_prev_plan THEN
            DBMS_OUTPUT.PUT_LINE(RPAD(' ', 20) || 'Sub Total ' || RPAD(v_prev_plan, 10) ||
                ' : R$ ' || TO_CHAR(v_subtotal, 'FM999990.00') || '  (' || v_sub_count || ' contratos)');
            DBMS_OUTPUT.PUT_LINE(' ');
            v_subtotal  := 0;
            v_sub_count := 0;
        END IF;

        DBMS_OUTPUT.PUT_LINE(RPAD(r.plan_name, 14) || RPAD(r.status_name, 14) ||
            RPAD(TO_CHAR(r.qty), 6) || 'R$ ' || TO_CHAR(r.amount, 'FM999990.00'));

        v_subtotal    := v_subtotal + r.amount;
        v_grand_total := v_grand_total + r.amount;
        v_sub_count   := v_sub_count + r.qty;
        v_total_count := v_total_count + r.qty;
        v_prev_plan   := r.plan_name;
        v_rows        := v_rows + 1;
    END LOOP;

    IF v_rows = 0 THEN
        RAISE e_empty_fact;
    END IF;

    DBMS_OUTPUT.PUT_LINE(RPAD(' ', 20) || 'Sub Total ' || RPAD(v_prev_plan, 10) ||
        ' : R$ ' || TO_CHAR(v_subtotal, 'FM999990.00') || '  (' || v_sub_count || ' contratos)');
    DBMS_OUTPUT.PUT_LINE(RPAD('=', 52, '='));
    DBMS_OUTPUT.PUT_LINE('Total Geral : R$ ' || TO_CHAR(v_grand_total, 'FM999990.00') ||
        '  (' || v_total_count || ' contratos)');

EXCEPTION
    WHEN VALUE_ERROR THEN
        v_code := SQLCODE;
        v_msg  := 'Valor minimo invalido: ' || p_min_value;
        DBMS_OUTPUT.PUT_LINE('ERRO: ' || v_msg);
        INSERT INTO TB_HEA_ERROR_LOG (PROCEDURE_NAME, USER_NAME, ERROR_CODE, ERROR_MESSAGE)
        VALUES ('SP_REPORT_REVENUE_FACT', USER, v_code, v_msg);
        COMMIT;
    WHEN e_empty_fact THEN
        v_code := '-20041';
        v_msg  := 'Tabela de fatos vazia para o filtro: ' || p_min_value;
        DBMS_OUTPUT.PUT_LINE('AVISO: ' || v_msg);
        INSERT INTO TB_HEA_ERROR_LOG (PROCEDURE_NAME, USER_NAME, ERROR_CODE, ERROR_MESSAGE)
        VALUES ('SP_REPORT_REVENUE_FACT', USER, v_code, v_msg);
        COMMIT;
    WHEN OTHERS THEN
        v_code := SQLCODE;
        v_msg  := SUBSTR(SQLERRM, 1, 2000);
        DBMS_OUTPUT.PUT_LINE('ERRO INESPERADO: ' || v_msg);
        INSERT INTO TB_HEA_ERROR_LOG (PROCEDURE_NAME, USER_NAME, ERROR_CODE, ERROR_MESSAGE)
        VALUES ('SP_REPORT_REVENUE_FACT', USER, v_code, v_msg);
        COMMIT;
END SP_REPORT_REVENUE_FACT;
/

CREATE OR REPLACE TRIGGER TRG_SUBSCRIPTION_AUDIT
    AFTER INSERT OR UPDATE OR DELETE ON TB_CAD_SUBSCRIPTION
    FOR EACH ROW
DECLARE
    v_op  VARCHAR2(10);
    v_old VARCHAR2(4000);
    v_new VARCHAR2(4000);
    v_pk  NUMBER(19);
BEGIN
    IF INSERTING THEN
        v_op := 'INSERT';
    ELSIF UPDATING THEN
        v_op := 'UPDATE';
    ELSE
        v_op := 'DELETE';
    END IF;

    IF INSERTING OR UPDATING THEN
        v_new := 'ID='                || :NEW.ID
              || ';PET_ID='           || :NEW.PET_ID
              || ';PLAN_ID='          || :NEW.PLAN_ID
              || ';STATUS='        || :NEW.STATUS
              || ';PAYMENT_METHOD='|| :NEW.PAYMENT_METHOD
              || ';CONTRACTED_VALUE=' || TO_CHAR(:NEW.CONTRACTED_VALUE, 'FM999990.00')
              || ';START_DATE='       || TO_CHAR(:NEW.START_DATE, 'YYYY-MM-DD');
        v_pk := :NEW.ID;
    END IF;

    IF UPDATING OR DELETING THEN
        v_old := 'ID='                || :OLD.ID
              || ';PET_ID='           || :OLD.PET_ID
              || ';PLAN_ID='          || :OLD.PLAN_ID
              || ';STATUS='        || :OLD.STATUS
              || ';PAYMENT_METHOD='|| :OLD.PAYMENT_METHOD
              || ';CONTRACTED_VALUE=' || TO_CHAR(:OLD.CONTRACTED_VALUE, 'FM999990.00')
              || ';START_DATE='       || TO_CHAR(:OLD.START_DATE, 'YYYY-MM-DD');
        v_pk := NVL(v_pk, :OLD.ID);
    END IF;

    INSERT INTO TB_HEA_AUDIT_LOG (TABLE_NAME, OPERATION, USER_NAME, ROW_PK, OLD_VALUES, NEW_VALUES)
    VALUES ('TB_CAD_SUBSCRIPTION', v_op, USER, v_pk, v_old, v_new);

EXCEPTION
    WHEN VALUE_ERROR THEN
        INSERT INTO TB_HEA_AUDIT_LOG (TABLE_NAME, OPERATION, USER_NAME, ROW_PK, OLD_VALUES, NEW_VALUES)
        VALUES ('TB_CAD_SUBSCRIPTION', v_op, USER, v_pk,
                SUBSTR(v_old, 1, 4000), SUBSTR(v_new, 1, 4000));
    WHEN OTHERS THEN
        RAISE_APPLICATION_ERROR(-20090, 'Falha ao auditar TB_CAD_SUBSCRIPTION: ' || SQLERRM);
END TRG_SUBSCRIPTION_AUDIT;
/

DECLARE
    v_count PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USER_OBJECTS
     WHERE OBJECT_TYPE = 'PROCEDURE' AND OBJECT_NAME = 'SP_INSERT_SUBSCRIPTION';
    IF v_count = 1 THEN
        EXECUTE IMMEDIATE q'~CREATE OR REPLACE PROCEDURE SP_INSERT_SUBSCRIPTION(
    p_pet_id            IN NUMBER,
    p_plan_id           IN NUMBER,
    p_status            IN VARCHAR2,
    p_payment_method    IN VARCHAR2,
    p_value             IN NUMBER
) IS
    v_code VARCHAR2(20);
    v_msg  VARCHAR2(2000);
BEGIN
    INSERT INTO TB_CAD_SUBSCRIPTION (PET_ID,PLAN_ID,STATUS,PAYMENT_METHOD,CONTRACTED_VALUE)
    VALUES (p_pet_id,p_plan_id,p_status,p_payment_method,p_value);
    COMMIT;
EXCEPTION
    WHEN INVALID_NUMBER THEN
        v_code := SQLCODE;
        v_msg  := 'Valor numérico inválido para assinatura do pet '||p_pet_id;
        INSERT INTO TB_HEA_ERROR_LOG (PROCEDURE_NAME,USER_NAME,ERROR_CODE,ERROR_MESSAGE)
        VALUES ('SP_INSERT_SUBSCRIPTION',USER,v_code,v_msg);
        COMMIT;
    WHEN VALUE_ERROR THEN
        v_code := SQLCODE;
        v_msg  := 'Dados inválidos para assinatura do pet '||p_pet_id;
        INSERT INTO TB_HEA_ERROR_LOG (PROCEDURE_NAME,USER_NAME,ERROR_CODE,ERROR_MESSAGE)
        VALUES ('SP_INSERT_SUBSCRIPTION',USER,v_code,v_msg);
        COMMIT;
    WHEN OTHERS THEN
        v_code := SQLCODE;
        v_msg  := SUBSTR(SQLERRM,1,2000);
        INSERT INTO TB_HEA_ERROR_LOG (PROCEDURE_NAME,USER_NAME,ERROR_CODE,ERROR_MESSAGE)
        VALUES ('SP_INSERT_SUBSCRIPTION',USER,v_code,v_msg);
        COMMIT;
END;~';
    END IF;
    FOR p IN (SELECT OBJECT_NAME FROM USER_OBJECTS WHERE OBJECT_TYPE = 'PROCEDURE'
               AND OBJECT_NAME = 'SP_INSERT_OWNER') LOOP
        EXECUTE IMMEDIATE 'ALTER PROCEDURE ' || p.OBJECT_NAME || ' COMPILE';
    END LOOP;
END;
/

DECLARE
    v_errors PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_errors FROM USER_ERRORS
     WHERE ATTRIBUTE = 'ERROR'
       AND NAME IN ('FN_JSON_ESCAPE','FN_JSON_NUMBER','FN_SUBSCRIPTION_TO_JSON',
                    'FN_CALCULATE_CONTRACT_VALUE','SP_REPORT_SUBSCRIPTIONS_JSON',
                    'SP_REPORT_REVENUE_FACT','TRG_SUBSCRIPTION_AUDIT','SP_INSERT_SUBSCRIPTION','SP_INSERT_OWNER');
    IF v_errors > 0 THEN
        RAISE_APPLICATION_ERROR(-20054, 'Falha ao recompilar objetos de contratacao. Consulte USER_ERRORS.');
    END IF;
END;
/
