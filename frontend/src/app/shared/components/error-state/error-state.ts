import { Component, input, output } from '@angular/core';
import { ProblemDetails } from '../../../core/error/problem-details.model';

@Component({
  selector: 'app-error-state',
  templateUrl: './error-state.html',
  styleUrl: './error-state.css'
})
export class ErrorState {
  readonly problem = input.required<ProblemDetails>();
  readonly retry = output<void>();
}
