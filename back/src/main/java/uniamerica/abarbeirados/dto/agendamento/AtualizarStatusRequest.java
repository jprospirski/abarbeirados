package uniamerica.abarbeirados.dto.agendamento;

import jakarta.validation.constraints.NotNull;
import uniamerica.abarbeirados.entity.StatusAgendamento;

public class AtualizarStatusRequest {

        @NotNull(message = "Status é obrigatório")
        private StatusAgendamento status;

        public StatusAgendamento getStatus() {
                return status;
        }

        public void setStatus(StatusAgendamento status) {
                this.status = status;
        }
}