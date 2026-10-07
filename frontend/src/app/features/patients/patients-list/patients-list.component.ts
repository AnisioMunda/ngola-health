import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PatientService, PatientResponse } from '../../../core/services/patient.service';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-patients-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './patients-list.component.html',
  styleUrls: ['./patients-list.component.scss'],
})
export class PatientsListComponent implements OnInit {
  patients: PatientResponse[] = [];
  loading = true;
  error = '';
  searchQuery = '';
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  private searchSubject = new Subject<string>();

  constructor(
    private patientService: PatientService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadPatients();
    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged()).subscribe(() => {
      this.currentPage = 0;
      this.loadPatients();
    });
  }

  loadPatients(): void {
    this.loading = true;
    this.patientService.findAll(this.searchQuery, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.patients = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load patients.';
        this.loading = false;
      },
    });
  }

  onSearch(): void {
    this.searchSubject.next(this.searchQuery);
  }

  goToCreate(): void {
    this.router.navigate(['/patients/new']);
  }

  goToDetail(id: string): void {
    this.router.navigate(['/patients', id]);
  }

  goToEdit(id: string, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/patients', id, 'edit']);
  }

  getGenderLabel(gender: string): string {
    return gender === 'MALE' ? 'Male' : 'Female';
  }

  getBloodTypeBadge(type: string): string {
    return type || '—';
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadPatients();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadPatients();
    }
  }
}
