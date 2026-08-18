import { Routes } from '@angular/router';

export const routes: Routes = [
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
  { path: '**', redirectTo: 'agendamentos' },
];
