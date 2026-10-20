package uniamerica.abarbeirados.dto.barbeiro;

import java.util.List;

public record BarbeiroResponse(
        Long id,
        String nome,
        Boolean ativo,
        List<ServicoResumoResponse> servicos
) {
}
