package uniamerica.abarbeirados.dto.barbeiro;

/**
 * Serviço enxuto dentro da resposta de barbeiro.
 *
 * A listagem de barbeiros só precisa do nome de cada serviço; devolver o
 * ServicoResponse inteiro carregaria valor, duração e ativo sem ninguém ler.
 */
public record ServicoResumoResponse(
        Long id,
        String nome
) {
}
