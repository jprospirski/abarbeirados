import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import Swal from 'sweetalert2';

import { ServicoService } from '../../../core/services/servico.service';

// - cadastro e edição no mesmo componente: o :id da rota decide
// - ativo vai sempre no corpo: o backend trata ausência como ativo, e desligar precisa chegar como false explícito
@Component({
  selector: 'app-servico-form',
  imports: [ReactiveFormsModule, RouterLink, MdbFormsModule, MdbRippleModule],
  templateUrl: './servico-form.component.html',
  styleUrl: './servico-form.component.scss',
})
export class ServicoFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(ServicoService);

  protected readonly editandoId = signal<number | null>(null);
  protected readonly modoEdicao = computed(() => this.editandoId() !== null);

  protected readonly erro = signal<string | null>(null);
  protected readonly salvando = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    valor: [null as number | null, [Validators.required, Validators.min(0.01)]],
    duracaoMinutos: [null as number | null, [Validators.required, Validators.min(1)]],
    ativo: true,
  });

  ngOnInit(): void {
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
        next: (servico) =>
          this.form.reset({
            nome: servico.nome,
            valor: servico.valor,
            duracaoMinutos: servico.duracaoMinutos,
            ativo: servico.ativo,
          }),
        error: (e: unknown) => {
          this.erro.set(
            e instanceof Error ? e.message : 'Não foi possível carregar o serviço.',
          );
        },
      });
  }

  protected salvar(): void {
    this.erro.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.erro.set('Preencha os campos destacados para salvar.');
      return;
    }

    const v = this.form.getRawValue();
    const corpo = {
      nome: v.nome.trim(),
      valor: Number(v.valor),
      duracaoMinutos: Number(v.duracaoMinutos),
      ativo: v.ativo,
    };
    const id = this.editandoId();

    this.salvando.set(true);

    const requisicao$ = id ? this.service.atualizar(id, corpo) : this.service.criar(corpo);

    requisicao$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.salvando.set(false);
        Swal.fire({ icon: 'success', title: 'Serviço salvo!', timer: 1600, showConfirmButton: false });
        this.router.navigate(['/servicos']);
      },
      error: (e: unknown) => {
        const mensagem =
          e instanceof Error ? e.message : 'Não foi possível salvar o serviço.';

        this.erro.set(mensagem);
        this.salvando.set(false);
        Swal.fire({ icon: 'error', title: 'Não deu para salvar', text: mensagem });
      },
    });
  }

  protected invalido(campo: 'nome' | 'valor' | 'duracaoMinutos'): boolean {
    const controle = this.form.controls[campo];
    return controle.invalid && controle.touched;
  }
}
