import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  AuditService, AuditLogDto, AuditStatsDto,
  AuditAction, EntityType,
  ACTION_LABELS, ENTITY_LABELS, RESULT_COLORS, ACTION_ICONS
} from '../../../core/services/audit.service';

@Component({
  selector: 'app-audit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit.component.html',
  styleUrls: ['./audit.component.scss']
})
export class AuditComponent implements OnInit {

  logs: AuditLogDto[] = [];
  stats: AuditStatsDto | null = null;
  loading      = true;
  loadingStats = true;
  error        = '';

  // Filtros
  actionFilter:     AuditAction | '' = '';
  entityTypeFilter: EntityType  | '' = '';
  dateFrom = '';
  dateTo   = '';

  totalElements = 0;
  totalPages    = 0;
  currentPage   = 0;

  actionLabels  = ACTION_LABELS;
  entityLabels  = ENTITY_LABELS;
  resultColors  = RESULT_COLORS;
  actionIcons   = ACTION_ICONS;

  actions: { value: AuditAction | ''; label: string }[] = [
    { value: '',             label: 'Todas as acções'  },
    { value: 'CREATE',       label: 'Criação'          },
    { value: 'UPDATE',       label: 'Actualização'     },
    { value: 'DELETE',       label: 'Eliminação'       },
    { value: 'LOGIN',        label: 'Login'            },
    { value: 'LOGIN_FAILED', label: 'Login Falhado'    },
    { value: 'PRINT',        label: 'Impressão'        },
    { value: 'APPROVE',      label: 'Aprovação'        },
    { value: 'REJECT',       label: 'Rejeição'         }
  ];

  entityTypes: { value: EntityType | ''; label: string }[] = [
    { value: '',           label: 'Todas as entidades' },
    { value: 'PATIENT',    label: 'Paciente'           },
    { value: 'EPISODE',    label: 'Episódio'           },
    { value: 'LAB_REQUEST',label: 'Laboratório'        },
    { value: 'MEDICATION', label: 'Medicamento'        },
    { value: 'INVOICE',    label: 'Factura'            },
    { value: 'APPOINTMENT',label: 'Agendamento'        },
    { value: 'ADMISSION',  label: 'Internamento'       },
    { value: 'USER',       label: 'Utilizador'         }
  ];

  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    this.load();
    this.loadStats();
  }

  load(): void {
    this.loading = true;
    this.auditService.findAll(
      undefined,
      this.actionFilter     || undefined,
      this.entityTypeFilter || undefined,
      this.dateFrom ? this.dateFrom + 'T00:00:00Z' : undefined,
      this.dateTo   ? this.dateTo   + 'T23:59:59Z' : undefined,
      this.currentPage
    ).subscribe({
      next: (page) => {
        this.logs          = page.content;
        this.totalElements = page.totalElements;
        this.totalPages    = page.totalPages;
        this.loading = false;
      },
      error: () => { this.error = 'Erro ao carregar logs.'; this.loading = false; }
    });
  }

  loadStats(): void {
    this.loadingStats = true;
    this.auditService.getStats().subscribe({
      next: (s) => { this.stats = s; this.loadingStats = false; },
      error: () => { this.loadingStats = false; }
    });
  }

  onFilterChange(): void { this.currentPage = 0; this.load(); }
  clearFilters():  void {
    this.actionFilter = ''; this.entityTypeFilter = '';
    this.dateFrom = ''; this.dateTo = '';
    this.onFilterChange();
  }

  prevPage(): void { if (this.currentPage > 0) { this.currentPage--; this.load(); } }
  nextPage(): void { if (this.currentPage < this.totalPages - 1) { this.currentPage++; this.load(); } }

  resultClass(result: string): string {
    return { SUCCESS: 'success', FAILURE: 'failure', UNAUTHORIZED: 'unauthorized' }[result] ?? '';
  }

  timeAgo(dateStr: string): string {
    const diff  = Date.now() - new Date(dateStr).getTime();
    const mins  = Math.floor(diff / 60000);
    const hours = Math.floor(mins  / 60);
    const days  = Math.floor(hours / 24);
    if (days  > 0) return days  + 'd';
    if (hours > 0) return hours + 'h';
    if (mins  > 0) return mins  + 'min';
    return 'agora';
  }
}