// Espelho do record em `uniamerica.abarbeirados.dto.cep`.

/**
 * CepResponse — o que `GET /api/clientes/cep/{cep}` devolve.
 *
 * O backend já converte CEP inexistente em 404, então quem chega aqui com 200
 * tem os campos de endereço preenchidos. `erro` fica no contrato só porque o
 * record do Java expõe o campo.
 */
export interface CepResponse {
  cep: string;
  logradouro: string;
  bairro: string;
  localidade: string;
  uf: string;
  erro?: boolean | null;
}
