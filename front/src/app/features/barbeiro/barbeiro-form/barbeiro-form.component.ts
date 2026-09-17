import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MdbCheckboxModule } from 'mdb-angular-ui-kit/checkbox';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import { forkJoin, of } from 'rxjs';
import Swal from 'sweetalert2';

import { Barbeiro } from '../../../core/models/barbeiro.model';
import { BarbeiroService } from '../../../core/services/barbeiro.service';
import { ServicoService } from '../../../core/services/servico.service';

/**
 * Cadastro e edição de barbeiro na mesma rota.
 *
 * Os serviços marcados viram `servicoIds` no corpo — é o @ManyToMany do
 * backend. Ficam num signal e não num FormControl porque a checagem é feita com
 * checkbox solto por serviço, e o formulário só precisa do resultado final.
 */
@Component({
  selector: 'app-barbeiro-form',
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    RouterLink,
    MdbCheckboxModule,
    MdbFormsModule,
    MdbRippleModule,
  ],
  templateUrl: './barbeiro-form.component.html',
  styleUrl: './barbeiro-form.component.scss',
})
export class BarbeiroFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(BarbeiroService);
  protected readonly servicoService = inject(ServicoService);

  protected readonly editandoId = signal<number | null>(null);
  protected readonly modoEdicao = computed(() => this.editandoId() !== null);

  protected readonly carregando = signal(true);
  protected readonly erro = signal<string | null>(null);
  protected readonly salvando = signal(false);
  /** Marcado só depois do primeiro envio, para não acusar erro em tela nova. */
  protected readonly tentouEnviar = signal(false);

  protected readonly selecionados = signal<number[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    ativo: true,
  });

  /** Só serviços ativos podem ser atribuídos: o desativado não é mais vendido. */
  protected readonly servicos = computed(() =>
    this.servicoService
      .servicos()
      .filter((servico) => servico.ativo)
      .sort((a, b) => a.nome.localeCompare(b.nome)),
  );

  protected readonly semServico = computed(
    () => this.tentouEnviar() && this.selecionados().length === 0,
  );

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : null;
    this.editandoId.set(id);

    /*
     * O catálogo e o barbeiro são carregados juntos: sem o catálogo em mãos a
     * lista de checkboxes abriria vazia, e as marcações do barbeiro não teriam
     * onde pousar.
     */
    forkJoin({
      servicos: this.servicoService.listar(true),
      barbeiro: id ? this.service.buscarPorId(id) : of(null),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ barbeiro }) => {
          if (barbeiro) {
            this.preencherParaEdicao(barbeiro);
          }
          this.carregando.set(false);
        },
        error: (e: unknown) => {
          this.erro.set(
            e instanceof Error ? e.message : 'Não foi possível carregar o formulário.',
          );
          this.carregando.set(false);
        },
      });
  }

  private preencherParaEdicao(barbeiro: Barbeiro): void {
    this.form.reset({ nome: barbeiro.nome, ativo: barbeiro.ativo });
    this.selecionados.set(barbeiro.servicos.map((servico) => servico.id));
  }

  // ------------------------------------------------------------------ ações

  protected marcado(servicoId: number): boolean {
    return this.selecionados().includes(servicoId);
  }

  protected alternarServico(servicoId: number): void {
    this.selecionados.update((atuais) =>
      atuais.includes(servicoId)
        ? atuais.filter((id) => id !== servicoId)
        : [...atuais, servicoId],
    );
  }

  protected salvar(): void {
    this.erro.set(null);
    this.tentouEnviar.set(true);

    if (this.form.invalid || this.selecionados().length === 0) {
      this.form.markAllAsTouched();
      this.erro.set('Informe o nome e ao menos um serviço para salvar.');
      return;
    }

    const v = this.form.getRawValue();
    const corpo = { nome: v.nome, servicoIds: this.selecionados(), ativo: v.ativo };

    const id = this.editandoId();
    this.salvando.set(true);

    const requisicao$ = id ? this.service.atualizar(id, corpo) : this.service.criar(corpo);

    requisicao$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.salvando.set(false);
        Swal.fire({ icon: 'success', title: 'Barbeiro salvo!', timer: 1600, showConfirmButton: false });
        this.router.navigate(['/barbeiros']);
      },
      error: (e: unknown) => {
        const mensagem =
          e instanceof Error ? e.message : 'Não foi possível salvar o barbeiro.';

        this.erro.set(mensagem);
        this.salvando.set(false);
        Swal.fire({ icon: 'error', title: 'Não deu para salvar', text: mensagem });
      },
    });
  }

  protected get nomeInvalido(): boolean {
    const controle = this.form.controls.nome;
    return controle.invalid && controle.touched;
  }
}
