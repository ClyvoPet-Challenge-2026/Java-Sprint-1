const { createPrivateKey, createPublicKey } = require('node:crypto');

try {
    const required = name => {
        const value = process.env[name];
        if (!value || !value.trim()) throw new Error(`Missing required environment variable: ${name}`);
        return value;
    };
    const javaName = required('JAVA_WEBAPP_NAME');
    const dotnetName = required('DOTNET_WEBAPP_NAME');
    for (const name of [javaName, dotnetName]) {
        if (!/^[a-z0-9][a-z0-9-]{0,58}[a-z0-9]$/.test(name))
            throw new Error('Web App names must be distinct lowercase Azure host names, 2 to 60 characters.');
    }
    if (javaName === dotnetName) throw new Error('Java and .NET require different Web App names.');

    const decode = name => {
        const value = required(name);
        const decoded = Buffer.from(value, 'base64');
        if (decoded.toString('base64') !== value) throw new Error(`Invalid single-line Base64: ${name}`);
        return decoded.toString('utf8');
    };
    const privatePem = decode('JWT_PRIVATE_KEY_BASE64');
    const publicPem = decode('JWT_PUBLIC_KEY_BASE64');
    if (publicPem.includes('PRIVATE KEY')) throw new Error('Configure only the public PEM in JWT_PUBLIC_KEY_BASE64.');
    let privateKey, publicKey;
    try {
        privateKey = createPrivateKey(privatePem);
        publicKey = createPublicKey(publicPem);
    } catch {
        throw new Error('Configure valid unencrypted RSA PEM files.');
    }
    if (privateKey.asymmetricKeyType !== 'rsa' || publicKey.asymmetricKeyType !== 'rsa'
        || privateKey.asymmetricKeyDetails.modulusLength < 2048
        || !createPublicKey(privateKey).export({ type: 'spki', format: 'der' })
            .equals(publicKey.export({ type: 'spki', format: 'der' })))
        throw new Error('Configure a matching RSA pair of at least 2048 bits.');

    const origins = (process.env.CORS_ALLOWED_ORIGINS || '').split(',').map(value => value.trim()).filter(Boolean);
    for (const origin of origins) {
        let url;
        try { url = new URL(origin); } catch { throw new Error('CORS_ALLOWED_ORIGINS must contain HTTPS origins.'); }
        if (url.protocol !== 'https:' || url.origin !== origin)
            throw new Error('CORS_ALLOWED_ORIGINS must contain HTTPS origins without paths or trailing slashes.');
    }
    const javaSettings = {
        SPRING_DATASOURCE_URL: required('SPRING_DATASOURCE_URL'),
        SPRING_DATASOURCE_USERNAME: required('SPRING_DATASOURCE_USERNAME'),
        SPRING_DATASOURCE_PASSWORD: required('SPRING_DATASOURCE_PASSWORD'),
        JWT_PRIVATE_KEY_BASE64: required('JWT_PRIVATE_KEY_BASE64'),
        JWT_PUBLIC_KEY_BASE64: required('JWT_PUBLIC_KEY_BASE64'),
        CORS_ALLOWED_ORIGINS: origins.join(','),
        JAVA_OPTS: '-Xms256m -Xmx768m',
        WEBSITE_DISABLE_JAVA_HEAP_CONFIGURATION: 'true',
        SCM_DO_BUILD_DURING_DEPLOYMENT: 'false'
    };
    const dotnetSettings = {
        ConnectionStrings__ClyvoCareOracle: required('ConnectionStrings__ClyvoCareOracle'),
        Mongo__ConnectionString: required('Mongo__ConnectionString'),
        Mongo__DatabaseName: required('Mongo__DatabaseName'),
        JWT_PUBLIC_KEY_BASE64: required('JWT_PUBLIC_KEY_BASE64'),
        Jwt__Issuer: 'clyvocare-api',
        Swagger__Enabled: 'true',
        ASPNETCORE_ENVIRONMENT: 'Production',
        DOTNET_ENVIRONMENT: 'Production',
        SCM_DO_BUILD_DURING_DEPLOYMENT: 'false'
    };
    // Override both local defaults even when only one web origin is configured.
    for (let index = 0; index < Math.max(2, origins.length); index++)
        dotnetSettings[`Cors__AllowedOrigins__${index}`] = origins[index] || '';

    process.stdout.write(JSON.stringify({
        $schema: 'https://schema.management.azure.com/schemas/2019-04-01/deploymentParameters.json#',
        contentVersion: '1.0.0.0',
        parameters: {
            location: { value: process.env.AZURE_LOCATION || 'eastus' },
            planName: { value: required('AZURE_PLAN_NAME') },
            javaAppName: { value: javaName },
            dotnetAppName: { value: dotnetName },
            javaAppSettings: { value: javaSettings },
            dotnetAppSettings: { value: dotnetSettings }
        }
    }));
} catch (error) {
    console.error(error.message);
    process.exitCode = 1;
}
