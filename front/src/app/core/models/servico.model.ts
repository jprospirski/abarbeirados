// Espelho dos records em `uniamerica.abarbeirados.dto.servico`.

/** ServicoResponse. `valor` é BigDecimal no Java e chega como number no JSON. */
export interface Servico {
  id: number;
  nome: string;
  valor: number;
  duracaoMinutos: number;
  ativo: boolean;
}

/* -----------------------------------------------------------------------------
   Carrinho — daqui para baixo é conceito só do frontend.

   Na tela o atendente marca vários itens (corte, barba, sobrancelha), mas o
   backend guarda um servicoId por agendamento. Então a combinação escolhida é
   traduzida para um Serviço único antes de virar requisição, e cada combinação
   vendável corresponde a uma linha da tabela `servico`.
-------------------------------------------------------------------------------- */

export type ItemServico = 'CORTE' | 'BARBA' | 'SOBRANCELHA' | 'QUIMICA';

export interface ItemCarrinho {
  chave: ItemServico;
  nome: string;
  duracaoMinutos: number;
  valor: number;
  /** Duração e valor são acertados no atendimento (caso da Química). */
  personalizado?: boolean;
  /** Não combina com os outros itens. */
  exclusivo?: boolean;
}

/** Ordena e junta os itens para virar chave do mapa: 'BARBA|CORTE'. */
export function chaveCombinacao(itens: ItemServico[]): string {
  return [...itens].sort().join('|');
}
