// Espelho dos records em `uniamerica.abarbeirados.dto.barbeiro`.
// Mudou o Java, muda aqui.

/** ServicoResumoResponse — o serviço enxuto que vem dentro do barbeiro. */
export interface ServicoResumo {
  id: number;
  nome: string;
}

/** BarbeiroResponse */
export interface Barbeiro {
  id: number;
  nome: string;
  ativo: boolean;
  servicos: ServicoResumo[];
}

/** BarbeiroRequest — corpo do POST/PUT /api/barbeiros */
export interface BarbeiroRequest {
  nome: string;
  servicoIds: number[];
  ativo?: boolean | null;
}
