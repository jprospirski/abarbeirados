// - espelho do record em uniamerica.abarbeirados.dto.cep
// - cep inexistente já vira 404 no backend; erro fica no contrato só porque o record expõe o campo
export interface CepResponse {
  cep: string;
  logradouro: string;
  bairro: string;
  localidade: string;
  uf: string;
  erro?: boolean | null;
}
