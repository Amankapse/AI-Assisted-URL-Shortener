import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AnnouncementAudience, AnnouncementResponse, AnnouncementSeverity } from '../../../core/api/api-types';
import { SiteExperienceService } from '../../../core/site/site-experience.service';

@Component({
  selector: 'app-site-announcements-admin',
  imports: [ReactiveFormsModule],
  templateUrl: './site-announcements-admin.html',
  styleUrl: '../site-experience-admin/site-admin.css'
})
export class SiteAnnouncementsAdmin implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly site = inject(SiteExperienceService);
  readonly announcements = signal<AnnouncementResponse[]>([]);
  readonly selected = signal<AnnouncementResponse | null>(null);
  readonly message = signal<string | null>(null);
  readonly loading = signal(false);

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(160)]],
    message: ['', [Validators.required, Validators.maxLength(1000)]],
    severity: ['INFO' as AnnouncementSeverity],
    audience: ['PUBLIC' as AnnouncementAudience],
    enabled: [true],
    startAt: [''],
    endAt: [''],
    dismissible: [true]
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.site.adminAnnouncements().subscribe((items) => this.announcements.set(items));
  }

  select(item: AnnouncementResponse): void {
    this.selected.set(item);
    this.form.patchValue({
      title: item.title,
      message: item.message,
      severity: item.severity,
      audience: item.audience,
      enabled: item.enabled,
      startAt: item.startAt ?? '',
      endAt: item.endAt ?? '',
      dismissible: item.dismissible
    });
  }

  clear(): void {
    this.selected.set(null);
    this.form.reset({ title: '', message: '', severity: 'INFO', audience: 'PUBLIC', enabled: true, startAt: '', endAt: '', dismissible: true });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const body = { ...value, startAt: value.startAt || null, endAt: value.endAt || null };
    this.loading.set(true);
    const selected = this.selected();
    const request = selected
      ? this.site.updateAnnouncement(selected.id, body, `"${selected.version}"`)
      : this.site.createAnnouncement(body);
    request.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => {
        this.message.set('Announcement saved.');
        this.clear();
        this.load();
      },
      error: () => this.message.set('Announcement save failed.')
    });
  }
}
