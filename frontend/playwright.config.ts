import { randomBytes } from 'node:crypto';
import { resolve } from 'node:path';
import { defineConfig, devices } from '@playwright/test';

const frontendDirectory = __dirname;
const backendDirectory = resolve(frontendDirectory, '../backend');
const baseURL = 'http://127.0.0.1:4200';
const databaseName = process.env.E2E_DB_NAME ?? 'hospitalao_e2e';
const databaseUser = process.env.E2E_DB_USER ?? 'hospitalao';
const databasePassword = process.env.E2E_DB_PASSWORD ?? 'hospitalao-e2e';
const databaseHost = process.env.E2E_DB_HOST ?? '127.0.0.1';
const databasePort = process.env.E2E_DB_PORT ?? '5433';
const bootstrapPassword = randomBytes(24).toString('base64url');
process.env.PLAYWRIGHT_ADMIN_EMAIL = 'playwright-admin@hospitalao.local';
process.env.PLAYWRIGHT_ADMIN_PASSWORD = randomBytes(24).toString('base64url');
process.env.PLAYWRIGHT_MEDICATION_ID = '00000000-0000-0000-0000-000000000991';
process.env.PLAYWRIGHT_SERVICE_PRICE_ID = '00000000-0000-0000-0000-000000000993';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'list',
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    ...devices['Desktop Chrome'],
  },
  webServer: [
    {
      command: './mvnw spring-boot:run',
      cwd: backendDirectory,
      port: 8080,
      timeout: 180_000,
      reuseExistingServer: !process.env.CI,
      env: {
        ...process.env,
        SPRING_PROFILES_ACTIVE: 'dev',
        SPRING_JPA_HIBERNATE_DDL_AUTO: 'none',
        DB_HOST: databaseHost,
        DB_PORT: databasePort,
        DB_NAME: databaseName,
        DB_USER: databaseUser,
        DB_PASSWORD: databasePassword,
        JWT_SECRET: process.env.JWT_SECRET ?? 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=',
        DEV_ADMIN_INITIAL_PASSWORD_BASE64: Buffer.from(bootstrapPassword).toString('base64'),
        APP_BASE_URL: baseURL,
        AGT_API_URL: 'https://example.invalid',
        AGT_ENABLED: 'false',
        AGT_NIF: '5000000001HA044',
        AGT_SOFTWARE_ID: 'HospitalAO-E2E',
        AGT_SOFTWARE_VERSION: '0.0.1-E2E',
      },
    },
    {
      command: 'npx ng serve --host 127.0.0.1 --port 4200 --poll 1000',
      cwd: frontendDirectory,
      url: baseURL,
      timeout: 120_000,
      reuseExistingServer: !process.env.CI,
    },
  ],
});
