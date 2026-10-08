/* eslint-disable */
/* tslint:disable */
// @ts-nocheck
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

export interface PortalRegisterRequest {
  /**
   * @format email
   * @maxLength 200
   */
  email: string;
  /**
   * @minLength 12
   * @maxLength 72
   */
  password: string;
  /** @maxLength 30 */
  patientNumber: string;
}

export interface PortalLoginRequest {
  /**
   * @format email
   * @maxLength 200
   */
  email: string;
  password: string;
}

export interface PortalRegistrationResponse {
  status: 'PENDING_APPROVAL';
  message: string;
}

export interface PortalLoginResponse {
  token: string;
  patientName: string;
  /** @format uuid */
  patientId: string;
  email: string;
}

export interface PortalDashboardDto {
  patientName: string;
  totalEpisodes: number;
  upcomingAppointments: number;
  pendingLabResults: number;
  pendingInvoices: number;
  totalDebt: number;
  recentEpisodes: PortalEpisodeDto[];
  upcomingAppointmentsList: PortalAppointmentDto[];
}

export interface PortalEpisodeDto {
  /** @format uuid */
  id: string;
  episodeType: string;
  status: string;
  statusLabel: string;
  doctorName: string;
  /** @format date-time */
  scheduledAt: string | null;
  /** @format date-time */
  completedAt: string | null;
  reason: string | null;
  diagnosis: string | null;
  hasLabResults: boolean;
  hasPrescriptions: boolean;
}

export interface PortalLabResultDto {
  /** @format uuid */
  id: string;
  examName: string;
  status: string;
  result: string;
  referenceValues: string | null;
  doctorName: string;
  /** @format date-time */
  requestedAt: string;
  /** @format date-time */
  resultAt: string | null;
}

export interface PortalPrescriptionDto {
  /** @format uuid */
  id: string;
  prescriptionNumber: string;
  doctorName: string;
  /** @format date */
  prescriptionDate: string;
  /** @format date */
  expiryDate: string;
  status: string;
  statusLabel: string;
  diagnosis: string | null;
  items: PortalPrescriptionItemDto[];
}

export interface PortalPrescriptionItemDto {
  medicationName: string;
  dosage: string;
  route: string;
  quantityPrescribed: number;
  quantityDispensed: number;
  status: string;
}

export interface PortalInvoiceDto {
  /** @format uuid */
  id: string;
  invoiceNumber: string;
  /** @format date-time */
  issueDate: string;
  totalAmount: number;
  status: string;
  statusLabel: string;
  description: string | null;
}

export interface PortalAppointmentDto {
  /** @format uuid */
  id: string;
  doctorName: string;
  specialty: string | null;
  /** @format date-time */
  scheduledAt: string;
  status: string;
  reason: string | null;
}
