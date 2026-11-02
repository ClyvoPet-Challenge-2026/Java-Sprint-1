SET SERVEROUTPUT ON;
SET DEFINE OFF;
WHENEVER SQLERROR EXIT FAILURE ROLLBACK;

DECLARE
    v_controls VARCHAR2(100);
    PROCEDURE ASSERT_ROUND_TRIP(p_text IN VARCHAR2) IS
        v_actual VARCHAR2(4000);
        v_escaped VARCHAR2(32767);
    BEGIN
        v_escaped := PKG_JSON.ESCAPE_TEXT(p_text);
        FOR control_code IN 0 .. 31 LOOP
            IF INSTR(v_escaped, CHR(control_code)) > 0 THEN
                RAISE_APPLICATION_ERROR(-20056, 'JSON output contains an unescaped control character.');
            END IF;
        END LOOP;
        SELECT JSON_VALUE('{"value":"' || v_escaped || '"}',
                          '$.value' RETURNING VARCHAR2(4000) ERROR ON ERROR)
          INTO v_actual FROM DUAL;
        IF UTL_RAW.CAST_TO_RAW(v_actual) <> UTL_RAW.CAST_TO_RAW(p_text)
           OR (v_actual IS NULL AND p_text IS NOT NULL) THEN
            RAISE_APPLICATION_ERROR(-20056, 'JSON escaping changed the original text.');
        END IF;
    END ASSERT_ROUND_TRIP;
BEGIN
    ASSERT_ROUND_TRIP('Quotes " and slash \');
    ASSERT_ROUND_TRIP(UNISTR('Acentua\00e7\00e3o'));
    ASSERT_ROUND_TRIP('First line' || CHR(10) || CHR(13) || CHR(9) || 'Next line');
    FOR control_code IN 0 .. 31 LOOP
        v_controls := v_controls || CHR(control_code);
    END LOOP;
    ASSERT_ROUND_TRIP(v_controls);
    IF PKG_JSON.ESCAPE_TEXT(NULL) IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20056, 'Null JSON escape input must remain empty.');
    END IF;
    DBMS_OUTPUT.PUT_LINE('JSON escaping: quotes, slashes, Unicode and all 32 control characters passed.');
END;
/
EXIT SUCCESS;
