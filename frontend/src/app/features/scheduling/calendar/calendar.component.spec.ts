import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { CalendarEvent, SchedulingService } from '../../../core/services/scheduling.service';
import {
  UserManagementService,
  UserResponse,
} from '../../../core/services/user-management.service';
import { CalendarComponent } from './calendar.component';

describe('CalendarComponent', () => {
  let component: CalendarComponent;
  let schedulingService: jasmine.SpyObj<SchedulingService>;
  let userService: jasmine.SpyObj<UserManagementService>;

  const doctor: UserResponse = {
    id: 'doctor-1',
    fullName: 'Dr. Silva',
    username: 'doctor.silva',
    email: '',
    phone: '',
    especiality: '',
    professionalCard: '',
    teamsUserId: null,
    registerStatus: 'ACTIVE',
    mustChangePassword: false,
    lastLogin: '',
    createdAt: '',
    roles: ['DOCTOR'],
  };

  const firstEvent: CalendarEvent = {
    id: 'appointment-1',
    doctorId: 'doctor-1',
    title: 'Ana',
    date: '2026-05-21',
    startTime: '10:00',
    endTime: '10:30',
    patientName: 'Ana',
    doctorName: 'Dr. Silva',
    status: 'SCHEDULED',
    color: '#3b82f6',
  };

  beforeEach(() => {
    schedulingService = jasmine.createSpyObj<SchedulingService>('SchedulingService', [
      'getCalendar',
    ]);
    userService = jasmine.createSpyObj<UserManagementService>('UserManagementService', ['findAll']);
    schedulingService.getCalendar.and.returnValue(of([firstEvent]));
    userService.findAll.and.returnValue(
      of({ content: [doctor], totalElements: 1, totalPages: 1, number: 0, size: 100 }),
    );

    TestBed.configureTestingModule({
      imports: [CalendarComponent],
      providers: [
        { provide: SchedulingService, useValue: schedulingService },
        { provide: UserManagementService, useValue: userService },
        { provide: Router, useValue: jasmine.createSpyObj<Router>('Router', ['navigate']) },
      ],
    });
    const fixture = TestBed.createComponent(CalendarComponent);
    component = fixture.componentInstance;
    component.currentDate = new Date(2026, 4, 1);
    fixture.detectChanges();
  });

  it('requests month boundaries as local dates and maps events to the correct day', () => {
    expect(schedulingService.getCalendar).toHaveBeenCalledWith('2026-05-01', '2026-05-31');
    expect(component.weeks.flat().find((day) => day.dateStr === firstEvent.date)?.events).toEqual([
      firstEvent,
    ]);
  });

  it('filters calendar events by doctor ID instead of display name', () => {
    const secondDoctorEvent: CalendarEvent = {
      ...firstEvent,
      id: 'appointment-2',
      doctorId: 'doctor-2',
    };
    component.events = [firstEvent, secondDoctorEvent];
    component.selectedDoctorId = 'doctor-2';

    component.onDoctorFilter();

    expect(component.weeks.flat().find((day) => day.dateStr === firstEvent.date)?.events).toEqual([
      secondDoctorEvent,
    ]);
  });
});
