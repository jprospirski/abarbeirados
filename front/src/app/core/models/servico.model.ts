export interface Servico {
  id: number;
  nome: string;
  valor: number;
  duracaoMinutos: number;
  ativo: boolean;
}

export interface ServicoRequest {
  nome: string;
  valor: number;
  duracaoMinutos: number;
  ativo?: boolean | null;
}

export type ItemServico = 'CORTE' | 'BARBA' | 'SOBRANCELHA' | 'QUIMICA';

export interface ItemCarrinho {
  chave: ItemServico;
  nome: string;
  duracaoMinutos: number;
  valor: number;
  exclusivo?: boolean;
}

export function chaveCombinacao(itens: ItemServico[]): string {
  return [...itens].sort().join('|');
}
