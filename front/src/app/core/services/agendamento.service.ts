import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, tap, throwError } from 'rxjs';

import {
  Agendamento,
  AgendamentoRequest,
  Horario,
  StatusAgendamento,
} from '../models/agendamento.model';
import {
  ItemCarrinho,
  ItemServico,
  Servico,
  chaveCombinacao,
} from '../models/servico.model';
import {
  dataDe,
  haConflito,
  horaDe,
  paraDataIso,
  paraHora,
  paraMinutos,
} from '../util/data.util';
import { traduzirErro } from '../util/erro.util';

// - fala só com /api/agendamentos; cliente e serviço ficam nos próprios services, por isso resolverServico recebe o catálogo por parâmetro
// - consultas (grade, agenda do dia) leem dos signals e são síncronas; só as escritas devolvem observable
@Injectable({ providedIn: 'root' })
export class AgendamentoService {
  private readonly http = inject(HttpClient);

  readonly abertura = '09:00';
  readonly fechamento = '19:00';
  // - de quantos em quantos minutos a agenda oferece um horário
  readonly intervalo = 40;

  // - o carrinho não tem tabela: a combinação marcada vira um serviço único em resolverServico()
  readonly itensCarrinho: ItemCarrinho[] = [
    { chave: 'CORTE', nome: 'Corte', duracaoMinutos: 40, valor: 50 },
    { chave: 'BARBA', nome: 'Barba', duracaoMinutos: 20, valor: 30 },
    { chave: 'SOBRANCELHA', nome: 'Sobrancelha', duracaoMinutos: 20, valor: 20 },
    { chave: 'QUIMICA', nome: 'Química', duracaoMinutos: 10, valor: 0, exclusivo: true },
  ];

  private readonly _agendamentos = signal<Agendamento[]>([]);
  private readonly _carregando = signal(false);
  private readonly _erroCarga = signal<string | null>(null);

  readonly agendamentos = this._agendamentos.asReadonly();
  readonly carregando = this._carregando.asReadonly();
  readonly erroCarga = this._erroCarga.asReadonly();

  private carregado = false;

  // - a primeira tela que abrir carrega e as outras reaproveitam; forcar recarrega depois de uma escrita
  carregar(forcar = false): void {
    if (this.carregado && !forcar) {
      return;
    }

    this.carregado = true;
    this._carregando.set(true);
    this._erroCarga.set(null);

    this.http
      .get<Agendamento[]>('/api/agendamentos')
      .pipe(catchError(traduzirErro('Não foi possível carregar os agendamentos.')))
      .subscribe({
        next: (agendamentos) => {
          this._agendamentos.set(agendamentos);
          this._carregando.set(false);
        },
        error: () => {
          this._erroCarga.set(
            'Não foi possível falar com o servidor. Confira se o backend está no ar na porta 8080.',
          );
          this._carregando.set(false);
          // - deixa tentar de novo na próxima visita à tela
          this.carregado = false;
        },
      });
  }

  // - traduz a combinação do carrinho para o serviço único do catálogo; valor e duração vêm dele, nunca customizados
  // - busca por nome e não por id: ids são gerados pelo banco, e uma divergência de nome vira undefined visível na tela
  resolverServico(itens: ItemServico[], servicos: Servico[]): Servico | undefined {
    const nome = COMBINACOES[chaveCombinacao(itens)];
    if (!nome) {
      return undefined;
    }

    const alvo = normalizar(nome);
    return servicos.find((s) => normalizar(s.nome) === alvo);
  }

  // - inverso de resolverServico: do nome gravado para os itens do carrinho, usado ao reabrir em edição
  itensDoServico(servicoNome: string): ItemServico[] {
    const alvo = normalizar(servicoNome);
    const chave = Object.entries(COMBINACOES).find(
      ([, nome]) => normalizar(nome) === alvo,
    )?.[0];

    return chave ? (chave.split('|') as ItemServico[]) : [];
  }

  // - quanto sairia item por item, para mostrar a economia do combo
  somaAvulsa(itens: ItemServico[]): number {
    return itens.reduce((total, chave) => {
      const item = this.itensCarrinho.find((i) => i.chave === chave);
      return total + (item?.valor ?? 0);
    }, 0);
  }

  // - em ordem de horário
  agendaDoDia(data: string): Agendamento[] {
    return this._agendamentos()
      .filter((a) => dataDe(a.dataHora) === data)
      .sort((a, b) => a.dataHora.localeCompare(b.dataHora));
  }

