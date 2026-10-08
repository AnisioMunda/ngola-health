import { Client } from 'pg';

const hospitalAdminId = '00000000-0000-0000-0000-000000000990';
const medicationId = process.env.PLAYWRIGHT_MEDICATION_ID;
const stockBatchId = '00000000-0000-0000-0000-000000000992';
const servicePriceId = process.env.PLAYWRIGHT_SERVICE_PRICE_ID;
const adminEmail = process.env.PLAYWRIGHT_ADMIN_EMAIL;
const adminPassword = process.env.PLAYWRIGHT_ADMIN_PASSWORD;

function createClient(): Client {
  return new Client({
    host: process.env.E2E_DB_HOST ?? '127.0.0.1',
    port: Number(process.env.E2E_DB_PORT ?? 5433),
    database: process.env.E2E_DB_NAME ?? 'hospitalao_e2e',
    user: process.env.E2E_DB_USER ?? 'hospitalao',
    password: process.env.E2E_DB_PASSWORD ?? 'hospitalao-e2e',
  });
}

export async function seedE2eData(): Promise<void> {
  if (!medicationId || !servicePriceId || !adminEmail || !adminPassword) {
    throw new Error('Playwright configuration is missing the synthetic E2E fixture values.');
  }

  const client = createClient();

  await client.connect();
  try {
    await client.query('CREATE EXTENSION IF NOT EXISTS pgcrypto');
    const { rows } = await client.query("SELECT id FROM hospitals WHERE code = 'HCL-001'");
    if (rows.length !== 1) {
      throw new Error('Expected exactly one HCL-001 hospital for the E2E fixture.');
    }
    const hospitalId = rows[0].id;

    await client.query(
      `INSERT INTO users (
         id, full_name, username, password_hash, email, register_status,
         must_change_password, hospital_id
       )
       VALUES ($1, 'Playwright Test Administrator', 'playwright-admin',
               crypt($2, gen_salt('bf', 12)), $3, 'ACTIVE', FALSE, $4)
       ON CONFLICT (id) DO UPDATE SET
         password_hash = EXCLUDED.password_hash,
         email = EXCLUDED.email,
         register_status = 'ACTIVE',
         must_change_password = FALSE,
         hospital_id = EXCLUDED.hospital_id`,
      [hospitalAdminId, adminPassword, adminEmail, hospitalId],
    );
    await client.query(
      `INSERT INTO user_roles (user_id, role_id)
       SELECT $1, id FROM roles WHERE name = 'ADMIN'
       ON CONFLICT DO NOTHING`,
      [hospitalAdminId],
    );
    await client.query(
      `INSERT INTO medications (
         id, hospital_id, name, generic_name, dosage_form, strength, unit,
         requires_prescription, min_stock_level, active
       )
       VALUES ($1, $2, 'E2E Paracetamol 500mg', 'Paracetamol',
               'TABLET', '500 mg', 'comprimido', TRUE, 10, TRUE)
       ON CONFLICT (id) DO UPDATE SET
         hospital_id = EXCLUDED.hospital_id,
         name = EXCLUDED.name,
         active = TRUE`,
      [medicationId, hospitalId],
    );
    await client.query(
      `INSERT INTO stock_batches (
         id, medication_id, hospital_id, batch_number, expiry_date,
         quantity_received, quantity_available, unit_cost, supplier, created_by
       )
       VALUES ($1, $2, $3, 'E2E-BATCH-001', '2099-12-31', 100, 100, 1.00,
               'Synthetic E2E fixture', $4)
       ON CONFLICT (id) DO UPDATE SET
         hospital_id = EXCLUDED.hospital_id,
         quantity_received = 100,
         quantity_available = 100,
         expiry_date = '2099-12-31'`,
      [stockBatchId, medicationId, hospitalId, hospitalAdminId],
    );
    await client.query(
      `INSERT INTO service_prices (
         id, hospital_id, code, description, category, unit_price, vat_rate, active
       )
       VALUES ($1, $2, 'E2E-001', 'Consulta E2E', 'CONSULTATION', 1500.00, 0.00, TRUE)
       ON CONFLICT (id) DO UPDATE SET
         hospital_id = EXCLUDED.hospital_id,
         code = EXCLUDED.code,
         description = EXCLUDED.description,
         active = TRUE`,
      [servicePriceId, hospitalId],
    );
  } finally {
    await client.end();
  }
}

export async function findE2ePatientId(fullName: string): Promise<string> {
  const client = createClient();
  await client.connect();
  try {
    const { rows } = await client.query<{ id: string }>(
      'SELECT id FROM patients WHERE full_name = $1 ORDER BY created_at DESC',
      [fullName],
    );
    if (rows.length !== 1) {
      throw new Error(`Expected one E2E patient named "${fullName}", found ${rows.length}.`);
    }
    return rows[0].id;
  } finally {
    await client.end();
  }
}

export async function findE2eEpisodeId(patientId: string): Promise<string> {
  const client = createClient();
  await client.connect();
  try {
    const { rows } = await client.query<{ id: string }>(
      `SELECT id FROM episodes
       WHERE patient_id = $1 AND reason = 'Consulta E2E'
       ORDER BY created_at DESC`,
      [patientId],
    );
    if (rows.length !== 1) {
      throw new Error(`Expected one E2E episode for patient "${patientId}", found ${rows.length}.`);
    }
    return rows[0].id;
  } finally {
    await client.end();
  }
}

export async function findE2eInvoiceNumber(patientId: string): Promise<string> {
  const client = createClient();
  await client.connect();
  try {
    const { rows } = await client.query<{ invoice_number: string }>(
      `SELECT invoice_number FROM invoices
       WHERE patient_id = $1
       ORDER BY created_at DESC`,
      [patientId],
    );
    if (rows.length !== 1) {
      throw new Error(`Expected one E2E invoice for patient "${patientId}", found ${rows.length}.`);
    }
    return rows[0].invoice_number;
  } finally {
    await client.end();
  }
}
