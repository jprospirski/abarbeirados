import { Injectable, signal } from '@angular/core';

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
 * Os dados estão em memória de propósito: o domínio Agendamento ainda é stub no
 * backend — controller, service e repository só têm TODO. As regras daqui são
 * as mesmas que o Java vai precisar validar.
 *
 * Para ligar na API é só injetar HttpClient e trocar o corpo dos métodos
 * públicos; as assinaturas já seguem o formato dos endpoints:
 *
 *   listarClientes()         GET    /api/clientes
 *   listarServicos()         GET    /api/servicos?apenasAtivos=true
 *   criarCliente(req)        POST   /api/clientes
 *   agendaDoDia(data)        GET    /api/agendamentos?data=yyyy-MM-dd
 *   criar(req)               POST   /api/agendamentos
 *   atualizarStatus(id, st)  PATCH  /api/agendamentos/{id}/status
 *
 * O proxy.conf.json manda /api para a 8080, então em `ng serve` não há CORS.
 */
@Injectable({ providedIn: 'root' })
export class AgendamentoService {
  readonly abertura = '09:00';
  readonly fechamento = '19:00';
  /** De quantos em quantos minutos a agenda oferece um horário. */
  readonly intervalo = 40;

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

  private readonly _clientes = signal<Cliente[]>(CLIENTES_INICIAIS);
  private readonly _servicos = signal<Servico[]>(SERVICOS_INICIAIS);
  private readonly _agendamentos = signal<Agendamento[]>(agendamentosIniciais());

  private proximoClienteId = 5;
  private proximoAgendamentoId = 4;

  readonly clientes = this._clientes.asReadonly();
  readonly servicos = this._servicos.asReadonly();
  readonly agendamentos = this._agendamentos.asReadonly();

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

  criarCliente(request: ClienteRequest): Cliente {
    // Vazio vira null, nunca string vazia — ver o comentário no ClienteRequest.
    const email = request.email?.trim().toLowerCase() || null;

    // Mesma regra que foi sugerida para o ClienteService do backend.
    if (email && this._clientes().some((c) => c.email?.toLowerCase() === email)) {
      throw new Error(`Já existe um cliente com o e-mail ${email}.`);
    }

    const cliente: Cliente = {
      id: this.proximoClienteId++,
      nome: request.nome.trim(),
      email,
      telefone: request.telefone.trim(),
      dataCadastro: new Date().toISOString().slice(0, 19),
    };

    this._clientes.update((lista) => [...lista, cliente]);
    return cliente;
  }

  /**
   * Cria o agendamento depois de revalidar o conflito de horário.
   *
   * `duracao` e `valor` vêm por parâmetro porque a Química é acertada no
   * atendimento; nos outros serviços eles saem do catálogo.
   */
  criar(request: AgendamentoRequest, duracao: number, valor: number): Agendamento {
    const cliente = this.buscarCliente(request.clienteId);
    if (!cliente) {
      throw new Error('Cliente não encontrado.');
    }

    const servico = this.buscarServico(request.servicoId);
    if (!servico) {
      throw new Error('Combinação de serviços sem cadastro correspondente.');
    }

    const data = dataDe(request.dataHora);
    const inicio = paraMinutos(horaDe(request.dataHora));

    // Confere de novo: outra pessoa pode ter pegado o horário nesse meio-tempo.
    const conflito = this.agendaDoDia(data)
      .filter((a) => a.status !== 'CANCELADO')
      .some((a) =>
        haConflito(inicio, duracao, paraMinutos(horaDe(a.dataHora)), a.duracaoMinutos),
      );

    if (conflito) {
      throw new Error('Esse horário acabou de ser ocupado. Escolha outro na grade.');
    }

    const agendamento: Agendamento = {
      id: this.proximoAgendamentoId++,
      clienteId: cliente.id,
      clienteNome: cliente.nome,
      clienteTelefone: cliente.telefone,
      servicoId: servico.id,
      servicoNome: servico.nome,
      valor,
      duracaoMinutos: duracao,
      dataHora: request.dataHora,
      status: 'AGENDADO',
      observacoes: request.observacoes?.trim() || null,
    };

    this._agendamentos.update((lista) => [...lista, agendamento]);
    return agendamento;
  }

