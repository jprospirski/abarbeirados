import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { TemaService } from './core/services/tema.service';

/** Casca da aplicacao: barra lateral fixa + area de rota. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  protected readonly nome = 'Abarbeirados';
  protected readonly tema = inject(TemaService);
}
