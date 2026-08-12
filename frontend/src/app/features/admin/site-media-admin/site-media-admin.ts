import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { MediaAssetResponse } from '../../../core/api/api-types';
import { SiteExperienceService } from '../../../core/site/site-experience.service';

@Component({
  selector: 'app-site-media-admin',
  imports: [ReactiveFormsModule],
  templateUrl: './site-media-admin.html',
  styleUrl: '../site-experience-admin/site-admin.css'
})
export class SiteMediaAdmin implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly site = inject(SiteExperienceService);
  readonly media = signal<MediaAssetResponse[]>([]);
  readonly loading = signal(false);
  readonly message = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    url: ['', [Validators.required, Validators.pattern(/^https:\/\/.+/)]],
    altText: ['', [Validators.required, Validators.maxLength(200)]],
    sourceName: ['', [Validators.maxLength(120)]],
    sourceUrl: ['', [Validators.pattern(/^$|^https:\/\/.+/)]],
    license: ['', [Validators.maxLength(160)]],
    attribution: ['', [Validators.maxLength(500)]],
    enabled: [true]
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.site.adminMedia().subscribe((items) => this.media.set(items));
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.loading.set(true);
    this.site.registerMedia({
      ...value,
      sourceName: value.sourceName || null,
      sourceUrl: value.sourceUrl || null,
      license: value.license || null,
      attribution: value.attribution || null
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => {
        this.message.set('Media asset registered.');
        this.form.reset({ name: '', url: '', altText: '', sourceName: '', sourceUrl: '', license: '', attribution: '', enabled: true });
        this.load();
      },
      error: () => this.message.set('Media asset save failed. Use HTTPS raster images and safe metadata.')
    });
  }
}
