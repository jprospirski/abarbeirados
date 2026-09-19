package uniamerica.abarbeirados.dto.barbeiro;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/** Corpo do POST e do PUT de /api/barbeiros. */
public record BarbeiroRequest(
        @NotBlank(message = "O nome é obrigatório")
        String nome,
        @NotEmpty(message = "Selecione ao menos um serviço")
        List<Long> servicoIds,
        // Opcional: quem nao informa cria o barbeiro ativo, mesmo padrao do ServicoRequest.
        Boolean ativo
) {

    public boolean ativoOuPadrao() {
        return ativo == null || ativo;
    }
}
