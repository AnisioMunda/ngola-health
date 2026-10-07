import { Routes } from '@angular/router';
import { authGuard, publicGuard } from './core/guards/auth.guard';
import { portalRoutes } from './portal.routes';

export const routes: Routes = [
  ...portalRoutes,
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
  },
  // Página pública — login (sem shell)
  {
    path: 'login',
    canActivate: [publicGuard],
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },

  // Aplicação protegida — com shell (menu lateral)
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      // Dashboard
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },

      // Pacientes
      {
        path: 'patients',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/patients/patients-list/patients-list.component').then(
                (m) => m.PatientsListComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/patients/patient-form/patient-form.component').then(
                (m) => m.PatientFormComponent,
              ),
          },
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./features/patients/patient-form/patient-form.component').then(
                (m) => m.PatientFormComponent,
              ),
          },
        ],
      },

      // Episódios / Consultas
      {
        path: 'episodes',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/episodes/episodes-list/episodes-list.component').then(
                (m) => m.EpisodesListComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/episodes/episode-form/episode-form.component').then(
                (m) => m.EpisodeFormComponent,
              ),
          },
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./features/episodes/episode-form/episode-form.component').then(
                (m) => m.EpisodeFormComponent,
              ),
          },
        ],
      },

      // Laboratório
      {
        path: 'lab',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/laboratory/lab-requests-list/lab-requests-list.component').then(
                (m) => m.LabRequestsListComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/laboratory/lab-request-form/lab-request-form.component').then(
                (m) => m.LabRequestFormComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/laboratory/lab-request-detail/lab-request-detail.component').then(
                (m) => m.LabRequestDetailComponent,
              ),
          },
        ],
      },

      // Farmácia
      {
        path: 'pharmacy',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/pharmacy/medications-list/medications-list.component').then(
                (m) => m.MedicationsListComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/pharmacy/medication-form/medication-form.component').then(
                (m) => m.MedicationFormComponent,
              ),
          },
          {
            path: 'expiring',
            loadComponent: () =>
              import('./features/pharmacy/expiring-stock/expiring-stock.component').then(
                (m) => m.ExpiringStockComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/pharmacy/medication-detail/medication-detail.component').then(
                (m) => m.MedicationDetailComponent,
              ),
          },
        ],
      },

      // Facturação
      {
        path: 'financial',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/financial/invoices-list/invoices-list.component').then(
                (m) => m.InvoicesListComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/financial/invoice-form/invoice-form.component').then(
                (m) => m.InvoiceFormComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/financial/invoice-detail/invoice-detail.component').then(
                (m) => m.InvoiceDetailComponent,
              ),
          },
        ],
      },

      // Agendamento
      {
        path: 'scheduling',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/scheduling/appointments-list/appointments-list.component').then(
                (m) => m.AppointmentsListComponent,
              ),
          },
          {
            path: 'calendar',
            loadComponent: () =>
              import('./features/scheduling/calendar/calendar.component').then(
                (m) => m.CalendarComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/scheduling/appointment-form/appointment-form.component').then(
                (m) => m.AppointmentFormComponent,
              ),
          },
          {
            path: 'schedules',
            loadComponent: () =>
              import('./features/scheduling/doctor-schedules/doctor-schedules.component').then(
                (m) => m.DoctorSchedulesComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/scheduling/appointment-detail/appointment-detail.component').then(
                (m) => m.AppointmentDetailComponent,
              ),
          },
        ],
      },
      // Internamentos — Sprint 8
      {
        path: 'inpatient',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/inpatient/ward-map/ward-map.component').then(
                (m) => m.WardMapComponent,
              ),
          },
          {
            path: 'admissions',
            loadComponent: () =>
              import('./features/inpatient/admissions-list/admissions-list.component').then(
                (m) => m.AdmissionsListComponent,
              ),
          },
          {
            path: 'admissions/new',
            loadComponent: () =>
              import('./features/inpatient/admission-form/admission-form.component').then(
                (m) => m.AdmissionFormComponent,
              ),
          },
          {
            path: 'admissions/:id',
            loadComponent: () =>
              import('./features/inpatient/admission-detail/admission-detail.component').then(
                (m) => m.AdmissionDetailComponent,
              ),
          },
          {
            path: 'setup',
            loadComponent: () =>
              import('./features/inpatient/ward-setup/ward-setup.component').then(
                (m) => m.WardSetupComponent,
              ),
          },
        ],
      },
      // Recursos Humanos — Sprint 10
      {
        path: 'hr',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/hr/hr-dashboard/hr-dashboard.component').then(
                (m) => m.HrDashboardComponent,
              ),
          },
          {
            path: 'shifts',
            loadComponent: () =>
              import('./features/hr/shifts/shifts.component').then((m) => m.ShiftsComponent),
          },
          {
            path: 'leaves',
            loadComponent: () =>
              import('./features/hr/leaves/leaves.component').then((m) => m.LeavesComponent),
          },
          {
            path: 'attendance',
            loadComponent: () =>
              import('./features/hr/attendance/attendance.component').then(
                (m) => m.AttendanceComponent,
              ),
          },
        ],
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notifications/notifications.component').then(
            (m) => m.NotificationsComponent,
          ),
      },
      // Prescrições — Sprint 11
      {
        path: 'prescriptions',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/prescriptions/prescriptions-list/prescriptions-list.component').then(
                (m) => m.PrescriptionsListComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/prescriptions/prescription-form/prescription-form.component').then(
                (m) => m.PrescriptionFormComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/prescriptions/prescription-detail/prescription-detail.component').then(
                (m) => m.PrescriptionDetailComponent,
              ),
          },
        ],
      },
      // Triagem — Sprint 12
      {
        path: 'triage',
        loadComponent: () =>
          import('./features/triage/triage/triage.component').then((m) => m.TriageComponent),
      },
      // Auditoria
      {
        path: 'audit',
        loadComponent: () =>
          import('./features/audit/audit/audit.component').then((m) => m.AuditComponent),
      },

      // Relatórios
      {
        path: 'reports',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/reports/reports.component').then((m) => m.ReportsComponent),
          },
          {
            path: 'executive',
            loadComponent: () =>
              import('./features/reports/executive-dashboard/executive-dashboard.component').then(
                (m) => m.ExecutiveDashboardComponent,
              ),
          },
        ],
      },

      // Utilizadores
      {
        path: 'users',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/users/users-list/users-list.component').then(
                (m) => m.UsersListComponent,
              ),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./features/users/user-form/user-form.component').then(
                (m) => m.UserFormComponent,
              ),
          },
          {
            path: ':id/edit',
            loadComponent: () =>
              import('./features/users/user-form/user-form.component').then(
                (m) => m.UserFormComponent,
              ),
          },
        ],
      },

      {
        path: 'equipment',
        loadComponent: () =>
          import('./features/equipment/equipment/equipment.component').then(
            (m) => m.EquipmentComponent,
          ),
      },

      {
        path: 'telemedicine',
        loadComponent: () =>
          import('./features/telemedicine/telemedicine/telemedicine.component').then(
            (m) => m.TelemedicineComponent,
          ),
      },

      // Redirect padrão
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  // Fallback
  { path: '**', redirectTo: 'dashboard' },
];
