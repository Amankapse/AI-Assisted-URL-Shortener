import { Component, inject, input, signal } from '@angular/core';
import { NotificationService } from '../../../core/error/notification.service';

@Component({
  selector: 'app-copy-button',
  templateUrl: './copy-button.html',
  styleUrl: './copy-button.css'
})
export class CopyButton {
  readonly value = input.required<string>();
  readonly label = input('Copy');
  readonly copied = signal(false);
  private readonly notifications = inject(NotificationService);

  async copy(): Promise<void> {
    await navigator.clipboard.writeText(this.value());
    this.copied.set(true);
    this.notifications.showInfo('Copied', 'Short URL copied to clipboard.');
    window.setTimeout(() => this.copied.set(false), 1800);
  }
}
