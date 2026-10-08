import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/reports`;

  downloadPatientReport(patientId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/patients/${patientId}`, {
      responseType: 'blob',
    });
  }

  downloadStockReport(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/stock`, {
      responseType: 'blob',
    });
  }

  // Helper: abre o PDF numa nova aba ou faz download
  openOrDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.style.display = 'none';
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
  }
}
