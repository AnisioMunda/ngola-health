import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PharmacyService, MedicationResponse, DOSAGE_FORM_LABELS } from '../../../core/services/pharmacy.service';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-medications-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './medications-list.component.html',
  styleUrls: ['./medications-list.component.scss']
})
export class MedicationsListComponent implements OnInit {
  medications: MedicationResponse[] = [];
  loading = true;
  error = '';
  searchQuery = '';
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;
  formLabels = DOSAGE_FORM_LABELS;
  private searchSubject = new Subject<string>();

  constructor(private pharmacyService: PharmacyService, private router: Router) {}

  ngOnInit(): void {
    this.loadMedications();
    this.searchSubject.pipe(debounceTime(400), distinctUntilChanged())
      .subscribe(() => { this.currentPage = 0; this.loadMedications(); });
  }

  loadMedications(): void {
    this.loading = true;
    this.pharmacyService.findAllMedications(this.searchQuery, this.currentPage, this.pageSize).subscribe({
      next: (page) => { this.medications = page.content; this.totalElements = page.totalElements; this.totalPages = page.totalPages; this.loading = false; },
      error: () => { this.error = 'Failed to load medications.'; this.loading = false; }
    });
  }

  onSearch(): void { this.searchSubject.next(this.searchQuery); }
  goToMedication(id: string): void { this.router.navigate(['/pharmacy', id]); }
  goToAdd(): void { this.router.navigate(['/pharmacy/new']); }
  goToExpiring(): void { this.router.navigate(['/pharmacy/expiring']); }
  isLowStock(med: MedicationResponse): boolean { return med.totalAvailable <= med.minStockLevel; }
  prevPage(): void { if (this.currentPage > 0) { this.currentPage--; this.loadMedications(); } }
  nextPage(): void { if (this.currentPage < this.totalPages - 1) { this.currentPage++; this.loadMedications(); } }
}