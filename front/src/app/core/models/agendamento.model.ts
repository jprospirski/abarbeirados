/*
 * Contrato do domínio de Agendamento.
 *
 * O backend ainda está em stub, então por enquanto estas interfaces valem como
 * especificação do que a API precisa devolver. Vale manter os mesmos nomes de
 * campo ao escrever o Java — evita ter que mapear na mão depois.
 */

/** Espelha o enum model/StatusAgendamento.java, que também está vazio. */
export type StatusAgendamento =
  | 'AGENDADO'
  | 'CONFIRMADO'
  | 'CONCLUIDO'
  | 'CANCELADO';

export const STATUS_LABEL: Record<StatusAgendamento, string> = {
  AGENDADO: 'Agendado',
  CONFIRMADO: 'Confirmado',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
};

/** Corpo do POST /api/agendamentos. */
export interface AgendamentoRequest {
  clienteId: number;
  servicoId: number;
  /** LocalDateTime em ISO, ex.: 2026-08-16T14:30 */
  dataHora: string;
  observacoes?: string | null;
}

/**
 * O que a listagem precisa receber. `valor` e `duracaoMinutos` são cópia do
 * momento do agendamento, e não o preço atual do serviço — se a tabela mudar,
 * o histórico continua contando a verdade.
 */
export interface Agendamento {
  id: number;
  clienteId: number;
  clienteNome: string;
  clienteTelefone: string;
  servicoId: number;
  servicoNome: string;
  valor: number;
  duracaoMinutos: number;
  dataHora: string;
  status: StatusAgendamento;
  observacoes: string | null;
}

/** Um bloco da grade. O motivo só é preenchido quando está indisponível. */
export interface Horario {
  /** HH:mm */
  hora: string;
  disponivel: boolean;
  motivo?: 'ocupado' | 'passado' | 'sem-tempo';
}
