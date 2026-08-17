package uniamerica.abarbeirados.dto.agendamento;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgendamentoRequest {

        @NotBlank(message = "Nome é obrigatório")
        private String nome;

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        private String email;

        @NotBlank(message = "Telefone é obrigatório")
        private String telefone;

        @NotBlank(message = "Serviço é obrigatório")
        private String servico;

        @NotNull(message = "Data e hora são obrigatórias")
        @Future(message = "A data do agendamento deve ser no futuro")
        private LocalDateTime dataHora;
}