import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import Swal from 'sweetalert2';

import { Barbeiro } from '../../../core/models/barbeiro.model';
import { BarbeiroService } from '../../../core/services/barbeiro.service';
import { ConfirmarExclusaoComponent } from '../../../shared/confirmar-exclusao/confirmar-exclusao.component';

// - carrega todos, inativos inclusive: é daqui que um barbeiro afastado volta para a escala
@Component({
  selector: 'app-barbeiro-lista',
  imports: [RouterLink, MdbRippleModule, ConfirmarExclusaoComponent],
  templateUrl: './barbeiro-lista.component.html',
  styleUrl: './barbeiro-lista.component.scss',
})
export class BarbeiroListaComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  protected readonly service = inject(BarbeiroService);

  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly busca = signal('');

  // - filtro local, mesmo critério das outras listagens
  protected readonly barbeiros = computed(() => {
    const termo = this.busca().trim().toLowerCase();

    return this.service
      .barbeiros()
      .filter((barbeiro) => !termo || barbeiro.nome.toLowerCase().includes(termo))
      .sort((a, b) => a.nome.localeCompare(b.nome));
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected atualizarBusca(evento: Event): void {
    this.busca.set((evento.target as HTMLInputElement).value);
  }

  // - "corte, barba, sobrancelha", ou um traço para quem não atende nada
  protected nomesServicos(barbeiro: Barbeiro): string {
    return barbeiro.servicos.map((s) => s.nome).join(', ') || '—';
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
            e instanceof Error ? e.message : 'Não foi possível carregar os barbeiros.',
          );
          this.carregando.set(false);
        },
      });
  }

  // - barbeiro com o modal de confirmação aberto, ou null quando fechado
  protected readonly exclusaoAlvo = signal<Barbeiro | null>(null);
  protected readonly excluindo = signal(false);
  protected readonly erroExclusao = signal<string | null>(null);

  protected readonly alvoExclusao = computed(() => {
    const alvo = this.exclusaoAlvo();
    return alvo ? `o barbeiro ${alvo.nome}` : '';
  });

  protected pedirExclusao(barbeiro: Barbeiro): void {
    this.erroExclusao.set(null);
    this.exclusaoAlvo.set(barbeiro);
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
          Swal.fire({ icon: 'success', title: 'Barbeiro excluído!', timer: 1600, showConfirmButton: false });
        },
        error: (e: unknown) => {
          // - fica no modal: barbeiro com agendamento devolve 409 e a mensagem precisa aparecer ao lado do botão
          this.erroExclusao.set(
            e instanceof Error ? e.message : 'Não foi possível excluir o barbeiro.',
          );
          this.excluindo.set(false);
        },
      });
  }
}
