package uniamerica.abarbeirados.mapper;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import uniamerica.abarbeirados.dto.barbeiro.BarbeiroRequest;
import uniamerica.abarbeirados.dto.barbeiro.BarbeiroResponse;
import uniamerica.abarbeirados.dto.barbeiro.ServicoResumoResponse;
import uniamerica.abarbeirados.model.Barbeiro;
import uniamerica.abarbeirados.model.Servico;

@Component
public class BarbeiroMapper {

    /*
     * Os Servicos chegam prontos: quem resolve os ids e o service, que e a camada
     * com acesso aos repositorios.
     */
    public Barbeiro forEntity(BarbeiroRequest request, Set<Servico> servicos) {
        return Barbeiro.builder()
                .nome(request.nome())
                .ativo(request.ativoOuPadrao())
                .servicos(servicos)
                .build();
    }

    public void updateEntity(BarbeiroRequest request, Barbeiro barbeiro, Set<Servico> servicos) {
        barbeiro.setNome(request.nome());
        barbeiro.setAtivo(request.ativoOuPadrao());
        barbeiro.setServicos(servicos);
    }

    public BarbeiroResponse forResponse(Barbeiro barbeiro) {
        List<ServicoResumoResponse> servicos = barbeiro.getServicos().stream()
                .map(servico -> new ServicoResumoResponse(servico.getId(), servico.getNome()))
                .toList();

        return new BarbeiroResponse(
                barbeiro.getId(),
                barbeiro.getNome(),
                barbeiro.getAtivo(),
                servicos
        );
    }
}
