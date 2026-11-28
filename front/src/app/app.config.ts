import { registerLocaleData } from '@angular/common';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import localePt from '@angular/common/locales/pt';
import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { meuhttpInterceptor } from './features/auth/http-interceptor.service';

// - sem isto o pipe currency escreve "brl 45.00" em vez de "r$ 45,00"
registerLocaleData(localePt);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    // - o interceptor envia o token jwt em toda chamada à api e trata 401/403
    provideHttpClient(withInterceptors([meuhttpInterceptor])),
    // - exigido pelos componentes do mdb (ripple, form-control): sem isto as transições ficam mudas
    provideAnimations(),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
  ],
};
