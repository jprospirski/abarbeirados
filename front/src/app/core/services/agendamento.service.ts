import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, forkJoin, map, tap, throwError } from 'rxjs';

import {
  Agendamento,
  AgendamentoRequest,
  Horario,
  StatusAgendamento,
} from '../models/agendamento.model';
import { Cliente, ClienteRequest } from '../models/cliente.model';
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

/**
 * Camada de dados da tela de agendamento.
 *
 * Os dados vêm da API. As três listas ficam em signals carregados uma vez por
 * `carregar()`, e as consultas abaixo (grade de horários, agenda do dia) leem
 * desses signals — por isso continuam síncronas e podem ser usadas direto no
 * template. Só as escritas devolvem Observable.
 *
 *   listarClientes()         GET    /api/clientes
 *   listarServicos()         GET    /api/servicos?apenasAtivos=true
 *   criarCliente(req)        POST   /api/clientes
 *   criar(req, duracao)      POST   /api/agendamentos
 *   atualizarStatus(id, st)  PATCH  /api/agendamentos/{id}/status
 *
 * O proxy.conf.json manda /api para a 8080, então em `ng serve` não há CORS.
 */
@Injectable({ providedIn: 'root' })
export class AgendamentoService {
  private readonly http = inject(HttpClient);

  readonly abertura = '09:00';
  readonly fechamento = '19:00';
  /** De quantos em quantos minutos a agenda oferece um horário. */
  readonly intervalo = 40;

  /*
   * O carrinho é conceito de tela e não tem tabela no banco: o atendente marca
   * itens soltos e a combinação é traduzida para um Serviço único em
   * resolverServico(). Por isso esta lista continua fixa aqui.
   */
  readonly itensCarrinho: ItemCarrinho[] = [
    { chave: 'CORTE', nome: 'Corte', duracaoMinutos: 40, valor: 50 },
    { chave: 'BARBA', nome: 'Barba', duracaoMinutos: 20, valor: 30 },
    { chave: 'SOBRANCELHA', nome: 'Sobrancelha', duracaoMinutos: 20, valor: 20 },
    {
      chave: 'QUIMICA',
      nome: 'Química',
      duracaoMinutos: 0,
      valor: 0,
      personalizado: true,
      exclusivo: true,
    },
  ];

  private readonly _clientes = signal<Cliente[]>([]);
  private readonly _servicos = signal<Servico[]>([]);
  private readonly _agendamentos = signal<Agendamento[]>([]);
  private readonly _carregando = signal(false);
  private readonly _erroCarga = signal<string | null>(null);

  readonly clientes = this._clientes.asReadonly();
  readonly servicos = this._servicos.asReadonly();
  readonly agendamentos = this._agendamentos.asReadonly();
  readonly carregando = this._carregando.asReadonly();
  readonly erroCarga = this._erroCarga.asReadonly();

  private carregado = false;

  // ------------------------------------------------------------------- carga

  /**
   * Puxa clientes, serviços e agendamentos de uma vez.
   *
   * As telas chamam sem argumento, então a primeira que abrir carrega e a
   * segunda reaproveita. `forcar` serve para recarregar depois de uma escrita.
   */
  carregar(forcar = false): void {
    if (this.carregado && !forcar) {
      return;
    }

    this.carregado = true;
    this._carregando.set(true);
    this._erroCarga.set(null);

    forkJoin({
      clientes: this.http.get<Cliente[]>('/api/clientes'),
      servicos: this.http.get<Servico[]>('/api/servicos?apenasAtivos=true'),
      agendamentos: this.http.get<Agendamento[]>('/api/agendamentos'),
    }).subscribe({
      next: ({ clientes, servicos, agendamentos }) => {
        this._clientes.set(clientes);
        this._servicos.set(servicos);
        this._agendamentos.set(agendamentos);
        this._carregando.set(false);
      },
      error: () => {
        this._erroCarga.set(
          'Não foi possível falar com o servidor. Confira se o backend está no ar na porta 8080.',
        );
        this._carregando.set(false);
        // Deixa tentar de novo na próxima visita à tela.
        this.carregado = false;
      },
    });
  }

  // ------------------------------------------------------- catalogo/combos

  /**
   * Traduz a combinação marcada na tela para o Serviço único que vai ao banco.
   * Devolve undefined se a combinação não tiver linha cadastrada.
   *
   * A busca é por NOME, e não por id, porque os ids quem gera é o banco. Um
   * mapa de ids fixos aqui casaria com a linha errada se a ordem de inserção
   * mudasse — e agendaria o serviço errado sem erro nenhum. Por nome, uma
   * divergência devolve undefined e a tela avisa que a combinação não tem
   * cadastro, que é uma falha visível.
   */
  resolverServico(itens: ItemServico[]): Servico | undefined {
    const nome = COMBINACOES[chaveCombinacao(itens)];
    if (!nome) {
      return undefined;
    }

    const alvo = normalizar(nome);
    return this._servicos().find((s) => normalizar(s.nome) === alvo);
  }

