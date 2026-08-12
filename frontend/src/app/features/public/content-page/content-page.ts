import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ContentPageKey, PublicContentPageResponse } from '../../../core/api/api-types';
import { SiteExperienceService } from '../../../core/site/site-experience.service';

@Component({
  selector: 'app-public-content-page',
  imports: [RouterLink],
  templateUrl: './content-page.html',
  styleUrl: './content-page.css'
})
export class ContentPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  readonly site = inject(SiteExperienceService);
  readonly page = signal<PublicContentPageResponse | null>(null);
  readonly failed = signal(false);

  ngOnInit(): void {
    this.site.loadSettings().subscribe();
    const pageKey = this.route.snapshot.data['pageKey'] as ContentPageKey;
    this.site.page(pageKey).subscribe({
      next: (page) => this.page.set(page),
      error: () => this.failed.set(true)
    });
  }
}
