import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import Swal from 'sweetalert2';

import { Servico } from '../../../core/models/servico.model';
import { ServicoService } from '../../../core/services/servico.service';
import { ConfirmarExclusaoComponent } from '../../../shared/confirmar-exclusao/confirmar-exclusao.component';

// - carrega todos, inativos inclusive: é daqui que um serviço desativado é religado
@Component({
  selector: 'app-servico-lista',
  imports: [CurrencyPipe, RouterLink, MdbRippleModule, ConfirmarExclusaoComponent],
  templateUrl: './servico-lista.component.html',
  styleUrl: './servico-lista.component.scss',
})
export class ServicoListaComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  protected readonly service = inject(ServicoService);

  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly busca = signal('');

  // - filtro local, mesmo critério da listagem de clientes
  protected readonly servicos = computed(() => {
    const termo = this.busca().trim().toLowerCase();

    return this.service
      .servicos()
      .filter((servico) => !termo || servico.nome.toLowerCase().includes(termo))
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
            e instanceof Error ? e.message : 'Não foi possível carregar os serviços.',
          );
          this.carregando.set(false);
        },
      });
  }

  // - serviço com o modal de confirmação aberto, ou null quando fechado
  protected readonly exclusaoAlvo = signal<Servico | null>(null);
  protected readonly excluindo = signal(false);
  protected readonly erroExclusao = signal<string | null>(null);

  protected readonly alvoExclusao = computed(() => {
    const alvo = this.exclusaoAlvo();
    return alvo ? `o serviço ${alvo.nome}` : '';
  });

  protected pedirExclusao(servico: Servico): void {
    this.erroExclusao.set(null);
    this.exclusaoAlvo.set(servico);
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
          Swal.fire({ icon: 'success', title: 'Serviço excluído!', timer: 1600, showConfirmButton: false });
        },
        error: (e: unknown) => {
          // - fica no modal: serviço com agendamento ou barbeiro vinculado devolve 409 e a mensagem precisa aparecer ao lado do botão
          this.erroExclusao.set(
            e instanceof Error ? e.message : 'Não foi possível excluir o serviço.',
          );
          this.excluindo.set(false);
        },
      });
  }
}
