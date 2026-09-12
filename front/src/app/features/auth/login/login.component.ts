import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly entrando = signal(false);
  protected readonly erro = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    usuario: ['', Validators.required],
    senha: ['', Validators.required],
  });

  protected entrar(): void {
    this.erro.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.erro.set('Informe usuário e senha para entrar.');
      return;
    }

    const { usuario, senha } = this.form.getRawValue();
    this.entrando.set(true);

    this.auth.login(usuario, senha).subscribe((ok) => {
      this.entrando.set(false);

      if (ok) {
        this.router.navigateByUrl('/agendamentos');
        return;
      }

      this.erro.set('Usuário ou senha inválidos.');
    });
  }
}
