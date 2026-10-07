import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  EpisodeService,
  EpisodeResponse,
  EpisodeStatus,
  EPISODE_TYPE_LABELS,
  EPISODE_STATUS_LABELS
} from '../../../core/services/episode.service';

@Component({
  selector: 'app-episodes-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './episodes-list.component.html',
  styleUrls: ['./episodes-list.component.scss']
})
export class EpisodesListComponent implements OnInit {

  episodes: EpisodeResponse[] = [];
  loading = true;
  error = '';

  statusFilter: EpisodeStatus | '' = '';
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  typeLabels = EPISODE_TYPE_LABELS;
  statusLabels = EPISODE_STATUS_LABELS;

  statuses: { value: EpisodeStatus | ''; label: string }[] = [
    { value: '',            label: 'All statuses' },
    { value: 'SCHEDULED',  label: 'Scheduled' },
    { value: 'IN_PROGRESS',label: 'In Progress' },
    { value: 'COMPLETED',  label: 'Completed' },
    { value: 'CANCELLED',  label: 'Cancelled' }
  ];

  constructor(
    private episodeService: EpisodeService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadEpisodes();
  }

  loadEpisodes(): void {
    this.loading = true;
    this.episodeService.findAll(
      undefined, undefined,
      this.statusFilter || undefined,
      this.currentPage, this.pageSize
    ).subscribe({
      next: (page) => {
        this.episodes = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load episodes.';
        this.loading = false;
      }
    });
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadEpisodes();
  }

  goToCreate(): void {
    this.router.navigate(['/episodes/new']);
  }

  goToDetail(id: string): void {
    this.router.navigate(['/episodes', id]);
  }

  changeStatus(episode: EpisodeResponse, action: 'start' | 'complete' | 'cancel', event: Event): void {
    event.stopPropagation();
    const obs = action === 'start'    ? this.episodeService.start(episode.id)
              : action === 'complete' ? this.episodeService.complete(episode.id)
              :                         this.episodeService.cancel(episode.id);

    obs.subscribe({
      next: (updated) => {
        const idx = this.episodes.findIndex(e => e.id === updated.id);
        if (idx !== -1) this.episodes[idx] = updated;
      },
      error: (err) => {
        this.error = err.error?.message ?? 'Failed to update episode.';
      }
    });
  }

  prevPage(): void {
    if (this.currentPage > 0) { this.currentPage--; this.loadEpisodes(); }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) { this.currentPage++; this.loadEpisodes(); }
  }
}