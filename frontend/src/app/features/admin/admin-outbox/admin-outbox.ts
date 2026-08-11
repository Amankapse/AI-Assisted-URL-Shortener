import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { debounceTime } from 'rxjs';
import { AdminApi } from '../../../core/api/admin-api.service';
import { OutboxEventSummaryResponse, OutboxEventType, OutboxPageResponse, OutboxStatus } from '../../../core/api/api-types';
import { ProblemDetails } from '../../../core/error/problem-details.model';
import { ProblemDetailsService } from '../../../core/error/problem-details.service';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ErrorState } from '../../../shared/components/error-state/error-state';

const STATUSES: OutboxStatus[] = ['PENDING', 'PROCESSING', 'PROCESSED', 'DEAD'];
const EVENT_TYPES: OutboxEventType[] = ['URL_CREATED','URL_DESTINATION_CHANGED','URL_EXPIRATION_CHANGED','URL_ENABLED','URL_DISABLED','URL_DELETED','URL_BLOCKED','URL_UNBLOCKED','URL_CACHE_INVALIDATION_REQUIRED','CLICK_RECORDED'];

@Component({
  selector: 'app-admin-outbox',
  imports: [DatePipe, ReactiveFormsModule, EmptyState, ErrorState],
  templateUrl: './admin-outbox.html',
  styleUrl: './admin-outbox.css'
})
export class AdminOutbox {
  private readonly api = inject(AdminApi);
  private readonly problems = inject(ProblemDetailsService);
  private readonly fb = inject(FormBuilder);

  readonly result = signal<OutboxPageResponse | null>(null);
  readonly loading = signal(false);
  readonly problem = signal<ProblemDetails | null>(null);
  readonly statuses = STATUSES;
  readonly eventTypes = EVENT_TYPES;
  readonly filters = this.fb.nonNullable.group({
    status: [''],
    eventType: [''],
    from: [''],
    to: [''],
    size: [20]
  });

  constructor() {
    this.filters.valueChanges.pipe(debounceTime(300)).subscribe(() => this.load(0));
    this.load();
  }

  load(page = this.result()?.page ?? 0): void {
    const value = this.filters.getRawValue();
    this.loading.set(true);
    this.problem.set(null);
    this.api.outbox({
      status: value.status as OutboxStatus || null,
      eventType: value.eventType as OutboxEventType || null,
      from: value.from || null,
      to: value.to || null,
      page,
      size: Number(value.size)
    }).subscribe({
      next: (result) => { this.result.set(result); this.loading.set(false); },
      error: (error) => { this.problem.set(this.problems.fromHttpError(error)); this.loading.set(false); }
    });
  }

  trackEvent(event: OutboxEventSummaryResponse): string {
    return event.eventId;
  }
}
