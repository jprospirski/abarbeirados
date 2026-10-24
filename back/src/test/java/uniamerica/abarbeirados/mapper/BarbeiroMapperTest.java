package uniamerica.abarbeirados.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import uniamerica.abarbeirados.dto.barbeiro.BarbeiroRequest;
import uniamerica.abarbeirados.dto.barbeiro.BarbeiroResponse;
import uniamerica.abarbeirados.dto.barbeiro.ServicoResumoResponse;
import uniamerica.abarbeirados.model.Barbeiro;
import uniamerica.abarbeirados.model.Servico;

class BarbeiroMapperTest {

    private final BarbeiroMapper mapper = new BarbeiroMapper();
    private Servico servico;
    private Set<Servico> servicos;

    @BeforeEach
    void prepararDados() {
        servico = Servico.builder().id(3L).nome("Corte").build();
        servicos = Set.of(servico);
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,true", "true,true", "false,false"}, nullValues = "NULL")
    void forEntity_quandoAtivoInformadoOuOmitido_resolveCamposEPadrao(Boolean ativo, boolean esperado) {
        // Arrange
        BarbeiroRequest request = new BarbeiroRequest("Bruno", List.of(3L), ativo);

        // Act
        Barbeiro resultado = mapper.forEntity(request, servicos);

        // Assert
        assertNull(resultado.getId());
        assertEquals(request.nome(), resultado.getNome());
        assertEquals(esperado, resultado.getAtivo());
        assertEquals(servicos, resultado.getServicos());
    }

    @ParameterizedTest
    @CsvSource(value = {"NULL,true", "true,true", "false,false"}, nullValues = "NULL")
    void updateEntity_quandoDadosMudam_substituiServicosEPreservaId(Boolean ativo, boolean esperado) {
        // Arrange
        BarbeiroRequest request = new BarbeiroRequest("Bruno", List.of(3L), ativo);
        Barbeiro barbeiro = Barbeiro.builder().id(9L).nome("Anterior").ativo(false)
                .servicos(Set.of()).build();

        // Act
        mapper.updateEntity(request, barbeiro, servicos);

        // Assert
        assertEquals(9L, barbeiro.getId());
        assertEquals(request.nome(), barbeiro.getNome());
        assertEquals(esperado, barbeiro.getAtivo());
        assertEquals(servicos, barbeiro.getServicos());
    }

    @Test
    void forResponse_quandoHaVariosServicos_retornaResumosComIdENome() {
        // Arrange
        Servico barba = Servico.builder().id(4L).nome("Barba").build();
        Barbeiro barbeiro = Barbeiro.builder().id(9L).nome("Bruno").ativo(false)
                .servicos(Set.of(servico, barba)).build();

        // Act
        BarbeiroResponse resultado = mapper.forResponse(barbeiro);

        // Assert
        assertEquals(9L, resultado.id());
        assertEquals("Bruno", resultado.nome());
        assertFalse(resultado.ativo());
        assertThat(resultado.servicos()).containsExactlyInAnyOrder(
                new ServicoResumoResponse(3L, "Corte"), new ServicoResumoResponse(4L, "Barba"));
    }

    @Test
    void forResponse_quandoNaoHaServicos_retornaListaVazia() {
        // Arrange
        Barbeiro barbeiro = Barbeiro.builder().id(9L).nome("Bruno").build();

        // Act
        BarbeiroResponse resultado = mapper.forResponse(barbeiro);

        // Assert
        assertEquals(List.of(), resultado.servicos());
        assertEquals(9L, resultado.id());
    }
}
