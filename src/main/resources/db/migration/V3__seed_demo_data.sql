-- Carga de demonstracao: inclui registros ausentes sem depender de IDs fixos.
-- Dados existentes de catalogo e senhas personalizadas sao preservados.

MERGE INTO TB_CAD_STATE target
USING (
    SELECT 'SP' UF, 'Sao Paulo' NAME FROM DUAL UNION ALL
    SELECT 'RJ', 'Rio de Janeiro' FROM DUAL UNION ALL
    SELECT 'MG', 'Minas Gerais' FROM DUAL UNION ALL
    SELECT 'PR', 'Parana' FROM DUAL UNION ALL
    SELECT 'CE', 'Ceara' FROM DUAL UNION ALL
    SELECT 'RS', 'Rio Grande do Sul' FROM DUAL UNION ALL
    SELECT 'BA', 'Bahia' FROM DUAL UNION ALL
    SELECT 'SC', 'Santa Catarina' FROM DUAL
) seed ON (target.UF = seed.UF)
WHEN NOT MATCHED THEN
    INSERT (UF, NAME) VALUES (seed.UF, seed.NAME);

MERGE INTO TB_CAD_CITY target
USING (
    SELECT state.ID STATE_ID, cities.NAME
    FROM (
        SELECT 'SP' UF, 'Sao Paulo' NAME FROM DUAL UNION ALL
        SELECT 'SP', 'Campinas' FROM DUAL UNION ALL
        SELECT 'SP', 'Santos' FROM DUAL UNION ALL
        SELECT 'RJ', 'Rio de Janeiro' FROM DUAL UNION ALL
        SELECT 'RJ', 'Niteroi' FROM DUAL UNION ALL
        SELECT 'MG', 'Belo Horizonte' FROM DUAL UNION ALL
        SELECT 'PR', 'Curitiba' FROM DUAL UNION ALL
        SELECT 'CE', 'Fortaleza' FROM DUAL UNION ALL
        SELECT 'RS', 'Porto Alegre' FROM DUAL UNION ALL
        SELECT 'BA', 'Salvador' FROM DUAL UNION ALL
        SELECT 'SC', 'Florianopolis' FROM DUAL
    ) cities
    JOIN TB_CAD_STATE state ON state.UF = cities.UF
) seed ON (target.STATE_ID = seed.STATE_ID AND target.NAME = seed.NAME)
WHEN NOT MATCHED THEN
    INSERT (STATE_ID, NAME) VALUES (seed.STATE_ID, seed.NAME);

MERGE INTO TB_CAD_SPECIES target
USING (
    SELECT 'Dog' NAME FROM DUAL UNION ALL
    SELECT 'Cat' FROM DUAL UNION ALL
    SELECT 'Bird' FROM DUAL UNION ALL
    SELECT 'Rabbit' FROM DUAL UNION ALL
    SELECT 'Reptile' FROM DUAL UNION ALL
    SELECT 'Hamster' FROM DUAL
) seed ON (target.NAME = seed.NAME)
WHEN NOT MATCHED THEN
    INSERT (NAME) VALUES (seed.NAME);

MERGE INTO TB_CAD_BREED target
USING (
    SELECT species.ID SPECIES_ID, breeds.NAME
    FROM (
        SELECT 'Dog' SPECIES_NAME, 'Labrador' NAME FROM DUAL UNION ALL
        SELECT 'Dog', 'Golden Retriever' FROM DUAL UNION ALL
        SELECT 'Dog', 'Bulldog' FROM DUAL UNION ALL
        SELECT 'Dog', 'Poodle' FROM DUAL UNION ALL
        SELECT 'Dog', 'Mixed' FROM DUAL UNION ALL
        SELECT 'Cat', 'Siamese' FROM DUAL UNION ALL
        SELECT 'Cat', 'Persian' FROM DUAL UNION ALL
        SELECT 'Cat', 'Maine Coon' FROM DUAL UNION ALL
        SELECT 'Bird', 'Cockatiel' FROM DUAL UNION ALL
        SELECT 'Rabbit', 'Mini Lop' FROM DUAL
    ) breeds
    JOIN TB_CAD_SPECIES species ON species.NAME = breeds.SPECIES_NAME
) seed ON (target.SPECIES_ID = seed.SPECIES_ID AND target.NAME = seed.NAME)
WHEN NOT MATCHED THEN
    INSERT (SPECIES_ID, NAME) VALUES (seed.SPECIES_ID, seed.NAME);

MERGE INTO TB_CAD_PLAN target
USING (
    SELECT 'Essential' NAME, 49.90 MONTHLY_VALUE FROM DUAL UNION ALL
    SELECT 'Basic', 69.90 FROM DUAL UNION ALL
    SELECT 'Premium', 99.90 FROM DUAL UNION ALL
    SELECT 'Master', 139.90 FROM DUAL UNION ALL
    SELECT 'Total', 179.90 FROM DUAL UNION ALL
    SELECT 'Corporate', 249.90 FROM DUAL
) seed ON (target.NAME = seed.NAME)
WHEN NOT MATCHED THEN
    INSERT (NAME, MONTHLY_VALUE) VALUES (seed.NAME, seed.MONTHLY_VALUE);

-- BCrypt gerado com BCryptPasswordEncoder para a senha de demonstracao senha123.
-- Apenas hash1 da Ana e hash2 do Carlos sao substituidos em contas existentes.
MERGE INTO TB_CAD_OWNER target
USING (
    SELECT owners.*, city.ID CITY_ID,
           '$2a$10$4NuiT5AE9QmXrpR/6CHs9.3MYiW5o7dGz6edrGoK1ADBviRArLyny' PASSWORD_HASH
    FROM (
        SELECT 'ana@email.com' EMAIL, 'Ana Paula Souza' NAME,
               '111.111.111-11' CPF, '(11) 91111-1111' PHONE,
               'ADMIN' ROLE_NAME, 'hash1' LEGACY_HASH FROM DUAL
        UNION ALL
        SELECT 'carlos@email.com', 'Carlos Henrique Lima',
               '222.222.222-22', '(11) 92222-2222',
               'OWNER', 'hash2' FROM DUAL
    ) owners
    JOIN TB_CAD_CITY city ON city.NAME = 'Sao Paulo'
    JOIN TB_CAD_STATE state ON state.ID = city.STATE_ID AND state.UF = 'SP'
) seed ON (target.EMAIL = seed.EMAIL)
WHEN MATCHED THEN
    UPDATE SET
        target.PASSWORD_HASH = CASE
            WHEN target.PASSWORD_HASH = seed.LEGACY_HASH THEN seed.PASSWORD_HASH
            ELSE target.PASSWORD_HASH
        END,
        target.ROLE_NAME = seed.ROLE_NAME
    WHERE target.PASSWORD_HASH = seed.LEGACY_HASH OR target.ROLE_NAME <> seed.ROLE_NAME
WHEN NOT MATCHED THEN
    INSERT (NAME, CPF, EMAIL, PASSWORD_HASH, ROLE_NAME, ENABLED, PHONE, CITY_ID)
    VALUES (seed.NAME, seed.CPF, seed.EMAIL, seed.PASSWORD_HASH,
            seed.ROLE_NAME, 'Y', seed.PHONE, seed.CITY_ID);