  // - grade do dia: bloqueia horário passado, sem tempo até o fechamento ou em conflito com agendamento ativo (cancelado libera)
  // - o conflito usa a duração real, não o bloco: corte das 15:00 (40 min) deixa 15:40 livre
  // - barbeiroId recorta a agenda para um barbeiro; sem ele qualquer barbeiro ocupa o bloco
  horariosDoDia(
    data: string,
    duracaoMinutos: number | null,
    ignorarId?: number,
    barbeiroId?: number | null,
  ): Horario[] {
    const inicio = paraMinutos(this.abertura);
    const fim = paraMinutos(this.fechamento);
    const duracao = duracaoMinutos && duracaoMinutos > 0 ? duracaoMinutos : this.intervalo;

    const ocupados = this.agendaDoDia(data)
      .filter((a) => a.status !== 'CANCELADO' && a.id !== ignorarId)
      .filter((a) => !barbeiroId || a.barbeiroId === barbeiroId)
      .map((a) => ({
        inicio: paraMinutos(horaDe(a.dataHora)),
        duracao: a.duracaoMinutos,
      }));

    const agora = new Date();
    const ehHoje = data === paraDataIso(agora);
    const minutosAgora = agora.getHours() * 60 + agora.getMinutes();

    const grade: Horario[] = [];

    for (let minuto = inicio; minuto < fim; minuto += this.intervalo) {
      const hora = paraHora(minuto);

      if (ehHoje && minuto <= minutosAgora) {
        grade.push({ hora, disponivel: false, motivo: 'passado' });
        continue;
      }

      if (minuto + duracao > fim) {
        grade.push({ hora, disponivel: false, motivo: 'sem-tempo' });
        continue;
      }

      const conflita = ocupados.some((o) =>
        haConflito(minuto, duracao, o.inicio, o.duracao),
      );

      grade.push(
        conflita
          ? { hora, disponivel: false, motivo: 'ocupado' }
          : { hora, disponivel: true },
      );
    }

    return grade;
  }

  // - revalida o conflito no cliente antes de enviar; o backend também checa, mas aqui a mensagem sai na hora
  // - ignorarId tira o próprio agendamento da lista de ocupados durante a edição
  private temConflito(
    dataHora: string,
    duracao: number,
    barbeiroId: number,
    ignorarId?: number,
  ): boolean {
    const inicio = paraMinutos(horaDe(dataHora));

    return this.agendaDoDia(dataDe(dataHora))
      .filter((a) => a.status !== 'CANCELADO' && a.id !== ignorarId)
      .filter((a) => a.barbeiroId === barbeiroId)
      .some((a) =>
        haConflito(inicio, duracao, paraMinutos(horaDe(a.dataHora)), a.duracaoMinutos),
      );
  }

  // - observação em branco vira null, nunca string vazia
  private corpoAgendamento(request: AgendamentoRequest): AgendamentoRequest {
    return {
      clienteId: request.clienteId,
      servicoId: request.servicoId,
      barbeiroId: request.barbeiroId,
      dataHora: request.dataHora,
      observacoes: request.observacoes?.trim() || null,
    };
  }

  // - valor e duração gravados são os que o backend copia do serviço
  criar(request: AgendamentoRequest, duracao: number): Observable<Agendamento> {
    if (this.temConflito(request.dataHora, duracao, request.barbeiroId)) {
      return throwError(
        () => new Error('Esse horário acabou de ser ocupado. Escolha outro na grade.'),
      );
    }

    return this.http
      .post<Agendamento>('/api/agendamentos', this.corpoAgendamento(request))
      .pipe(
        tap((criado) => this._agendamentos.update((lista) => [...lista, criado])),
        catchError(traduzirErro('Não foi possível agendar.')),
      );
  }

  atualizarStatus(id: number, status: StatusAgendamento): Observable<Agendamento> {
    return this.http
      .patch<Agendamento>(`/api/agendamentos/${id}/status`, { status })
      .pipe(
        tap((atualizado) =>
          this._agendamentos.update((lista) =>
            lista.map((a) => (a.id === id ? atualizado : a)),
          ),
        ),
        catchError(traduzirErro('Não foi possível alterar o status.')),
      );
  }

  // - busca direta ao servidor: a edição pode abrir por link antes da listagem carregar
  obterPorId(id: number): Observable<Agendamento> {
    return this.http
      .get<Agendamento>(`/api/agendamentos/${id}`)
      .pipe(catchError(traduzirErro('Não foi possível carregar o agendamento.')));
  }

  atualizar(id: number, request: AgendamentoRequest, duracao: number): Observable<Agendamento> {
    if (this.temConflito(request.dataHora, duracao, request.barbeiroId, id)) {
      return throwError(
        () => new Error('Esse horário acabou de ser ocupado. Escolha outro na grade.'),
      );
    }

    return this.http
      .put<Agendamento>(`/api/agendamentos/${id}`, this.corpoAgendamento(request))
      .pipe(
        tap((atualizado) =>
          this._agendamentos.update((lista) =>
            lista.map((a) => (a.id === id ? atualizado : a)),
          ),
        ),
        catchError(traduzirErro('Não foi possível atualizar o agendamento.')),
      );
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/agendamentos/${id}`).pipe(
      tap(() => this._agendamentos.update((lista) => lista.filter((a) => a.id !== id))),
      catchError(traduzirErro('Não foi possível excluir o agendamento.')),
    );
  }
}

// - compara nomes ignorando caixa e acento
function normalizar(nome: string): string {
  return nome
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '');
}

// - itens ordenados e unidos por '|' apontando para o nome do serviço; precisa bater com a tabela servico
const COMBINACOES: Record<string, string> = {
  CORTE: 'Corte',
  BARBA: 'Barba',
  SOBRANCELHA: 'Sobrancelha',
  QUIMICA: 'Química',
  'BARBA|CORTE': 'Corte + Barba',
  'CORTE|SOBRANCELHA': 'Corte + Sobrancelha',
  'BARBA|SOBRANCELHA': 'Barba + Sobrancelha',
  'BARBA|CORTE|SOBRANCELHA': 'Corte + Barba + Sobrancelha',
};
