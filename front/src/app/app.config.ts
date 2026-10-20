import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import localePt from '@angular/common/locales/pt';
import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';

// - sem isto o pipe currency escreve "brl 45.00" em vez de "r$ 45,00"
registerLocaleData(localePt);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(),
    // - exigido pelos componentes do mdb (ripple, form-control): sem isto as transições ficam mudas
    provideAnimations(),
    { provide: LOCALE_ID, useValue: 'pt-BR' },
  ],
};
