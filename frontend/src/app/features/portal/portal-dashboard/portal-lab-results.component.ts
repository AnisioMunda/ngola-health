import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { PortalLabResultDto } from '../../../core/services/portal.service';

@Component({
  selector: 'app-portal-lab-results',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './portal-lab-results.component.html',
})
export class PortalLabResultsComponent {
  @Input() results: PortalLabResultDto[] = [];
  @Input() loaded = false;
}
