import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/services/auth.service';
import { TemaService } from './core/services/tema.service';

// - a lateral só aparece com sessão aberta: no login o menu só ofereceria links que o guard devolveria
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  private readonly router = inject(Router);

  protected readonly nome = 'Abarbeirados';
  protected readonly tema = inject(TemaService);
  protected readonly auth = inject(AuthService);

  protected sair(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
