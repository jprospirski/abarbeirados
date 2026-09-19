package uniamerica.abarbeirados.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import uniamerica.abarbeirados.dto.cep.CepResponse;

/**
 * Integração externa com a ViaCEP.
 *
 * A interface é só o contrato: o Feign gera a implementação no boot, a partir
 * do {@code @EnableFeignClients} da classe principal.
 */
@FeignClient(name = "viacep", url = "https://viacep.com.br/ws")
public interface CepClient {

    @GetMapping("/{cep}/json")
    CepResponse buscar(@PathVariable("cep") String cep);
}
