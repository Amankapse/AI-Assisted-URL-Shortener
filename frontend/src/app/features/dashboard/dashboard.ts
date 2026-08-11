import { Component, inject, OnInit } from '@angular/core';
import { AuthStateService } from '../../core/auth/auth-state.service';
import { WorkspaceStateService } from '../../core/workspace/workspace-state.service';
import { LoadingState } from '../../shared/components/loading-state/loading-state';

@Component({
  selector: 'app-dashboard',
  imports: [LoadingState],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  readonly authState = inject(AuthStateService);
  readonly workspaceState = inject(WorkspaceStateService);

  ngOnInit(): void {
    if (this.workspaceState.workspaces().length === 0) {
      this.workspaceState.load();
    }
  }
}
