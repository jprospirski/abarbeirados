import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Único ponto do projeto que usa Bootstrap — foi pedido assim para este modal
 * de confirmação especificamente. O import do framework fica isolado no SCSS
 * deste componente: a encapsulação padrão do Angular prende as classes aqui
 * dentro e não deixa vazar para o resto da interface.
 */
@Component({
  selector: 'app-confirmar-exclusao',
  templateUrl: './confirmar-exclusao.component.html',
  styleUrl: './confirmar-exclusao.component.scss',
})
export class ConfirmarExclusaoComponent {
  @Input({ required: true }) aberto = false;
  @Input() nomeCliente = '';
  @Input() erro: string | null = null;
  @Input() excluindo = false;

  @Output() confirmar = new EventEmitter<void>();
  @Output() cancelar = new EventEmitter<void>();
}
