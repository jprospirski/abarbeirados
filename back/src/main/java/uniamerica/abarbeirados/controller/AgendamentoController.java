package uniamerica.abarbeirados.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uniamerica.abarbeirados.dto.agendamento.AgendaDoDiaResponse;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoRequest;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoResponse;
import uniamerica.abarbeirados.dto.agendamento.AtualizarStatusRequest;
import uniamerica.abarbeirados.entity.Agendamento;
import uniamerica.abarbeirados.mapper.AgendamentoMapper;
import uniamerica.abarbeirados.service.AgendamentoService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    @Autowired
    private AgendamentoService agendamentoService;

    @PostMapping
    public ResponseEntity<AgendamentoResponse> save(@Valid @RequestBody AgendamentoRequest request) {
        Agendamento saved = agendamentoService.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendamentoMapper.toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<AgendamentoResponse>> findAll(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia) {

        List<AgendamentoResponse> agendamentos = agendamentoService.findAll(busca, dia).stream()
                .map(AgendamentoMapper::toResponse)
                .toList();

        return ResponseEntity.ok(agendamentos);
    }

    @GetMapping("/agenda")
    public ResponseEntity<List<AgendaDoDiaResponse>> getAgenda() {
        return ResponseEntity.ok(agendamentoService.getAgendaAgrupadaPorDia());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendamentoResponse> findById(@PathVariable Long id) {
        Agendamento agendamento = agendamentoService.findById(id);
        return ResponseEntity.ok(AgendamentoMapper.toResponse(agendamento));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendamentoResponse> update(@PathVariable Long id, @Valid @RequestBody AgendamentoRequest request) {
        Agendamento updated = agendamentoService.update(id, request);
        return ResponseEntity.ok(AgendamentoMapper.toResponse(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AgendamentoResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody AtualizarStatusRequest request) {
        Agendamento updated = agendamentoService.updateStatus(id, request);
        return ResponseEntity.ok(AgendamentoMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        agendamentoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}