  atualizarStatus(id: number, status: StatusAgendamento): void {
    this._agendamentos.update((lista) =>
      lista.map((a) => (a.id === id ? { ...a, status } : a)),
    );
  }
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
 * O que deve existir na tabela `servico`. Cada combinação vendável é um
 * registro próprio, já que o agendamento aponta para um servicoId só.
 *
 * Os ids abaixo servem só para este mock; nada depende deles. Ao trocar por
 * `GET /api/servicos`, esta lista some e a resolução por nome continua valendo.
 */
const SERVICOS_INICIAIS: Servico[] = [
  { id: 1, nome: 'Corte', valor: 50, duracaoMinutos: 40, ativo: true },
  { id: 2, nome: 'Barba', valor: 30, duracaoMinutos: 20, ativo: true },
  { id: 3, nome: 'Sobrancelha', valor: 20, duracaoMinutos: 20, ativo: true },
  { id: 4, nome: 'Química', valor: 0, duracaoMinutos: 0, ativo: true },
  { id: 5, nome: 'Corte + Barba', valor: 70, duracaoMinutos: 60, ativo: true },
  { id: 6, nome: 'Corte + Sobrancelha', valor: 60, duracaoMinutos: 60, ativo: true },
  { id: 7, nome: 'Barba + Sobrancelha', valor: 40, duracaoMinutos: 40, ativo: true },
  {
    id: 8,
    nome: 'Corte + Barba + Sobrancelha',
    valor: 80,
    duracaoMinutos: 70,
    ativo: true,
  },
];

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

// ------------------------------------------------------ dados de demonstracao

const CLIENTES_INICIAIS: Cliente[] = [
  { id: 1, nome: 'Rafael Moretti', email: 'rafael.moretti@email.com', telefone: '(45) 99812-4477' },
  { id: 2, nome: 'Diego Sanches', email: 'diego.sanches@email.com', telefone: '(45) 99640-1183' },
  { id: 3, nome: 'Vinicius Prado', email: 'vinicius.prado@email.com', telefone: '(45) 99271-0592' },
  { id: 4, nome: 'Otavio Lemes', email: 'otavio.lemes@email.com', telefone: '(45) 99155-8830' },
];

/** Ocupa alguns blocos de hoje para a grade já nascer com conflitos de verdade. */
function agendamentosIniciais(): Agendamento[] {
  const hoje = paraDataIso(new Date());

  return [
    {
      id: 1,
      clienteId: 1,
      clienteNome: 'Rafael Moretti',
      clienteTelefone: '(45) 99812-4477',
      servicoId: 5,
      servicoNome: 'Corte + Barba',
      valor: 70,
      duracaoMinutos: 60,
      dataHora: `${hoje}T10:20`,
      status: 'CONFIRMADO',
      observacoes: 'Prefere máquina 2 nas laterais.',
    },
    // Corte de 40 min às 15:00: termina 15:40 e deixa o bloco seguinte livre.
    {
      id: 2,
      clienteId: 3,
      clienteNome: 'Vinicius Prado',
      clienteTelefone: '(45) 99271-0592',
      servicoId: 1,
      servicoNome: 'Corte',
      valor: 50,
      duracaoMinutos: 40,
      dataHora: `${hoje}T15:00`,
      status: 'AGENDADO',
      observacoes: null,
    },
    {
      id: 3,
      clienteId: 2,
      clienteNome: 'Diego Sanches',
      clienteTelefone: '(45) 99640-1183',
      servicoId: 3,
      servicoNome: 'Sobrancelha',
      valor: 20,
      duracaoMinutos: 20,
      dataHora: `${hoje}T17:00`,
      status: 'AGENDADO',
      observacoes: null,
    },
  ];
}
