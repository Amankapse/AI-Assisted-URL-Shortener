import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-tag-chip',
  templateUrl: './tag-chip.html',
  styleUrl: './tag-chip.css'
})
export class TagChip {
  readonly tag = input.required<string>();
  readonly removable = input(false);
  readonly removed = output<string>();
}
