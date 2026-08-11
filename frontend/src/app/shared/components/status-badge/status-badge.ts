import { Component, computed, input } from '@angular/core';
import { UrlState } from '../../../core/api/api-types';
import { urlStatePresentation } from '../../utilities/url-state-presentation';

@Component({
  selector: 'app-status-badge',
  templateUrl: './status-badge.html',
  styleUrl: './status-badge.css'
})
export class StatusBadge {
  readonly state = input.required<UrlState>();
  readonly presentation = computed(() => urlStatePresentation(this.state()));
}
