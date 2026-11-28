import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { Router, RouterLink } from '@angular/router';
import Swal from 'sweetalert2';
import { Login } from '../login/login';
import { LoginService } from '../login/login.service';

// - cadastro de novo usuário (POST /api/register); reaproveita o visual e o modelo da tela de login
@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [FormsModule, MdbFormsModule, RouterLink],
  templateUrl: './registro.component.html',
  styleUrl: '../login/login.component.scss'
})
export class RegistroComponent {

  login: Login = new Login();

  confirmacao = '';

  salvando = false;

  router = inject(Router);

  loginService = inject(LoginService);


  registrar() {

    if (!this.login.username || !this.login.password) {
      Swal.fire('Informe usuário e senha.', '', 'warning');
      return;
    }

    if (this.login.password !== this.confirmacao) {
      Swal.fire('As senhas não conferem.', '', 'warning');
      return;
    }

    this.salvando = true;

    this.loginService.registrar(this.login).subscribe({
      next: () => {
        this.salvando = false;

        Swal.fire('Usuário criado!', 'Agora é só entrar com ele.', 'success');
        this.router.navigate(['/login']);
      },
      error: erro => {
        this.salvando = false;

        Swal.fire('Não foi possível criar o usuário.', this.mensagem(erro), 'error');
      }
    });

  }

  // - a api devolve o motivo em fields (validação campo a campo) ou em message (regra de negócio)
  private mensagem(erro: any): string {
    const corpo = erro?.error;

    if (corpo?.fields)
      return Object.values(corpo.fields).join(' ');

    return corpo?.message || 'Tente novamente em instantes.';
  }

}
