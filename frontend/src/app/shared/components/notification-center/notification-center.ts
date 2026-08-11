import { Component, inject } from '@angular/core';
import { NotificationService } from '../../../core/error/notification.service';

@Component({
  selector: 'app-notification-center',
  templateUrl: './notification-center.html',
  styleUrl: './notification-center.css'
})
export class NotificationCenter {
  readonly notifications = inject(NotificationService);
}
