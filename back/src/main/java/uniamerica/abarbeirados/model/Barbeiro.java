package uniamerica.abarbeirados.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "barbeiro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Barbeiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Builder.Default
    @Column(nullable = false)
    private Boolean ativo = true;

    /*
     * Quais servicos este barbeiro atende. E ManyToMany porque o mesmo servico e
     * feito por varios barbeiros e cada barbeiro faz varios servicos — a tabela
     * de juncao guarda so o par, sem coluna extra.
     */
    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "barbeiro_servico",
            joinColumns = @JoinColumn(name = "barbeiro_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id")
    )
    private Set<Servico> servicos = new HashSet<>();

    /** Lado inverso do @ManyToOne de Agendamento. */
    @Builder.Default
    @OneToMany(mappedBy = "barbeiro")
    private List<Agendamento> agendamentos = new ArrayList<>();
}
