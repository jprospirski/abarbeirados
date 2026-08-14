package uniamerica.abarbeirados.mapper;

import org.springframework.stereotype.Component;

import uniamerica.abarbeirados.dto.servico.ServicoRequest;
import uniamerica.abarbeirados.dto.servico.ServicoResponse;
import uniamerica.abarbeirados.model.Servico;

@Component
public class ServicoMapper {

    public Servico forEntity(ServicoRequest request) {
        return Servico.builder()
                .nome(request.nome())
                .valor(request.valor())
                .duracaoMinutos(request.duracaoMinutos())
                .build();
    }

    public void updateEntity(ServicoRequest request, Servico servico) {
        servico.setNome(request.nome());
        servico.setValor(request.valor());
        servico.setDuracaoMinutos(request.duracaoMinutos());
    }

    public ServicoResponse forResponse(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getValor(),
                servico.getDuracaoMinutos(),
                servico.getAtivo()
        );
    }
}
