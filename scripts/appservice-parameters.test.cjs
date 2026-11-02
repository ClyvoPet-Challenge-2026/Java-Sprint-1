const assert = require('node:assert/strict');
const { generateKeyPairSync } = require('node:crypto');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { spawnSync } = require('node:child_process');

const pair = bits => generateKeyPairSync('rsa', {
    modulusLength: bits,
    privateKeyEncoding: { type: 'pkcs8', format: 'pem' },
    publicKeyEncoding: { type: 'spki', format: 'pem' }
});
const base64 = value => Buffer.from(value).toString('base64');
const keys = pair(2048);
const required = {
    AZURE_PLAN_NAME: 'asp-clyvocare-test',
    JAVA_WEBAPP_NAME: 'clyvocare-java-test',
    DOTNET_WEBAPP_NAME: 'clyvocare-dotnet-test',
    SPRING_DATASOURCE_URL: 'jdbc:oracle:thin:@example.invalid:1521:ORCL',
    SPRING_DATASOURCE_USERNAME: 'test',
    SPRING_DATASOURCE_PASSWORD: 'test-secret-"-$-\\-\n',
    ConnectionStrings__ClyvoCareOracle: 'Data Source=example.invalid:1521/ORCL;User Id=test;Password=test;',
    Mongo__ConnectionString: 'mongodb+srv://test:secret@example.invalid',
    Mongo__DatabaseName: 'test',
    JWT_PRIVATE_KEY_BASE64: base64(keys.privateKey),
    JWT_PUBLIC_KEY_BASE64: base64(keys.publicKey)
};
const run = (overrides = {}) => spawnSync(process.execPath, [join(__dirname, 'appservice-parameters.cjs')], {
    env: { ...process.env, ...required, CORS_ALLOWED_ORIGINS: '', ...overrides }, encoding: 'utf8'
});
const successful = run();
assert.equal(successful.status, 0, successful.stderr);
const parameters = JSON.parse(successful.stdout).parameters;
const java = parameters.javaAppSettings.value;
const dotnet = parameters.dotnetAppSettings.value;
assert.equal(java.SPRING_DATASOURCE_PASSWORD, required.SPRING_DATASOURCE_PASSWORD);
assert.equal(java.JWT_PUBLIC_KEY_BASE64, dotnet.JWT_PUBLIC_KEY_BASE64);
assert.equal(dotnet.JWT_PRIVATE_KEY_BASE64, undefined);
assert.equal(dotnet.Cors__AllowedOrigins__0, '');
assert.equal(dotnet.Cors__AllowedOrigins__1, '');
assert.equal(dotnet.DOTNET_ENVIRONMENT, 'Production');
assert.equal(dotnet.Swagger__Enabled, 'true');

const withCors = JSON.parse(run({ CORS_ALLOWED_ORIGINS: 'https://app.example.com' }).stdout).parameters;
assert.equal(withCors.javaAppSettings.value.CORS_ALLOWED_ORIGINS, 'https://app.example.com');
assert.equal(withCors.dotnetAppSettings.value.Cors__AllowedOrigins__0, 'https://app.example.com');
assert.equal(withCors.dotnetAppSettings.value.Cors__AllowedOrigins__1, '');

for (const name of Object.keys(required)) {
    const result = run({ [name]: '' });
    assert.equal(result.status, 1, `Missing ${name} was accepted.`);
    assert.equal(result.stdout, '');
    assert.ok(!result.stderr.includes(required.SPRING_DATASOURCE_PASSWORD));
    assert.ok(!result.stderr.includes(required.JWT_PRIVATE_KEY_BASE64));
}
const weak = pair(1024);
const other = pair(2048);
for (const overrides of [
    { JAVA_WEBAPP_NAME: 'invalid/name' },
    { JAVA_WEBAPP_NAME: required.DOTNET_WEBAPP_NAME },
    { JWT_PRIVATE_KEY_BASE64: 'invalid!' },
    { JWT_PUBLIC_KEY_BASE64: 'invalid!' },
    { JWT_PRIVATE_KEY_BASE64: base64('invalid PEM') },
    { JWT_PUBLIC_KEY_BASE64: base64(keys.privateKey) },
    { JWT_PUBLIC_KEY_BASE64: base64(other.publicKey) },
    { JWT_PRIVATE_KEY_BASE64: base64(weak.privateKey), JWT_PUBLIC_KEY_BASE64: base64(weak.publicKey) },
    { CORS_ALLOWED_ORIGINS: 'http://app.example.com' },
    { CORS_ALLOWED_ORIGINS: 'https://app.example.com/path' }
]) {
    const result = run(overrides);
    assert.equal(result.status, 1, 'Invalid deployment settings were accepted.');
    assert.equal(result.stdout, '');
    assert.ok(!result.stderr.includes(required.JWT_PRIVATE_KEY_BASE64));
}

const template = JSON.parse(readFileSync(join(__dirname, '../infra/appservice.json')));
const plans = template.resources.filter(resource => resource.type === 'Microsoft.Web/serverfarms');
const apps = template.resources.filter(resource => resource.type === 'Microsoft.Web/sites');
assert.equal(plans.length, 1);
assert.deepEqual(plans[0].sku, { name: 'B2', tier: 'Basic', capacity: 1 });
assert.equal(apps.length, 2);
assert.equal(apps[0].properties.serverFarmId, apps[1].properties.serverFarmId);
assert.equal(template.parameters.javaAppSettings.type, 'secureObject');
assert.equal(template.parameters.dotnetAppSettings.type, 'secureObject');
console.log('App Service parameters: assertions passed.');
