import { Injectable, signal } from '@angular/core';
import { ProblemDetails } from './problem-details.model';

export interface NotificationMessage {
  id: number;
  tone: 'info' | 'error';
  title: string;
  detail?: string;
  correlationId?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private nextId = 1;
  private readonly messagesSignal = signal<NotificationMessage[]>([]);

  readonly messages = this.messagesSignal.asReadonly();

  showError(problem: ProblemDetails): void {
    this.messagesSignal.update((messages) => [
      ...messages,
      {
        id: this.nextId++,
        tone: 'error',
        title: problem.title ?? 'Request failed',
        detail: problem.detail,
        correlationId: problem.correlationId
      }
    ]);
  }

  showInfo(title: string, detail?: string): void {
    this.messagesSignal.update((messages) => [...messages, { id: this.nextId++, tone: 'info', title, detail }]);
  }

  dismiss(id: number): void {
    this.messagesSignal.update((messages) => messages.filter((message) => message.id !== id));
  }
}
