import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { RuntimeConfigService } from './core/config/runtime-config.service';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { workspaceInterceptor } from './core/interceptors/workspace.interceptor';

export function initializeApplication(): Promise<void> {
  const runtimeConfig = inject(RuntimeConfigService);
  const auth = inject(AuthService);

  return runtimeConfig.load()
    .then(() => firstValueFrom(auth.initialize()))
    .then(() => undefined);
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([authInterceptor, workspaceInterceptor, errorInterceptor])),
    provideRouter(routes),
    provideAppInitializer(initializeApplication)
  ]
};
