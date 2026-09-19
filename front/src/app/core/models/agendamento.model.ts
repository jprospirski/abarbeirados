// - espelho dos records em uniamerica.abarbeirados.dto.agendamento e do enum StatusAgendamento
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

// - corpo do post/put /api/agendamentos
export interface AgendamentoRequest {
  clienteId: number;
  servicoId: number;
  barbeiroId: number;
  // - localdatetime em iso, ex.: 2026-08-16t14:30
  dataHora: string;
  observacoes?: string | null;
}

// - valor e duracaoMinutos são cópia do momento do agendamento, não o preço atual do serviço
export interface Agendamento {
  id: number;
  clienteId: number;
  clienteNome: string;
  clienteTelefone: string;
  servicoId: number;
  servicoNome: string;
  barbeiroId: number;
  barbeiroNome: string;
  valor: number;
  duracaoMinutos: number;
  dataHora: string;
  status: StatusAgendamento;
  observacoes: string | null;
}

// - um bloco da grade; o motivo só vem preenchido quando está indisponível
export interface Horario {
  // - hh:mm
  hora: string;
  disponivel: boolean;
  motivo?: 'ocupado' | 'passado' | 'sem-tempo';
}
