import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { LabService, LabTestResponse, Priority } from '../../../core/services/lab.service';
import { PatientService } from '../../../core/services/patient.service';

@Component({
  selector: 'app-lab-request-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './lab-request-form.component.html',
  styleUrls: ['./lab-request-form.component.scss']
})
export class LabRequestFormComponent implements OnInit {

  form!: FormGroup;
  saving = false;
  error = '';

  patients: { id: string; fullName: string }[] = [];
  availableTests: LabTestResponse[] = [];
  selectedTestIds: Set<string> = new Set();

  priorities: { value: Priority; label: string }[] = [
    { value: 'NORMAL', label: 'Normal' },
    { value: 'URGENT', label: 'Urgent' },
    { value: 'STAT',   label: 'STAT (Immediate)' }
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private labService: LabService,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      patientId:     ['', Validators.required],
      priority:      ['NORMAL', Validators.required],
      clinicalNotes: ['']
    });

    this.loadPatients();
    this.loadTests();
  }

  loadPatients(): void {
    this.patientService.findAll('', 0, 100).subscribe({
      next: (page) => {
        this.patients = page.content.map(p => ({ id: p.id, fullName: p.fullName }));
      }
    });
  }

  loadTests(): void {
    this.labService.findAllTests().subscribe({
      next: (tests) => { this.availableTests = tests; }
    });
  }

  toggleTest(testId: string): void {
    if (this.selectedTestIds.has(testId)) {
      this.selectedTestIds.delete(testId);
    } else {
      this.selectedTestIds.add(testId);
    }
  }

  isTestSelected(testId: string): boolean {
    return this.selectedTestIds.has(testId);
  }

  onSubmit(): void {
    if (this.form.invalid || this.selectedTestIds.size === 0) {
      this.form.markAllAsTouched();
      if (this.selectedTestIds.size === 0) {
        this.error = 'Select at least one test.';
      }
      return;
    }

    this.saving = true;
    this.error = '';

    this.labService.createRequest({
      ...this.form.value,
      labTestIds: Array.from(this.selectedTestIds)
    }).subscribe({
      next: () => this.router.navigate(['/lab']),
      error: (err) => {
        this.saving = false;
        this.error = err.error?.message ?? 'Failed to create lab request.';
      }
    });
  }

  goBack(): void { this.router.navigate(['/lab']); }

  get f() { return this.form.controls; }
}