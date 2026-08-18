package uniamerica.abarbeirados.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClienteRequest(
        @NotBlank(message = "O nome é obrigatório")
        String nome,
        // Opcional: a tela cadastra cliente novo só com nome e telefone, e manda
        // null quando o email não for informado.
        @Email(message = "O email deve ser válido")
        String email,
        @NotBlank(message = "O telefone é obrigatório")
        String telefone
) {
}
