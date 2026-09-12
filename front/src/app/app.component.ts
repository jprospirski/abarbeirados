import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/services/auth.service';
import { TemaService } from './core/services/tema.service';

/** Casca da aplicacao: barra lateral fixa + area de rota. */
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