  /** Quanto sairia comprando item por item — serve para mostrar a economia. */
  somaAvulsa(itens: ItemServico[]): number {
    return itens.reduce((total, chave) => {
      const item = this.itensCarrinho.find((i) => i.chave === chave);
      return total + (item?.valor ?? 0);
    }, 0);
  }

  // --------------------------------------------------------------- consultas

  buscarCliente(id: number): Cliente | undefined {
    return this._clientes().find((c) => c.id === id);
  }

  buscarServico(id: number): Servico | undefined {
    return this._servicos().find((s) => s.id === id);
  }

  /** Em ordem de horário. */
  agendaDoDia(data: string): Agendamento[] {
    return this._agendamentos()
      .filter((a) => dataDe(a.dataHora) === data)
      .sort((a, b) => a.dataHora.localeCompare(b.dataHora));
  }

  /**
   * Grade do dia, de {@link intervalo} em {@link intervalo} minutos.
   *
   * Um horário fica indisponível quando já passou, quando o serviço não caberia
   * antes do fechamento, ou quando conflita com um agendamento ativo — cancelar
   * libera a vaga de volta.
   *
   * Quem decide o conflito é a duração real, não o tamanho do bloco: um corte
   * das 15:00 leva 40 min, termina 15:40 e deixa o bloco das 15:40 livre.
   */
  horariosDoDia(data: string, duracaoMinutos: number | null): Horario[] {
    const inicio = paraMinutos(this.abertura);
    const fim = paraMinutos(this.fechamento);
    const duracao = duracaoMinutos && duracaoMinutos > 0 ? duracaoMinutos : this.intervalo;

    const ocupados = this.agendaDoDia(data)
      .filter((a) => a.status !== 'CANCELADO')
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

  // ---------------------------------------------------------------- comandos

  criarCliente(request: ClienteRequest): Observable<Cliente> {
    const corpo: ClienteRequest = {
      nome: request.nome.trim(),
      // Vazio vira null, nunca string vazia — ver o comentário no ClienteRequest.
      email: request.email?.trim().toLowerCase() || null,
      telefone: request.telefone.trim(),
    };

    return this.http.post<Cliente>('/api/clientes', corpo).pipe(
      tap((cliente) => this._clientes.update((lista) => [...lista, cliente])),
      catchError(traduzirErro('Não foi possível cadastrar o cliente.')),
    );
  }

  /**
   * Cria o agendamento depois de revalidar o conflito de horário.
   *
   * A checagem de conflito é feita aqui porque o backend ainda não valida
   * sobreposição — está anotado como pendência no README. `duracao` vem por
   * parâmetro só para essa conta; o valor e a duração gravados são os que o
   * backend copia do serviço.
   */
  criar(request: AgendamentoRequest, duracao: number): Observable<Agendamento> {
    const data = dataDe(request.dataHora);
    const inicio = paraMinutos(horaDe(request.dataHora));

    const conflito = this.agendaDoDia(data)
      .filter((a) => a.status !== 'CANCELADO')
      .some((a) =>
        haConflito(inicio, duracao, paraMinutos(horaDe(a.dataHora)), a.duracaoMinutos),
      );

    if (conflito) {
      return throwError(
        () => new Error('Esse horário acabou de ser ocupado. Escolha outro na grade.'),
      );
    }

    const corpo: AgendamentoRequest = {
      clienteId: request.clienteId,
      servicoId: request.servicoId,
      dataHora: request.dataHora,
      observacoes: request.observacoes?.trim() || null,
    };

    return this.http.post<Agendamento>('/api/agendamentos', corpo).pipe(
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
}

/**
 * Converte o ApiError do backend em Error com a mensagem que ele mandou.
 *
 * O GlobalException devolve sempre {status, error, message, fields}; quando é
 * erro de validação, `fields` traz campo a campo, e é essa a mensagem útil.
 */
function traduzirErro(padrao: string) {
  return (resposta: unknown): Observable<never> => {
    const corpo = (resposta as { error?: ApiError })?.error;

    const detalhe = corpo?.fields
      ? Object.values(corpo.fields).join(' ')
      : corpo?.message;

    return throwError(() => new Error(detalhe || padrao));
  };
}

interface ApiError {
  message?: string;
  fields?: Record<string, string> | null;
}

// -------------------------------------------------------------- catalogo

/** Compara nomes ignorando caixa e acento. */
function normalizar(nome: string): string {
  return nome
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '');
}

/**
 * Itens ordenados e unidos por '|' apontando para o NOME do Serviço.
 * Os nomes precisam bater com os cadastrados na tabela `servico`.
 */
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
