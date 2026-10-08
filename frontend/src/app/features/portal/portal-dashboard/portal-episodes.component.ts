import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { PortalEpisodeDto } from '../../../core/services/portal.service';
import { portalStatusColor } from './portal-dashboard.utils';

@Component({
  selector: 'app-portal-episodes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './portal-episodes.component.html',
})
export class PortalEpisodesComponent {
  @Input() episodes: PortalEpisodeDto[] = [];

  statusColor = portalStatusColor;
}
