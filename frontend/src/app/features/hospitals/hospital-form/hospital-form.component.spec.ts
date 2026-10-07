import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { HospitalManagementService } from '../../../core/services/hospital-management.service';
import { HospitalFormComponent } from './hospital-form.component';

describe('HospitalFormComponent', () => {
  let component: HospitalFormComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HospitalFormComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({}) } },
        },
        { provide: HospitalManagementService, useValue: {} },
      ],
    });

    component = TestBed.createComponent(HospitalFormComponent).componentInstance;
  });

  it('validates required fields, institutional code, and optional email', () => {
    component.form.patchValue({
      name: 'Hospital Geral de Luanda',
      code: 'hgl-001',
      type: 'HOSPITAL',
      province: 'Luanda',
      email: '',
    });
    component.normalizeCode();

    expect(component.form.controls['code'].value).toBe('HGL-001');
    expect(component.form.valid).toBeTrue();

    component.form.controls['code'].setValue('invalid code');
    expect(component.form.controls['code'].invalid).toBeTrue();
  });

  it('rejects an invalid optional email address', () => {
    component.form.patchValue({
      name: 'Hospital Geral de Luanda',
      code: 'HGL-001',
      type: 'HOSPITAL',
      province: 'Luanda',
      email: 'endereco-invalido',
    });

    expect(component.form.controls['email'].invalid).toBeTrue();
  });

  it('does not accept whitespace-only required fields', () => {
    component.form.patchValue({ name: '   ', code: 'HGL-001', type: 'HOSPITAL', province: '  ' });

    expect(component.form.controls['name'].invalid).toBeTrue();
    expect(component.form.controls['province'].invalid).toBeTrue();
  });
});
