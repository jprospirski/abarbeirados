package uniamerica.abarbeirados.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import uniamerica.abarbeirados.dto.servico.ServicoRequest;
import uniamerica.abarbeirados.dto.servico.ServicoResponse;
import uniamerica.abarbeirados.model.Servico;

class ServicoMapperTest {

    private final ServicoMapper mapper = new ServicoMapper();

    @ParameterizedTest
    @CsvSource(value = {"NULL,true", "true,true", "false,false"}, nullValues = "NULL")
    void forEntity_quandoAtivoInformadoOuOmitido_copiaCamposEAplicaPadrao(Boolean ativo, boolean esperado) {
        // Arrange
        ServicoRequest request = novoRequest(ativo);

        // Act
        Servico resultado = mapper.forEntity(request);

        // Assert
        assertNull(resultado.getId());
        assertEquals(request.nome(), resultado.getNome());
        assertEquals(request.valor(), resultado.getValor());
        assertEquals(request.duracaoMinutos(), resultado.getDuracaoMinutos());
        assertEquals(esperado, resultado.getAtivo());
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,true", "true,true", "false,false"}, nullValues = "NULL")
    void updateEntity_quandoDadosMudam_atualizaCamposEPreservaId(Boolean ativo, boolean esperado) {
        // Arrange
        ServicoRequest request = novoRequest(ativo);
        Servico servico = Servico.builder().id(4L).nome("Anterior").valor(BigDecimal.TEN)
                .duracaoMinutos(10).ativo(false).build();

        // Act
        mapper.updateEntity(request, servico);

        // Assert
        assertEquals(4L, servico.getId());
        assertEquals(request.nome(), servico.getNome());
        assertEquals(request.valor(), servico.getValor());
        assertEquals(request.duracaoMinutos(), servico.getDuracaoMinutos());
        assertEquals(esperado, servico.getAtivo());
    }

    @Test
    void forResponse_quandoServicoCompleto_retornaTodosOsCampos() {
        // Arrange
        BigDecimal valor = new BigDecimal("55.00");
        Servico servico = Servico.builder().id(4L).nome("Corte").valor(valor)
                .duracaoMinutos(45).ativo(false).build();
        ServicoResponse esperado = new ServicoResponse(4L, "Corte", valor, 45, false);

        // Act
        ServicoResponse resultado = mapper.forResponse(servico);

        // Assert
        assertEquals(esperado, resultado);
    }

    private ServicoRequest novoRequest(Boolean ativo) {
        return new ServicoRequest("Corte", new BigDecimal("55.00"), 45, ativo);
    }
}
