import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.scss'],
})
export class ReportsComponent {
  constructor(public router: Router) {}

  exportPatients(): void {
    alert('Relatório de pacientes em PDF — implementar com iText 7');
  }
  exportEpisodes(): void {
    alert('Relatório de consultas em PDF — implementar com iText 7');
  }
  exportFinancial(): void {
    alert('Relatório financeiro em PDF — implementar com iText 7');
  }
  exportPharmacy(): void {
    alert('Relatório de farmácia em PDF — implementar com iText 7');
  }
  exportLab(): void {
    alert('Relatório laboratorial em PDF — implementar com iText 7');
  }
}
