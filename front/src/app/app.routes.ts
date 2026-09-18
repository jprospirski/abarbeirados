import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

/**
 * Um `children` por domínio, todos atrás do authGuard.
 *
 * O aninhamento não é decorativo: o guard fica declarado uma vez, no pai, e
 * cada domínio agrupa suas três rotas (lista, novo, editar) no mesmo prefixo —
 * `/clientes`, `/clientes/novo`, `/clientes/:id/editar`.
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'agendamentos' },
  {
    path: 'agendamentos',
    title: 'Agendamentos | Abarbeirados',
    loadComponent: () =>
      import('./features/agenda/agenda.component').then((m) => m.AgendaComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'agendamentos' },
      {
        path: 'agendamentos',
        children: [
          {
            path: '',
            title: 'Agendamentos | Abarbeirados',
            loadComponent: () =>
              import('./features/agenda/agenda.component').then((m) => m.AgendaComponent),
          },
          {
            path: 'novo',
            title: 'Novo agendamento | Abarbeirados',
            loadComponent: () =>
              import('./features/agendamento/agendamento.component').then(
                (m) => m.AgendamentoComponent,
              ),
          },
          {
            path: ':id/editar',
            title: 'Editar agendamento | Abarbeirados',
            loadComponent: () =>
              import('./features/agendamento/agendamento.component').then(
                (m) => m.AgendamentoComponent,
              ),
          },
        ],
      },
      {
        path: 'clientes',
        children: [
          {
            path: '',
            title: 'Clientes | Abarbeirados',
            loadComponent: () =>
              import('./features/cliente/cliente-lista/cliente-lista.component').then(
                (m) => m.ClienteListaComponent,
              ),
          },
          {
            path: 'novo',
            title: 'Novo cliente | Abarbeirados',
            loadComponent: () =>
              import('./features/cliente/cliente-form/cliente-form.component').then(
                (m) => m.ClienteFormComponent,
              ),
          },
          {
            path: ':id/editar',
            title: 'Editar cliente | Abarbeirados',
            loadComponent: () =>
              import('./features/cliente/cliente-form/cliente-form.component').then(
                (m) => m.ClienteFormComponent,
              ),
          },
        ],
      },
      {
        path: 'servicos',
        children: [
          {
            path: '',
            title: 'Serviços | Abarbeirados',
            loadComponent: () =>
              import('./features/servico/servico-lista/servico-lista.component').then(
                (m) => m.ServicoListaComponent,
              ),
          },
          {
            path: 'novo',
            title: 'Novo serviço | Abarbeirados',
            loadComponent: () =>
              import('./features/servico/servico-form/servico-form.component').then(
                (m) => m.ServicoFormComponent,
              ),
          },
          {
            path: ':id/editar',
            title: 'Editar serviço | Abarbeirados',
            loadComponent: () =>
              import('./features/servico/servico-form/servico-form.component').then(
                (m) => m.ServicoFormComponent,
              ),
          },
        ],
      },
      {
        path: 'barbeiros',
        children: [
          {
            path: '',
            title: 'Barbeiros | Abarbeirados',
            loadComponent: () =>
              import('./features/barbeiro/barbeiro-lista/barbeiro-lista.component').then(
                (m) => m.BarbeiroListaComponent,
              ),
          },
          {
            path: 'novo',
            title: 'Novo barbeiro | Abarbeirados',
            loadComponent: () =>
              import('./features/barbeiro/barbeiro-form/barbeiro-form.component').then(
                (m) => m.BarbeiroFormComponent,
              ),
          },
          {
            path: ':id/editar',
            title: 'Editar barbeiro | Abarbeirados',
            loadComponent: () =>
              import('./features/barbeiro/barbeiro-form/barbeiro-form.component').then(
                (m) => m.BarbeiroFormComponent,
              ),
          },
        ],
      },
      { path: '**', redirectTo: 'agendamentos' },
    ],
  },
  { path: '**', redirectTo: 'agendamentos' },
];
