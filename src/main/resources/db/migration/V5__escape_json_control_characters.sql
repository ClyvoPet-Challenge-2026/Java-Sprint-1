-- Escape every JSON control character while preserving existing migration checksums.
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
        FOR control_code IN 0 .. 31 LOOP
            IF control_code NOT IN (9, 10, 13) THEN
                v_out := REPLACE(v_out, CHR(control_code), '\u00' || LOWER(TO_CHAR(control_code, 'FM0X')));
            END IF;
        END LOOP;
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

DECLARE
    v_errors PLS_INTEGER;
    v_invalid PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_errors FROM USER_ERRORS
     WHERE NAME = 'PKG_JSON' AND ATTRIBUTE = 'ERROR';
    SELECT COUNT(*) INTO v_invalid FROM USER_OBJECTS
     WHERE OBJECT_NAME = 'PKG_JSON' AND STATUS <> 'VALID';
    IF v_errors > 0 OR v_invalid > 0 THEN
        RAISE_APPLICATION_ERROR(-20055, 'PKG_JSON compilation failed. Inspect USER_ERRORS.');
    END IF;
END;
/
