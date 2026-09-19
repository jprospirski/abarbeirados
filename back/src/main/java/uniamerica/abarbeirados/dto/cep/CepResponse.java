package uniamerica.abarbeirados.dto.cep;

/**
 * Resposta da ViaCEP.
 *
 * Só os campos que a tela usa para preencher o endereço. Quando o CEP não
 * existe a API devolve `{"erro": true}` e mais nada — por isso `erro` é Boolean
 * e não boolean: nas respostas boas ele vem nulo.
 */
public record CepResponse(
        String cep,
        String logradouro,
        String bairro,
        String localidade,
        String uf,
        Boolean erro
) {
}
