import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type DocumentType = 'FT' | 'FR' | 'NC' | 'ND' | 'RC';
export type InvoiceStatus = 'RASCUNHO' | 'EMITIDO' | 'PAGO_PARCIALMENTE' | 'PAGO' | 'ANULADO' | 'EM_ATRASO';
export type PaymentMethod = 'NUMERARIO' | 'TRANSFERENCIA' | 'CARTAO' | 'SEGURO' | 'CHEQUE' | 'DINHEIRO_MOVEL';

export interface ServicePriceResponse {
  id: string;
  code: string;
  description: string;
  category: string;
  unitPrice: number;
  vatRate: number;
  active: boolean;
}

export interface InvoiceItemResponse {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  discountPercent: number;
  vatRate: number;
  lineTotal: number;
}

export interface PaymentResponse {
  id: string;
  amount: number;
  paymentMethod: PaymentMethod;
  reference: string;
  paidAt: string;
  receivedByName: string;
  notes: string;
}

export interface InvoiceResponse {
  id: string;
  invoiceNumber: string;
  documentType: DocumentType;
  patientId: string;
  patientName: string;
  patientNif: string;
  episodeId: string;
  status: InvoiceStatus;
  paymentMethod: PaymentMethod;
  subtotal: number;
  discountAmount: number;
  vatAmount: number;
  totalAmount: number;
  paidAmount: number;
  balance: number;
  insuranceProvider: string;
  insurancePolicyNumber: string;
  insuranceCoveragePercent: number;
  issuedAt: string;
  dueDate: string;
  paidAt: string;
  notes: string;
  createdAt: string;
  agtStatus: string;
  agtValidationCode: string;
  items: InvoiceItemResponse[];
  payments: PaymentResponse[];
}

export interface CreateInvoiceRequest {
  patientId: string;
  episodeId?: string;
  documentType?: DocumentType;
  patientNif?: string;
  patientFiscalName?: string;
  insuranceProvider?: string;
  insurancePolicyNumber?: string;
  insuranceCoveragePercent?: number;
  dueDate?: string;
  notes?: string;
  items: {
    servicePriceId?: string;
    description?: string;
    quantity?: number;
    unitPrice?: number;
    discountPercent?: number;
    vatRate?: number;
  }[];
}

export interface RegisterPaymentRequest {
  amount: number;
  paymentMethod: PaymentMethod;
  reference?: string;
  notes?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  FT: 'Factura',
  FR: 'Factura/Recibo',
  NC: 'Nota de Crédito',
  ND: 'Nota de Débito',
  RC: 'Recibo'
};

export const STATUS_LABELS: Record<InvoiceStatus, string> = {
  RASCUNHO:          'Rascunho',
  EMITIDO:           'Emitido',
  PAGO_PARCIALMENTE: 'Pago Parcialmente',
  PAGO:              'Pago',
  ANULADO:           'Anulado',
  EM_ATRASO:         'Em Atraso'
};

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  NUMERARIO:     'Numerário',
  TRANSFERENCIA: 'Transferência Bancária',
  CARTAO:        'Cartão',
  SEGURO:        'Seguro de Saúde',
  CHEQUE:        'Cheque',
  DINHEIRO_MOVEL:'Dinheiro Móvel'
};

@Injectable({ providedIn: 'root' })
export class FinancialService {

  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/financial`;

  findAllPrices(): Observable<ServicePriceResponse[]> {
    return this.http.get<ServicePriceResponse[]>(`${this.apiUrl}/prices`);
  }

  findAllInvoices(
    patientId?: string,
    status?: InvoiceStatus,
    page = 0,
    size = 20
  ): Observable<Page<InvoiceResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (patientId) params = params.set('patientId', patientId);
    if (status)    params = params.set('status', status);
    return this.http.get<Page<InvoiceResponse>>(`${this.apiUrl}/invoices`, { params });
  }

  findById(id: string): Observable<InvoiceResponse> {
    return this.http.get<InvoiceResponse>(`${this.apiUrl}/invoices/${id}`);
  }

  create(request: CreateInvoiceRequest): Observable<InvoiceResponse> {
    return this.http.post<InvoiceResponse>(`${this.apiUrl}/invoices`, request);
  }

  issue(id: string): Observable<InvoiceResponse> {
    return this.http.patch<InvoiceResponse>(`${this.apiUrl}/invoices/${id}/issue`, {});
  }

  registerPayment(id: string, request: RegisterPaymentRequest): Observable<InvoiceResponse> {
    return this.http.post<InvoiceResponse>(
      `${this.apiUrl}/invoices/${id}/payments`, request);
  }

  void_(id: string, reason: string): Observable<InvoiceResponse> {
    return this.http.patch<InvoiceResponse>(
      `${this.apiUrl}/invoices/${id}/void`,
      {},
      { params: new HttpParams().set('reason', reason) }
    );
  }

  downloadPdf(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/invoices/${id}/pdf`, {
      responseType: 'blob'
    });
  }
}