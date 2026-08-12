import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { SiteExperienceService } from '../../../core/site/site-experience.service';

@Component({
  selector: 'app-site-experience-admin',
  imports: [ReactiveFormsModule],
  templateUrl: './site-experience-admin.html',
  styleUrl: './site-admin.css'
})
export class SiteExperienceAdmin implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly site = inject(SiteExperienceService);
  readonly loading = signal(false);
  readonly message = signal<string | null>(null);
  private etag = '"0"';

  readonly form = this.fb.nonNullable.group({
    brandName: ['', [Validators.required, Validators.maxLength(80)]],
    tagline: ['', [Validators.required, Validators.maxLength(160)]],
    supportEmail: ['', [Validators.email, Validators.maxLength(320)]],
    supportUrl: ['', [Validators.maxLength(500)]],
    contactText: ['', [Validators.maxLength(1000)]],
    primaryColor: ['#185A9D', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    secondaryColor: ['#123047', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    accentColor: ['#0F766E', [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
    footerDescription: ['', [Validators.required, Validators.maxLength(500)]],
    footerCopyright: ['', [Validators.required, Validators.maxLength(160)]],
    logoAssetId: [''],
    logoDarkAssetId: [''],
    faviconAssetId: [''],
    loginBackgroundAssetId: [''],
    landingHeroAssetId: ['']
  });

  ngOnInit(): void {
    this.loading.set(true);
    this.site.adminSettings().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (resource) => {
        this.etag = resource.etag ?? '"0"';
        this.form.patchValue({
          brandName: resource.body.brandName,
          tagline: resource.body.tagline,
          supportEmail: resource.body.supportEmail ?? '',
          supportUrl: resource.body.supportUrl ?? '',
          contactText: resource.body.contactText ?? '',
          primaryColor: resource.body.primaryColor,
          secondaryColor: resource.body.secondaryColor,
          accentColor: resource.body.accentColor,
          footerDescription: resource.body.footerDescription,
          footerCopyright: resource.body.footerCopyright,
          logoAssetId: resource.body.logo?.id ?? '',
          logoDarkAssetId: resource.body.logoDark?.id ?? '',
          faviconAssetId: resource.body.favicon?.id ?? '',
          loginBackgroundAssetId: resource.body.loginBackground?.id ?? '',
          landingHeroAssetId: resource.body.landingHero?.id ?? ''
        });
      },
      error: () => this.message.set('Unable to load site settings.')
    });
  }

  save(): void {
    this.message.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.message.set('Fix validation errors before saving.');
      return;
    }
    const value = this.form.getRawValue();
    this.loading.set(true);
    this.site.updateSettings({
      ...value,
      supportEmail: value.supportEmail || null,
      supportUrl: value.supportUrl || null,
      contactText: value.contactText || null,
      logoAssetId: value.logoAssetId || null,
      logoDarkAssetId: value.logoDarkAssetId || null,
      faviconAssetId: value.faviconAssetId || null,
      loginBackgroundAssetId: value.loginBackgroundAssetId || null,
      landingHeroAssetId: value.landingHeroAssetId || null
    }, this.etag).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (resource) => {
        this.etag = resource.etag ?? this.etag;
        this.message.set('Site experience settings saved.');
      },
      error: () => this.message.set('Save failed. Refresh and check the current version if another admin changed this content.')
    });
  }
}
