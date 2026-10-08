import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import {
  DoctorScheduleResponse,
  SchedulingService,
} from '../../../core/services/scheduling.service';
import {
  UserManagementService,
  UserResponse,
} from '../../../core/services/user-management.service';
import { DoctorSchedulesComponent } from './doctor-schedules.component';

describe('DoctorSchedulesComponent', () => {
  let component: DoctorSchedulesComponent;
  let schedulingService: jasmine.SpyObj<SchedulingService>;
  let userService: jasmine.SpyObj<UserManagementService>;

  const doctor: UserResponse = {
    id: 'doctor-1',
    fullName: 'Dr. Silva',
    username: 'doctor.silva',
    email: '',
    phone: '',
    especiality: 'Medicina Geral',
    professionalCard: '',
    teamsUserId: null,
    registerStatus: 'ACTIVE',
    mustChangePassword: false,
    lastLogin: '',
    createdAt: '',
    roles: ['DOCTOR'],
  };

  const schedule: DoctorScheduleResponse = {
    id: 'schedule-1',
    doctorId: doctor.id,
    doctorName: doctor.fullName,
    specialty: doctor.especiality,
    dayOfWeek: 0,
    dayLabel: 'Segunda-feira',
    startTime: '08:00',
    endTime: '13:00',
    slotDurationMinutes: 30,
    maxPatientsPerSlot: 1,
    active: true,
  };

  beforeEach(() => {
    schedulingService = jasmine.createSpyObj<SchedulingService>('SchedulingService', [
      'getHospitalSchedules',
      'createSchedule',
      'deleteSchedule',
    ]);
    userService = jasmine.createSpyObj<UserManagementService>('UserManagementService', ['findAll']);
    schedulingService.getHospitalSchedules.and.returnValue(of([]));
    schedulingService.createSchedule.and.returnValue(of(schedule));
    schedulingService.deleteSchedule.and.returnValue(of(void 0));
    userService.findAll.and.returnValue(
      of({ content: [doctor], totalElements: 1, totalPages: 1, number: 0, size: 100 }),
    );

    TestBed.configureTestingModule({
      imports: [DoctorSchedulesComponent],
      providers: [
        { provide: SchedulingService, useValue: schedulingService },
        { provide: UserManagementService, useValue: userService },
      ],
    });
    const fixture = TestBed.createComponent(DoctorSchedulesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('rejects a schedule whose end precedes its start', () => {
    component.form.patchValue({
      doctorId: doctor.id,
      startTime: '13:00',
      endTime: '12:00',
    });

    component.onSubmit();

    expect(component.form.hasError('invalidTimeRange')).toBe(true);
    expect(schedulingService.createSchedule).not.toHaveBeenCalled();
  });

  it('rejects a slot duration longer than the schedule interval', () => {
    component.form.patchValue({
      doctorId: doctor.id,
      startTime: '10:00',
      endTime: '10:15',
      slotDurationMinutes: 30,
    });

    component.onSubmit();

    expect(component.form.hasError('slotExceedsInterval')).toBe(true);
    expect(schedulingService.createSchedule).not.toHaveBeenCalled();
  });

  it('reports failures when the doctor list cannot be loaded', () => {
    userService.findAll.and.returnValue(throwError(() => new Error('Request failed')));

    component.loadDoctors();

    expect(component.doctorError).toBe('Erro ao carregar a lista de médicos.');
  });
});
