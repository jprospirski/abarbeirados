// - espelho dos records em uniamerica.abarbeirados.dto.barbeiro

// - o serviço enxuto que vem dentro do barbeiro
export interface ServicoResumo {
  id: number;
  nome: string;
}

export interface Barbeiro {
  id: number;
  nome: string;
  ativo: boolean;
  servicos: ServicoResumo[];
}

// - corpo do post/put /api/barbeiros
export interface BarbeiroRequest {
  nome: string;
  servicoIds: number[];
  ativo?: boolean | null;
}
