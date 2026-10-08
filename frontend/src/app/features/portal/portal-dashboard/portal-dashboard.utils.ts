const STATUS_COLORS: Record<string, string> = {
  ACTIVE: '#3b82f6',
  DISPENSED: '#16a34a',
  CANCELLED: '#dc2626',
  EXPIRED: '#9ca3af',
  PARTIALLY_DISPENSED: '#f59e0b',
  COMPLETED: '#16a34a',
  SCHEDULED: '#3b82f6',
  IN_PROGRESS: '#f59e0b',
  PAID: '#16a34a',
  PENDING: '#f59e0b',
  OVERDUE: '#dc2626',
  EMITIDO: '#3b82f6',
  PAGO_PARCIALMENTE: '#f59e0b',
  PAGO: '#16a34a',
  EM_ATRASO: '#dc2626',
};

const INVOICE_STATUS_LABELS: Record<string, string> = {
  EMITIDO: 'Emitida',
  PAGO_PARCIALMENTE: 'Parcialmente paga',
  EM_ATRASO: 'Em atraso',
  PAGO: 'Paga',
  PAID: 'Pago',
  PENDING: 'Pendente',
  OVERDUE: 'Em atraso',
  CANCELLED: 'Cancelado',
  ISSUED: 'Emitido',
};

export function portalStatusColor(status: string): string {
  return STATUS_COLORS[status] ?? '#9ca3af';
}

export function portalInvoiceStatusLabel(status: string): string {
  return INVOICE_STATUS_LABELS[status] ?? status;
}
