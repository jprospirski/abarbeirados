import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import Swal from 'sweetalert2';

import { BarbeiroService } from '../../../core/services/barbeiro.service';
import { ServicoService } from '../../../core/services/servico.service';

// - cadastro e edição no mesmo componente: o :id da rota decide
// - lista todos os serviços, inativos inclusive: um serviço desativado precisa continuar marcado na edição em vez de sumir do put
@Component({
  selector: 'app-barbeiro-form',
  imports: [ReactiveFormsModule, RouterLink, MdbFormsModule, MdbRippleModule],
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

  protected readonly erro = signal<string | null>(null);
  protected readonly salvando = signal(false);
  protected readonly carregandoServicos = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    servicoIds: [[] as number[], Validators.required],
    ativo: true,
  });

  // - ordem alfabética, com os inativos no fim para não se misturarem aos ativos
  protected readonly servicos = computed(() =>
    [...this.servicoService.servicos()].sort(
      (a, b) => Number(b.ativo) - Number(a.ativo) || a.nome.localeCompare(b.nome),
    ),
  );

  ngOnInit(): void {
    this.carregarServicos();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }

    const id = Number(idParam);
    this.editandoId.set(id);

    this.service
      .buscarPorId(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (barbeiro) =>
          this.form.reset({
            nome: barbeiro.nome,
            servicoIds: barbeiro.servicos.map((s) => s.id),
            ativo: barbeiro.ativo,
          }),
        error: (e: unknown) => {
          this.erro.set(
            e instanceof Error ? e.message : 'Não foi possível carregar o barbeiro.',
          );
        },
      });
  }

  private carregarServicos(): void {
    this.carregandoServicos.set(true);

    this.servicoService
      .listar()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.carregandoServicos.set(false),
        error: (e: unknown) => {
          this.erro.set(
            e instanceof Error ? e.message : 'Não foi possível carregar os serviços.',
          );
          this.carregandoServicos.set(false);
        },
      });
  }

  protected marcado(servicoId: number): boolean {
    return this.form.controls.servicoIds.value.includes(servicoId);
  }

  protected alternarServico(servicoId: number): void {
    const controle = this.form.controls.servicoIds;
    const atuais = controle.value;

    controle.setValue(
      atuais.includes(servicoId)
        ? atuais.filter((id) => id !== servicoId)
        : [...atuais, servicoId],
    );
    controle.markAsTouched();
  }

  protected salvar(): void {
    this.erro.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.erro.set('Preencha os campos destacados para salvar.');
      return;
    }

    const v = this.form.getRawValue();
    const corpo = { nome: v.nome.trim(), servicoIds: v.servicoIds, ativo: v.ativo };
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

  protected invalido(campo: 'nome' | 'servicoIds'): boolean {
    const controle = this.form.controls[campo];
    return controle.invalid && controle.touched;
  }
}
