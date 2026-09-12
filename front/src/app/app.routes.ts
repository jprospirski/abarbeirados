import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Entrar | Abarbeirados',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'agendamentos' },
      {
        path: 'agendamentos',
        title: 'Agendamentos | Abarbeirados',
        loadComponent: () =>
          import('./features/agenda/agenda.component').then((m) => m.AgendaComponent),
      },
      {
        path: 'agendamentos/novo',
        title: 'Novo agendamento | Abarbeirados',
        loadComponent: () =>
          import('./features/agendamento/agendamento.component').then(
            (m) => m.AgendamentoComponent,
          ),
      },
      {
        path: 'agendamentos/:id/editar',
        title: 'Editar agendamento | Abarbeirados',
        loadComponent: () =>
          import('./features/agendamento/agendamento.component').then(
            (m) => m.AgendamentoComponent,
          ),
      },
      { path: '**', redirectTo: 'agendamentos' },
    ],
  },
];
