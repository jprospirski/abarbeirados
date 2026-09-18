import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import Swal from 'sweetalert2';

import { CepResponse } from '../../../core/models/cep.model';
import { ClienteService } from '../../../core/services/cliente.service';

/**
 * Cadastro e edição de cliente na mesma rota — o que decide é o `:id` do
 * caminho, lido do snapshot, igual ao AgendamentoComponent.
 *
 * O CEP é só conveniência de tela: a consulta sai pelo backend (Feign/ViaCEP) e
 * o endereço aparece como confirmação visual, mas não é enviado no corpo do
 * POST/PUT — a tabela `clientes` não tem coluna de endereço.
 */
@Component({
  selector: 'app-cliente-form',
  imports: [ReactiveFormsModule, RouterLink, MdbFormsModule, MdbRippleModule],
  templateUrl: './cliente-form.component.html',
  styleUrl: './cliente-form.component.scss',
})
export class ClienteFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(ClienteService);

  protected readonly editandoId = signal<number | null>(null);
  protected readonly modoEdicao = computed(() => this.editandoId() !== null);

  protected readonly erro = signal<string | null>(null);
  protected readonly salvando = signal(false);

  /** Endereço devolvido pela ViaCEP, ou null enquanto ninguém consultou. */
  protected readonly endereco = signal<CepResponse | null>(null);
  protected readonly buscandoCep = signal(false);
  protected readonly erroCep = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    nome: ['', Validators.required],
    telefone: ['', Validators.required],
    email: ['', Validators.email],
    cep: '',
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
        next: (cliente) =>
          this.form.reset({
            nome: cliente.nome,
            telefone: cliente.telefone,
            email: cliente.email ?? '',
            cep: '',
          }),
        error: (e: unknown) => {
          this.erro.set(
            e instanceof Error ? e.message : 'Não foi possível carregar o cliente.',
          );
        },
      });
  }

  // -------------------------------------------------------------------- cep

  protected buscarCep(): void {
    const cep = this.form.controls.cep.value.replace(/\D/g, '');

    this.erroCep.set(null);
    this.endereco.set(null);

    if (cep.length !== 8) {
      this.erroCep.set('Informe um CEP com 8 dígitos.');
      return;
    }

    this.buscandoCep.set(true);

    this.service
      .buscarPorCep(cep)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (resposta) => {
          this.endereco.set(resposta);
          this.buscandoCep.set(false);
        },
        error: (e: unknown) => {
          this.erroCep.set(
            e instanceof Error ? e.message : 'Não foi possível consultar o CEP.',
          );
          this.buscandoCep.set(false);
        },
      });
  }

  // ------------------------------------------------------------------ ações

  protected salvar(): void {
    this.erro.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.erro.set('Preencha os campos destacados para salvar.');
      return;
    }

    const v = this.form.getRawValue();
    const corpo = { nome: v.nome, email: v.email || null, telefone: v.telefone };
    const id = this.editandoId();

    this.salvando.set(true);

    const requisicao$ = id ? this.service.atualizar(id, corpo) : this.service.criar(corpo);

    requisicao$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.salvando.set(false);
        Swal.fire({ icon: 'success', title: 'Cliente salvo!', timer: 1600, showConfirmButton: false });
        this.router.navigate(['/clientes']);
      },
      error: (e: unknown) => {
        const mensagem =
          e instanceof Error ? e.message : 'Não foi possível salvar o cliente.';

        this.erro.set(mensagem);
        this.salvando.set(false);
        Swal.fire({ icon: 'error', title: 'Não deu para salvar', text: mensagem });
      },
    });
  }

  // ------------------------------------------------------------- validação

  protected invalido(campo: 'nome' | 'telefone' | 'email'): boolean {
    const controle = this.form.controls[campo];
    return controle.invalid && controle.touched;
  }
}
