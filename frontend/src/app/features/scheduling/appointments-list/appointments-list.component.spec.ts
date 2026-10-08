import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AppointmentResponse, SchedulingService } from '../../../core/services/scheduling.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { AppointmentsListComponent } from './appointments-list.component';

describe('AppointmentsListComponent', () => {
  let component: AppointmentsListComponent;
  let schedulingService: jasmine.SpyObj<SchedulingService>;
  let userService: jasmine.SpyObj<UserManagementService>;

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
    status: 'SCHEDULED',
    statusLabel: 'Agendado',
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
      'findAll',
      'confirm',
      'cancel',
      'noShow',
    ]);
    userService = jasmine.createSpyObj<UserManagementService>('UserManagementService', ['findAll']);
    schedulingService.findAll.and.returnValue(
      of({ content: [appointment], totalElements: 1, totalPages: 1, number: 0, size: 20 }),
    );
    schedulingService.noShow.and.returnValue(of({ ...appointment, status: 'NO_SHOW' }));
    userService.findAll.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }),
    );

    TestBed.configureTestingModule({
      imports: [AppointmentsListComponent],
      providers: [
        { provide: SchedulingService, useValue: schedulingService },
        { provide: UserManagementService, useValue: userService },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
      ],
    });
    const fixture = TestBed.createComponent(AppointmentsListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('reloads the filtered page after recording a no-show', () => {
    const event = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);
    const updatedAppointment: AppointmentResponse = {
      ...appointment,
      status: 'NO_SHOW',
      statusLabel: 'Não Compareceu',
    };
    schedulingService.findAll.and.returnValue(
      of({ content: [updatedAppointment], totalElements: 1, totalPages: 1, number: 0, size: 20 }),
    );

    component.noShow(appointment.id, event);

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(schedulingService.findAll).toHaveBeenCalledTimes(2);
    expect(component.appointments[0].status).toBe('NO_SHOW');
  });

  it('shows API conflict details when recording a no-show fails', () => {
    schedulingService.noShow.and.returnValue(
      throwError(() => ({ error: { detail: 'O estado da consulta já foi alterado.' } })),
    );
    const event = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);

    component.noShow(appointment.id, event);

    expect(component.error).toBe('O estado da consulta já foi alterado.');
  });
});
