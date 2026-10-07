import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import {
  AppointmentResponse,
  DayAvailabilityResponse,
  SchedulingService,
  SlotResponse,
} from '../../../core/services/scheduling.service';
import { PatientService } from '../../../core/services/patient.service';
import { UserManagementService } from '../../../core/services/user-management.service';
import { AppointmentFormComponent } from './appointment-form.component';

describe('AppointmentFormComponent', () => {
  let component: AppointmentFormComponent;
  let schedulingService: jasmine.SpyObj<SchedulingService>;
  let patientService: jasmine.SpyObj<PatientService>;
  let userService: jasmine.SpyObj<UserManagementService>;
  let router: jasmine.SpyObj<Router>;

  const slot: SlotResponse = {
    date: '2026-10-08',
    startTime: '10:00',
    endTime: '10:30',
    doctorId: 'doctor-1',
    doctorName: 'Dr. Silva',
    available: true,
    bookedCount: 0,
    maxPatients: 1,
  };

  const availability: DayAvailabilityResponse = {
    date: slot.date,
    dateLabel: 'quinta-feira, 08 de Outubro',
    dayOfWeek: 'Quinta-feira',
    slots: [slot],
    totalSlots: 1,
    availableSlots: 1,
  };

  beforeEach(() => {
    schedulingService = jasmine.createSpyObj<SchedulingService>('SchedulingService', [
      'getAvailability',
      'create',
    ]);
    patientService = jasmine.createSpyObj<PatientService>('PatientService', ['findAll']);
    userService = jasmine.createSpyObj<UserManagementService>('UserManagementService', ['findAll']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    schedulingService.getAvailability.and.returnValue(of(availability));
    schedulingService.create.and.returnValue(
      throwError(() => ({ error: { detail: 'O horário acabou de ser ocupado.' } })),
    );
    patientService.findAll.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 200 }),
    );
    userService.findAll.and.returnValue(
      of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 }),
    );
    router.navigate.and.resolveTo(true);

    TestBed.configureTestingModule({
      imports: [AppointmentFormComponent],
      providers: [
        { provide: SchedulingService, useValue: schedulingService },
        { provide: PatientService, useValue: patientService },
        { provide: UserManagementService, useValue: userService },
        { provide: Router, useValue: router },
      ],
    });
    const fixture = TestBed.createComponent(AppointmentFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('uses the browser local date as the minimum booking date', () => {
    const today = new Date();
    const expected = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(
      today.getDate(),
    ).padStart(2, '0')}`;

    expect(component.minDate).toBe(expected);
  });

  it('ignores stale slot responses when a newer availability request completes first', () => {
    const firstRequest = new Subject<DayAvailabilityResponse>();
    const secondRequest = new Subject<DayAvailabilityResponse>();
    const laterAvailability = { ...availability, date: '2026-10-09' };
    schedulingService.getAvailability.and.returnValues(firstRequest, secondRequest);

    component.loadSlots('doctor-1', '2026-10-08');
    component.loadSlots('doctor-1', '2026-10-09');
    secondRequest.next(laterAvailability);
    firstRequest.next(availability);

    expect(component.availability).toBe(laterAvailability);
    expect(component.loadingSlots).toBeFalse();
  });

  it('shows a booking conflict and prevents a duplicate submission while saving', () => {
    const response = new Subject<AppointmentResponse>();
    schedulingService.create.and.returnValue(response);
    component.form.patchValue({
      patientId: 'patient-1',
      doctorId: 'doctor-1',
      appointmentDate: slot.date,
      startTime: slot.startTime,
      appointmentType: 'OUTPATIENT',
      reason: 'Consulta de rotina',
      notes: '',
    });
    component.selectedSlot = slot;

    component.onSubmit();
    component.onSubmit();

    expect(schedulingService.create).toHaveBeenCalledTimes(1);
    expect(component.saving).toBeTrue();
    response.error({ error: { detail: 'O horário acabou de ser ocupado.' } });

    expect(component.error).toBe('O horário acabou de ser ocupado.');
    expect(component.saving).toBeFalse();
  });
});
