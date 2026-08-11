import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { AuthStateService } from '../core/auth/auth-state.service';
import { WorkspaceStateService } from '../core/workspace/workspace-state.service';
import { NotificationCenter } from '../shared/components/notification-center/notification-center';

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationCenter],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.css'
})
export class AppShell {
  readonly authState = inject(AuthStateService);
  readonly auth = inject(AuthService);
  readonly workspaceState = inject(WorkspaceStateService);
}
