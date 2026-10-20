// - espelho dos records em uniamerica.abarbeirados.dto.cliente
export interface Cliente {
  id: number;
  nome: string;
  email: string | null;
  telefone: string;
  dataCadastro?: string;
}

// - email vai como null quando não informado, nunca string vazia: vários null convivem num unique, strings vazias colidem
export interface ClienteRequest {
  nome: string;
  email: string | null;
  telefone: string;
}
