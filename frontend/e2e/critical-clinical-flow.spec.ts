import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';
import {
  findE2eEpisodeId,
  findE2eInvoiceNumber,
  seedE2eData,
} from './seed';

test.beforeAll(async () => {
  await seedE2eData();
});

test('login to invoice through the critical clinical workflow', async ({ page }) => {
  const email = process.env.PLAYWRIGHT_ADMIN_EMAIL;
  const password = process.env.PLAYWRIGHT_ADMIN_PASSWORD;
  const medicationId = process.env.PLAYWRIGHT_MEDICATION_ID;
  const servicePriceId = process.env.PLAYWRIGHT_SERVICE_PRICE_ID;
  expect(email).toBeTruthy();
  expect(password).toBeTruthy();
  expect(medicationId).toBeTruthy();
  expect(servicePriceId).toBeTruthy();

  await page.goto('/login');
  await page.getByLabel('Email').fill(email!);
  await page.getByLabel('Palavra-passe', { exact: true }).fill(password!);
  const loginResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().endsWith('/api/auth/login'),
  );
  await page.getByRole('button', { name: 'Entrar no Sistema' }).click();
  const loginResponse = await loginResponsePromise;
  expect(loginResponse.status()).toBe(200);
  await expect(page).toHaveURL(/\/dashboard$/);

  const patientName = `Paciente E2E ${randomUUID().slice(0, 8)}`;
  await page.goto('/patients/new');
  await page.getByLabel('Nome completo *').fill(patientName);
  await page.getByLabel('Data de nascimento *').fill('1990-01-01');
  await page.getByLabel('Género *').selectOption('FEMALE');
  const patientResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().endsWith('/api/patients'),
  );
  await page.getByRole('button', { name: 'Registar paciente' }).click();
  const patientResponse = await patientResponsePromise;
  expect(patientResponse.status()).toBe(201);
  const accessToken = await page.evaluate(() => localStorage.getItem('accessToken'));
  expect(accessToken).toBeTruthy();
  const patientListResponse = await page.request.get(
    'http://127.0.0.1:4200/api/patients?page=0&size=100&sort=fullName',
    { headers: { Authorization: `Bearer ${accessToken}` } },
  );
  expect(patientListResponse.status()).toBe(200);
  const patientList = (await patientListResponse.json()) as {
    content: Array<{ id: string; fullName: string }>;
  };
  const patient = patientList.content.find((item) => item.fullName === patientName);
  expect(patient).toBeDefined();
  const patientId = patient!.id;

  const patientOptionsResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'GET' &&
      new URL(response.url()).pathname === '/api/patients' &&
      new URL(response.url()).searchParams.get('size') === '100',
  );
  await page.goto('/episodes/new');
  const patientOptionsResponse = await patientOptionsResponsePromise;
  expect(patientOptionsResponse.status()).toBe(200);
  const patientOptions = (await patientOptionsResponse.json()) as {
    content: Array<{ id: string; fullName: string }>;
  };
  expect(patientOptions.content.some((item) => item.fullName === patientName)).toBe(true);
  await expect(page.locator('#episode-patient option', { hasText: patientName })).toHaveCount(1);
  await page.locator('#episode-patient').selectOption({ label: patientName });
  await page.getByLabel('Motivo').fill('Consulta E2E');
  await page.getByLabel('Diagnóstico').fill('Diagnóstico E2E');
  const episodeResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().endsWith('/api/episodes'),
  );
  await page.getByRole('button', { name: 'Registar episódio' }).click();
  const episodeResponse = await episodeResponsePromise;
  expect(episodeResponse.status()).toBe(201);
  const episodeId = await findE2eEpisodeId(patientId);

  await page.goto(`/prescriptions/new?patientId=${patientId}&episodeId=${episodeId}`);
  await expect(page.locator('select').first()).toHaveValue(patientId);
  await page.locator('select').nth(1).selectOption(medicationId!);
  await page.locator('input[formcontrolname="diagnosis"]').fill('Diagnóstico E2E');
  await page.locator('input[formcontrolname="dosage"]').fill('1 comprimido de 8 em 8 horas');
  const prescriptionResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().endsWith('/api/prescriptions'),
  );
  await page.getByRole('button', { name: 'Emitir Prescrição' }).click();
  const prescriptionResponse = await prescriptionResponsePromise;
  expect(prescriptionResponse.status()).toBe(201);
  await expect(page.getByRole('heading', { name: 'Detalhe da Prescrição' })).toBeVisible();
  await expect(page.getByText(patientName, { exact: true })).toBeVisible();
  await expect(page.getByText('Diagnóstico E2E', { exact: true })).toBeVisible();
  await expect(page.getByText('E2E Paracetamol 500mg', { exact: false })).toBeVisible();

  await page.goto('/financial/new');
  await page.getByRole('combobox').nth(0).selectOption({ label: patientName });
  await page.getByRole('combobox').nth(1).selectOption(servicePriceId!);
  const invoiceResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST' && response.url().endsWith('/api/financial/invoices'),
  );
  await page.getByRole('button', { name: 'Criar Documento' }).click();
  const invoiceResponse = await invoiceResponsePromise;
  expect(invoiceResponse.status()).toBe(201);
  const invoiceNumber = await findE2eInvoiceNumber(patientId);
  await expect(page.getByRole('heading', { name: 'Facturação' })).toBeVisible();
  await expect(page.getByText(patientName, { exact: true })).toBeVisible();
  await expect(page.getByText(invoiceNumber, { exact: true })).toBeVisible();
});
