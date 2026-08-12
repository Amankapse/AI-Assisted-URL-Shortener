import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AdminContentPageResponse, ContentPageKey } from '../../../core/api/api-types';
import { SiteExperienceService } from '../../../core/site/site-experience.service';

@Component({
  selector: 'app-site-content-admin',
  imports: [ReactiveFormsModule],
  templateUrl: './site-content-admin.html',
  styleUrl: '../site-experience-admin/site-admin.css'
})
export class SiteContentAdmin implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly site = inject(SiteExperienceService);
  readonly pages = signal<AdminContentPageResponse[]>([]);
  readonly selected = signal<AdminContentPageResponse | null>(null);
  readonly loading = signal(false);
  readonly message = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(160)]],
    summary: ['', [Validators.required, Validators.maxLength(500)]],
    content: ['', [Validators.required, Validators.maxLength(8000)]],
    status: ['DRAFT' as 'DRAFT' | 'PUBLISHED', [Validators.required]]
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.site.adminPages().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (pages) => {
        this.pages.set(pages);
        if (!this.selected() && pages.length > 0) {
          this.select(pages[0]);
        }
      },
      error: () => this.message.set('Unable to load content pages.')
    });
  }

  select(page: AdminContentPageResponse): void {
    this.selected.set(page);
    this.form.patchValue({
      title: page.title,
      summary: page.summary,
      content: page.content,
      status: page.status
    });
  }

  save(): void {
    const page = this.selected();
    if (!page) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.site.updatePage(page.pageKey as ContentPageKey, this.form.getRawValue(), `"${page.version}"`)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (resource) => {
          this.selected.set(resource.body);
          this.message.set('Content page saved.');
          this.load();
        },
        error: () => this.message.set('Save failed. Refresh before retrying if another admin changed this page.')
      });
  }
}
