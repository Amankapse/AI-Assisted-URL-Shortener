import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  templateUrl: './empty-state.html',
  styleUrl: './empty-state.css'
})
export class EmptyState {
  readonly title = input.required<string>();
  readonly detail = input.required<string>();
  readonly actionLabel = input<string | null>(null);
  readonly action = output<void>();
}
