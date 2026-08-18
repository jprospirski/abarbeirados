package uniamerica.abarbeirados.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import uniamerica.abarbeirados.dto.agendamento.AgendaDoDiaResponse;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoRequest;
import uniamerica.abarbeirados.dto.agendamento.AgendamentoResponse;
import uniamerica.abarbeirados.dto.agendamento.AtualizarStatusRequest;
import uniamerica.abarbeirados.service.AgendamentoService;

@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    @PostMapping
    public ResponseEntity<AgendamentoResponse> criar(@Valid @RequestBody AgendamentoRequest request) {
        AgendamentoResponse response = agendamentoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AgendamentoResponse>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(agendamentoService.listar(busca, data));
    }

    @GetMapping("/agenda")
    public ResponseEntity<List<AgendaDoDiaResponse>> agenda() {
        return ResponseEntity.ok(agendamentoService.agendaAgrupadaPorDia());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendamentoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendamentoResponse> atualizar(
            @PathVariable Long id, @Valid @RequestBody AgendamentoRequest request) {
        return ResponseEntity.ok(agendamentoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AgendamentoResponse> atualizarStatus(
            @PathVariable Long id, @Valid @RequestBody AtualizarStatusRequest request) {
        return ResponseEntity.ok(agendamentoService.atualizarStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        agendamentoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
