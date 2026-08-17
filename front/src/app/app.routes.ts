import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'agendamentos' },
  {
    path: 'agendamentos',
    title: 'Agendamentos | Abarbeirados',
    loadComponent: () =>
      import('./features/agendamento/agendamento.component').then(
        (m) => m.AgendamentoComponent,
      ),
  },
  // TODO: rota 'clientes' — a segunda tela da entrega, ainda não desenvolvida.
  { path: '**', redirectTo: 'agendamentos' },
];
