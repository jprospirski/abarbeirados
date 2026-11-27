package uniamerica.abarbeirados.service;

import org.springframework.stereotype.Service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uniamerica.abarbeirados.client.CepClient;
import uniamerica.abarbeirados.dto.cep.CepResponse;
import uniamerica.abarbeirados.exception.NegocioException;
import uniamerica.abarbeirados.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CepService {

    private final CepClient cepClient;

    /**
     * Consulta o CEP na ViaCEP.
     *
     * Sem @Transactional de propósito: não toca no banco, só sai para fora. Os
     * dois modos de falha viram erro tratado pelo GlobalException — CEP que não
     * existe é 404, ViaCEP fora do ar é 400 com mensagem legível, e não o 500
     * que a FeignException daria se vazasse.
     */
    public CepResponse buscar(String cep) {
        try {
            CepResponse resposta = cepClient.buscar(cep.replaceAll("\\D", ""));

            if (Boolean.TRUE.equals(resposta.erro())) {
                throw new ResourceNotFoundException("CEP não encontrado: " + cep);
            }

            return resposta;
        } catch (FeignException ex) {
            log.warn("Falha ao consultar o CEP {} na ViaCEP", cep, ex);
            throw new NegocioException("Não foi possível consultar o CEP no momento.");
        }
    }
}
