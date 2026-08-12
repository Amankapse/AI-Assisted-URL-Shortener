import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AnnouncementResponse } from '../../../core/api/api-types';
import { SiteExperienceService } from '../../../core/site/site-experience.service';

@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css'
})
export class Landing implements OnInit {
  readonly site = inject(SiteExperienceService);
  readonly announcements = signal<AnnouncementResponse[]>([]);

  ngOnInit(): void {
    this.site.loadSettings().subscribe();
    this.site.announcements('PUBLIC').subscribe((items) => this.announcements.set(items));
  }
}
