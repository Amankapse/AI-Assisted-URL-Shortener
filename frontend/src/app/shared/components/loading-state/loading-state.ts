import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading-state',
  template: '<p class="loading">{{ label() }}</p>',
  styles: ['.loading { color: #475569; margin: 0; }']
})
export class LoadingState {
  readonly label = input('Loading...');
}
