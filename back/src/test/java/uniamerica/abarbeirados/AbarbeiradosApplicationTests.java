package uniamerica.abarbeirados;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import uniamerica.abarbeirados.controller.AgendamentoController;
import uniamerica.abarbeirados.controller.BarbeiroController;
import uniamerica.abarbeirados.controller.ClienteController;
import uniamerica.abarbeirados.controller.ServicoController;

@SpringBootTest
class AbarbeiradosApplicationTests {

	@Autowired
	private ApplicationContext contexto;

	@Test
	void deveRegistrarControllers_quandoAplicacaoIniciar() {
		// Given: o SpringBootTest inicializa o contexto completo da aplicacao.

		// When
		var clientes = contexto.getBeansOfType(ClienteController.class);
		var barbeiros = contexto.getBeansOfType(BarbeiroController.class);
		var servicos = contexto.getBeansOfType(ServicoController.class);
		var agendamentos = contexto.getBeansOfType(AgendamentoController.class);

		// Then
		assertThat(clientes).as("controller de clientes registrado").hasSize(1);
		assertThat(barbeiros).as("controller de barbeiros registrado").hasSize(1);
		assertThat(servicos).as("controller de servicos registrado").hasSize(1);
		assertThat(agendamentos).as("controller de agendamentos registrado").hasSize(1);
	}

}
