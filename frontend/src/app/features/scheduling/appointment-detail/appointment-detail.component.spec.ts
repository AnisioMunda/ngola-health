import { registerLocaleData } from '@angular/common';
import localePtAo from '@angular/common/locales/pt-AO';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AppointmentResponse, SchedulingService } from '../../../core/services/scheduling.service';
import { AppointmentDetailComponent } from './appointment-detail.component';

registerLocaleData(localePtAo);

describe('AppointmentDetailComponent', () => {
  let component: AppointmentDetailComponent;
  let schedulingService: jasmine.SpyObj<SchedulingService>;

  const appointment: AppointmentResponse = {
    id: 'appointment-1',
    patientId: 'patient-1',
    patientName: 'Ana Silva',
    patientPhone: '',
    doctorId: 'doctor-1',
    doctorName: 'Dr. Silva',
    doctorSpecialty: 'Medicina Geral',
    appointmentDate: '2026-10-08',
    appointmentDateLabel: '08/10/2026',
    startTime: '10:00',
    endTime: '10:30',
    status: 'CONFIRMED',
    statusLabel: 'Confirmado',
    appointmentType: 'OUTPATIENT',
    reason: 'Consulta de rotina',
    notes: '',
    cancellationReason: '',
    bookedByName: '',
    createdAt: '',
    confirmedAt: '',
  };

  beforeEach(() => {
    schedulingService = jasmine.createSpyObj<SchedulingService>('SchedulingService', [
      'findById',
      'confirm',
      'complete',
      'cancel',
      'noShow',
    ]);
    schedulingService.findById.and.returnValue(of(appointment));
    schedulingService.complete.and.returnValue(
      of({ ...appointment, status: 'COMPLETED', statusLabel: 'Realizado' }),
    );

    TestBed.configureTestingModule({
      imports: [AppointmentDetailComponent],
      providers: [
        { provide: SchedulingService, useValue: schedulingService },
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(convertToParamMap({ id: appointment.id })) },
        },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
      ],
    });
    const fixture = TestBed.createComponent(AppointmentDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the appointment and exposes only valid state actions', () => {
    expect(schedulingService.findById).toHaveBeenCalledWith(appointment.id);
    expect(component.appointment).toEqual(appointment);
    expect(component.canComplete()).toBeTrue();
    expect(component.canConfirm()).toBeFalse();
    expect(component.canCancel()).toBeTrue();
  });

  it('updates the view when a confirmed appointment is completed', () => {
    component.complete();

    expect(schedulingService.complete).toHaveBeenCalledWith(appointment.id);
    expect(component.appointment?.status).toBe('COMPLETED');
    expect(component.success).toBe('Consulta marcada como realizada.');
    expect(component.updating).toBeFalse();
  });

  it('shows server details when a state transition conflicts', () => {
    schedulingService.complete.and.returnValue(
      throwError(() => ({ error: { detail: 'A consulta já foi concluída.' } })),
    );

    component.complete();

    expect(component.error).toBe('A consulta já foi concluída.');
    expect(component.updating).toBeFalse();
    expect(component.appointment?.status).toBe('CONFIRMED');
  });
});
