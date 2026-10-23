package uniamerica.abarbeirados.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uniamerica.abarbeirados.dto.agendamento.AgendamentoRequest;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoResponse;
import uniamerica.abarbeirados.model.Agendamento;
import uniamerica.abarbeirados.model.Barbeiro;
import uniamerica.abarbeirados.model.Cliente;
import uniamerica.abarbeirados.model.Servico;
import uniamerica.abarbeirados.model.StatusAgendamento;

class AgendamentoMapperTest {

    private final AgendamentoMapper mapper = new AgendamentoMapper();
    private Cliente cliente;
    private Barbeiro barbeiro;
    private Servico servico;
    private AgendamentoRequest request;
    private Agendamento agendamentoExistente;

    @BeforeEach
    void prepararDados() {
        cliente = Cliente.builder().id(1L).nome("Ana").telefone("45999999999").build();
        barbeiro = Barbeiro.builder().id(2L).nome("Bruno").build();
        servico = Servico.builder().id(3L).nome("Corte").valor(new BigDecimal("40.00"))
                .duracaoMinutos(30).build();
        request = new AgendamentoRequest(1L, 2L, 3L,
                LocalDateTime.of(2030, 6, 10, 14, 30), "Corte curto");
        agendamentoExistente = Agendamento.builder().id(8L).status(StatusAgendamento.CONFIRMADO)
                .valor(BigDecimal.ONE).duracaoMinutos(10).observacoes("Anterior").build();
    }

    @Test
    void forEntity_quandoDadosValidos_copiaRelacionamentosEHorario() {
        // Arrange
        LocalDateTime horarioEsperado = request.dataHora();

        // Act
        Agendamento resultado = mapper.forEntity(request, cliente, barbeiro, servico);

        // Assert
        assertEquals(cliente, resultado.getCliente());
        assertEquals(barbeiro, resultado.getBarbeiro());
        assertEquals(servico, resultado.getServico());
        assertEquals(horarioEsperado, resultado.getDataHora());
        assertEquals(request.observacoes(), resultado.getObservacoes());
    }

    @Test
    void forEntity_quandoCriado_defineStatusEPrecoDoServico() {
        // Arrange
        BigDecimal valorEsperado = servico.getValor();

        // Act
        Agendamento resultado = mapper.forEntity(request, cliente, barbeiro, servico);

        // Assert
        assertNull(resultado.getId());
        assertEquals(StatusAgendamento.AGENDADO, resultado.getStatus());
        assertEquals(valorEsperado, resultado.getValor());
        assertEquals(30, resultado.getDuracaoMinutos());
    }

    @Test
    void updateEntity_quandoDadosMudam_atualizaRelacionamentosEHorario() {
        // Arrange
        LocalDateTime horarioEsperado = request.dataHora();

        // Act
        mapper.updateEntity(request, agendamentoExistente, cliente, barbeiro, servico);

        // Assert
        assertEquals(cliente, agendamentoExistente.getCliente());
        assertEquals(barbeiro, agendamentoExistente.getBarbeiro());
        assertEquals(servico, agendamentoExistente.getServico());
        assertEquals(horarioEsperado, agendamentoExistente.getDataHora());
        assertEquals(request.observacoes(), agendamentoExistente.getObservacoes());
    }

    @Test
    void updateEntity_quandoPrecoMuda_atualizaValorPreservandoIdEStatus() {
        // Arrange
        BigDecimal valorEsperado = servico.getValor();

        // Act
        mapper.updateEntity(request, agendamentoExistente, cliente, barbeiro, servico);

        // Assert
        assertEquals(8L, agendamentoExistente.getId());
        assertEquals(StatusAgendamento.CONFIRMADO, agendamentoExistente.getStatus());
        assertEquals(valorEsperado, agendamentoExistente.getValor());
        assertEquals(30, agendamentoExistente.getDuracaoMinutos());
    }

    @Test
    void forResponse_quandoPrecoDoServicoMudou_preservaValorEDuracaoDoAgendamento() {
        // Arrange
        Agendamento agendamento = Agendamento.builder().id(8L).cliente(cliente).barbeiro(barbeiro)
                .servico(servico).dataHora(request.dataHora()).status(StatusAgendamento.CONCLUIDO)
                .observacoes(request.observacoes()).valor(new BigDecimal("35.00"))
                .duracaoMinutos(25).build();
        AgendamentoResponse esperado = new AgendamentoResponse(8L, 1L, "Ana", "45999999999",
                2L, "Bruno", 3L, "Corte", new BigDecimal("35.00"), 25,
                request.dataHora(), StatusAgendamento.CONCLUIDO, "Corte curto");

        // Act
        AgendamentoResponse resultado = mapper.forResponse(agendamento);

        // Assert
        assertEquals(esperado, resultado);
    }
}
