import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import Swal from 'sweetalert2';

import { Cliente } from '../../../core/models/cliente.model';
import { ClienteService } from '../../../core/services/cliente.service';
import { ConfirmarExclusaoComponent } from '../../../shared/confirmar-exclusao/confirmar-exclusao.component';

/** Listagem de clientes, com busca por nome e exclusão confirmada em modal. */
@Component({
  selector: 'app-cliente-lista',
  imports: [RouterLink, MdbRippleModule, ConfirmarExclusaoComponent],
  templateUrl: './cliente-lista.component.html',
  styleUrl: './cliente-lista.component.scss',
})
export class ClienteListaComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  protected readonly service = inject(ClienteService);

  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly busca = signal('');

  /*
   * O filtro é local, e não uma nova chamada a /api/clientes?nome=: a lista
   * inteira já está no signal do service, então filtrar aqui responde a cada
   * tecla sem uma ida ao servidor por caractere digitado.
   */
  protected readonly clientes = computed(() => {
    const termo = this.busca().trim().toLowerCase();

    return this.service
      .clientes()
      .filter(
        (cliente) =>
          !termo ||
          cliente.nome.toLowerCase().includes(termo) ||
          cliente.telefone.includes(termo),
      )
      .sort((a, b) => a.nome.localeCompare(b.nome));
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected atualizarBusca(evento: Event): void {
    this.busca.set((evento.target as HTMLInputElement).value);
  }

  private carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);

    this.service
      .listar()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.carregando.set(false),
        error: (e: unknown) => {
          this.erro.set(
            e instanceof Error ? e.message : 'Não foi possível carregar os clientes.',
          );
          this.carregando.set(false);
        },
      });
  }

  // ---------------------------------------------------------------- exclusão

  /** Cliente com o modal de confirmação aberto, ou null quando fechado. */
  protected readonly exclusaoAlvo = signal<Cliente | null>(null);
  protected readonly excluindo = signal(false);
  protected readonly erroExclusao = signal<string | null>(null);

  protected readonly alvoExclusao = computed(() => {
    const alvo = this.exclusaoAlvo();
    return alvo ? `o cliente ${alvo.nome}` : '';
  });

  protected pedirExclusao(cliente: Cliente): void {
    this.erroExclusao.set(null);
    this.exclusaoAlvo.set(cliente);
  }

  protected cancelarExclusao(): void {
    if (this.excluindo()) {
      return;
    }
    this.exclusaoAlvo.set(null);
    this.erroExclusao.set(null);
  }

  protected confirmarExclusao(): void {
    const alvo = this.exclusaoAlvo();
    if (!alvo) {
      return;
    }

    this.excluindo.set(true);
    this.erroExclusao.set(null);

    this.service
      .excluir(alvo.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.excluindo.set(false);
          this.exclusaoAlvo.set(null);
          Swal.fire({ icon: 'success', title: 'Cliente excluído!', timer: 1600, showConfirmButton: false });
        },
        error: (e: unknown) => {
          // Fica no modal: excluir um cliente com agendamento devolve 409, e a
          // mensagem precisa aparecer ao lado do botão que o atendente apertou.
          this.erroExclusao.set(
            e instanceof Error ? e.message : 'Não foi possível excluir o cliente.',
          );
          this.excluindo.set(false);
        },
      });
  }
}
