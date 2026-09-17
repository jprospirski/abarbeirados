import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';

/**
 * Modal de confirmação de exclusão, compartilhado por Agenda, Cliente, Serviço
 * e Barbeiro.
 *
 * Nasceu dentro de `features/agenda/` quando só o agendamento podia ser
 * excluído; subiu para `shared/` quando as quatro telas passaram a precisar
 * dele, em vez de copiar o mesmo modal quatro vezes.
 *
 * A marcação usa as classes de modal do MDB (que carrega o Bootstrap junto, via
 * `mdb.scss` no angular.json) e o ripple do MDB nos botões de ação — por isso o
 * SCSS daqui só ajusta o que o kit próprio do projeto precisa recuperar.
 */
@Component({
  selector: 'app-confirmar-exclusao',
  imports: [MdbRippleModule],
  templateUrl: './confirmar-exclusao.component.html',
  styleUrl: './confirmar-exclusao.component.scss',
})
export class ConfirmarExclusaoComponent {
  @Input({ required: true }) aberto = false;
  /** 'Excluir cliente', 'Excluir serviço'... */
  @Input() titulo = 'Excluir registro';
  /** O que aparece em negrito no meio da frase: o nome do que vai sumir. */
  @Input() alvo = '';
  @Input() erro: string | null = null;
  @Input() excluindo = false;

  @Output() confirmar = new EventEmitter<void>();
  @Output() cancelar = new EventEmitter<void>();
}
