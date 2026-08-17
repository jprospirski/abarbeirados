// Espelho dos records em `uniamerica.abarbeirados.dto.cliente`.
// Mudou o Java, muda aqui.

/** ClienteResponse */
export interface Cliente {
  id: number;
  nome: string;
  email: string | null;
  telefone: string;
  dataCadastro?: string;
}

/**
 * ClienteRequest — corpo do POST /api/clientes
 *
 * `email` vai como null quando não for informado, e nunca como string vazia:
 * se a coluna ganhar `unique`, vários NULL convivem numa restrição de unicidade,
 * mas várias strings vazias colidem entre si.
 */
export interface ClienteRequest {
  nome: string;
  email: string | null;
  telefone: string;
}
