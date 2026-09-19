package uniamerica.abarbeirados.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClienteRequest(
        @NotBlank(message = "O nome é obrigatório")
        String nome,
        // - opcional: a tela cadastra cliente novo só com nome e telefone e manda null sem email
        @Email(message = "O email deve ser válido")
        String email,
        @NotBlank(message = "O telefone é obrigatório")
        // - ddd + número, com ou sem o nono dígito; o front já manda só dígitos
        @Pattern(regexp = "\\d{10,11}", message = "O telefone deve ter 10 ou 11 dígitos, só números")
        String telefone
) {
}
