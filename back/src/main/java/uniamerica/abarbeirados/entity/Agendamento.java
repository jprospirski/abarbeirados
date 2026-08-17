package uniamerica.abarbeirados.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agendamento")
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: quando Cliente/Servico estiverem prontos, trocar para Long clienteId / Long servicoId
    // (ou @ManyToOne para as entidades Cliente e Servico do Cauã)
    private String nome;
    private String email;
    private String telefone;

    private String servico;

    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    private StatusAgendamento status;

}