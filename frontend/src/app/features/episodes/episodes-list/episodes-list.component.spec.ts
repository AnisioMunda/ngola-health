import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { EpisodeResponse, EpisodeService } from '../../../core/services/episode.service';
import { EpisodesListComponent } from './episodes-list.component';

describe('EpisodesListComponent', () => {
  let component: EpisodesListComponent;
  let episodeService: jasmine.SpyObj<EpisodeService>;
  let router: jasmine.SpyObj<Router>;

  const scheduledEpisode: EpisodeResponse = {
    id: 'episode-1',
    patientId: 'patient-1',
    patientName: 'Ana Silva',
    doctorId: '',
    doctorName: '',
    episodeType: 'OUTPATIENT',
    status: 'SCHEDULED',
    scheduledAt: '',
    startedAt: '',
    completedAt: '',
    reason: '',
    symptoms: '',
    diagnosis: '',
    prescription: '',
    notes: '',
    bloodPressure: '',
    heartRate: 0,
    temperature: 0,
    weightKg: 0,
    createdAt: '',
  };

  beforeEach(() => {
    episodeService = jasmine.createSpyObj<EpisodeService>('EpisodeService', [
      'findAll',
      'start',
      'complete',
      'cancel',
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    episodeService.findAll.and.returnValue(
      of({
        content: [scheduledEpisode],
        totalElements: 1,
        totalPages: 1,
        number: 0,
        size: 20,
      }),
    );

    TestBed.configureTestingModule({
      imports: [EpisodesListComponent],
      providers: [
        { provide: EpisodeService, useValue: episodeService },
        { provide: Router, useValue: router },
      ],
    });
    component = TestBed.createComponent(EpisodesListComponent).componentInstance;
    component.ngOnInit();
  });

  it('carrega episódios e filtra por estado', () => {
    expect(episodeService.findAll).toHaveBeenCalledWith(undefined, undefined, undefined, 0, 20);
    expect(component.episodes).toEqual([scheduledEpisode]);
    expect(component.statuses[0].label).toBe('Todos os estados');
    expect(component.statusLabels.SCHEDULED).toBe('Agendado');
    expect(component.typeLabels.OUTPATIENT).toBe('Consulta externa');

    component.statusFilter = 'COMPLETED';
    component.currentPage = 2;
    component.onFilterChange();

    expect(episodeService.findAll).toHaveBeenCalledWith(undefined, undefined, 'COMPLETED', 0, 20);
  });

  it('abre o formulário de edição ao seleccionar um episódio', () => {
    component.goToEdit(scheduledEpisode.id);

    expect(router.navigate).toHaveBeenCalledWith(['/episodes', 'episode-1', 'edit']);
  });

  it('actualiza o episódio quando muda o estado', () => {
    const updated = { ...scheduledEpisode, status: 'IN_PROGRESS' as const };
    const event = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);
    component.episodes = [scheduledEpisode];
    episodeService.start.and.returnValue(of(updated));

    component.changeStatus(scheduledEpisode, 'start', event);

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(episodeService.start).toHaveBeenCalledWith(scheduledEpisode.id);
    expect(component.episodes[0].status).toBe('IN_PROGRESS');
    expect(component.updatingEpisodeId).toBeNull();
  });

  it('apresenta explicitamente falhas de carregamento', () => {
    episodeService.findAll.and.returnValue(
      throwError(() => ({ error: { detail: 'Falha de serviço.' } })),
    );

    component.loadEpisodes();

    expect(component.error).toBe('Falha de serviço.');
    expect(component.loading).toBe(false);
  });
});
