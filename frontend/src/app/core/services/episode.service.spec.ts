import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { EpisodeService } from './episode.service';

describe('EpisodeService', () => {
  let service: EpisodeService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/episodes`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [EpisodeService],
    });
    service = TestBed.inject(EpisodeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('applies status and pagination filters to episode searches', () => {
    service.findAll(undefined, undefined, 'SCHEDULED', 1, 10).subscribe();

    const request = httpMock.expectOne(
      (candidate) =>
        candidate.url === apiUrl &&
        candidate.params.get('status') === 'SCHEDULED' &&
        candidate.params.get('page') === '1' &&
        candidate.params.get('size') === '10',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], totalElements: 0, totalPages: 0, number: 1, size: 10 });
  });

  it('starts an episode through its lifecycle endpoint', () => {
    service.start('episode-1').subscribe();

    const request = httpMock.expectOne(`${apiUrl}/episode-1/start`);
    expect(request.request.method).toBe('PATCH');
    request.flush({});
  });
});
