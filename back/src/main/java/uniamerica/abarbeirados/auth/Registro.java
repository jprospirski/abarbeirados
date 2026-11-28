package uniamerica.abarbeirados.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// - corpo de /api/register: os mesmos campos do Login, com as regras de um cadastro novo
@Getter
@Setter
public class Registro {

    @NotBlank(message = "O usuário é obrigatório")
    @Size(min = 3, max = 50, message = "O usuário deve ter entre 3 e 50 caracteres")
    private String username;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
    private String password;

}
