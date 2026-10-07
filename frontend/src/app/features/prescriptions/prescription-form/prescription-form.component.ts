import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PrescriptionService, ROUTES } from '../../../core/services/prescription.service';
import { PatientService } from '../../../core/services/patient.service';
import { PharmacyService } from '../../../core/services/pharmacy.service';

@Component({
  selector: 'app-prescription-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './prescription-form.component.html',
  styleUrls: ['./prescription-form.component.scss']
})
export class PrescriptionFormComponent implements OnInit {

  form!: FormGroup;
  saving = false;
  error  = '';

  patients:    { id: string; fullName: string }[] = [];
  medications: { id: string; name: string; unit: string; stock: number }[] = [];

  routes = ROUTES;

  // Pré-selecção por query params
  prePatientId:   string | null = null;
  preEpisodeId:   string | null = null;
  preAdmissionId: string | null = null;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private prescriptionService: PrescriptionService,
    private patientService: PatientService,
    private pharmacyService: PharmacyService
  ) {}

  ngOnInit(): void {
    this.prePatientId   = this.route.snapshot.queryParamMap.get('patientId');
    this.preEpisodeId   = this.route.snapshot.queryParamMap.get('episodeId');
    this.preAdmissionId = this.route.snapshot.queryParamMap.get('admissionId');

    this.buildForm();
    this.loadPatients();
    this.loadMedications();
  }

  buildForm(): void {
    this.form = this.fb.group({
      patientId:    [this.prePatientId  ?? '', Validators.required],
      episodeId:    [this.preEpisodeId  ?? ''],
      admissionId:  [this.preAdmissionId ?? ''],
      diagnosis:    [''],
      notes:        [''],
      validityDays: [30],
      items:        this.fb.array([])
    });

    // Adicionar pelo menos um item
    this.addItem();
  }

  loadPatients(): void {
    this.patientService.findAll('', 0, 200).subscribe({
      next: (p) => {
        this.patients = p.content.map(x => ({ id: x.id, fullName: x.fullName }));
      }
    });
  }

  loadMedications(): void {
    this.pharmacyService.findAllMedications('', 0, 200).subscribe({
      next: (p) => {
        this.medications = p.content.map(m => ({
          id:    m.id,
          name:  m.name,
          unit:  m.unit,
          stock: m.totalAvailable ?? 0
        }));
      }
    });
  }

  get items(): FormArray { return this.form.get('items') as FormArray; }

  addItem(): void {
    this.items.push(this.fb.group({
      medicationId:       ['', Validators.required],
      quantityPrescribed: [1,  [Validators.required, Validators.min(1)]],
      dosage:             ['', Validators.required],
      frequencyHours:     [8],
      durationDays:       [7],
      route:              ['Oral'],
      instructions:       ['']
    }));
  }

  removeItem(i: number): void {
    if (this.items.length > 1) this.items.removeAt(i);
  }

  getMedication(id: string) {
    return this.medications.find(m => m.id === id);
  }

  onSubmit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.error  = '';

    const v = this.form.getRawValue();
    this.prescriptionService.create({
      patientId:    v.patientId,
      episodeId:    v.episodeId   || null,
      admissionId:  v.admissionId || null,
      diagnosis:    v.diagnosis   || null,
      notes:        v.notes       || null,
      validityDays: v.validityDays,
      items: v.items.map((i: any) => ({
        medicationId:       i.medicationId,
        quantityPrescribed: i.quantityPrescribed,
        dosage:             i.dosage,
        frequencyHours:     i.frequencyHours || null,
        durationDays:       i.durationDays   || null,
        route:              i.route          || null,
        instructions:       i.instructions   || null
      }))
    }).subscribe({
      next: (p) => this.router.navigate(['/prescriptions', p.id]),
      error: (err) => {
        this.error  = err.error?.message ?? 'Erro ao criar prescrição.';
        this.saving = false;
      }
    });
  }

  goBack(): void { this.router.navigate(['/prescriptions']); }
  get f() { return this.form.controls; }
}