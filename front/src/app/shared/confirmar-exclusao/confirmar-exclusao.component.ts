import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';

// - modal de exclusão compartilhado pelas quatro telas; usa as classes de modal e o ripple do mdb
@Component({
  selector: 'app-confirmar-exclusao',
  imports: [MdbRippleModule],
  templateUrl: './confirmar-exclusao.component.html',
  styleUrl: './confirmar-exclusao.component.scss',
})
export class ConfirmarExclusaoComponent {
  @Input({ required: true }) aberto = false;
  // - 'excluir cliente', 'excluir serviço'...
  @Input() titulo = 'Excluir registro';
  // - o que aparece em negrito no meio da frase: o nome do que vai sumir
  @Input() alvo = '';
  @Input() erro: string | null = null;
  @Input() excluindo = false;

  @Output() confirmar = new EventEmitter<void>();
  @Output() cancelar = new EventEmitter<void>();
}
