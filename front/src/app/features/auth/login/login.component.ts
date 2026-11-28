import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { Router, RouterLink } from '@angular/router';
import Swal from 'sweetalert2';
import { Login } from './login';
import { LoginService } from './login.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, MdbFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  login: Login = new Login();

  entrando = false;

  router = inject(Router);

  loginService = inject(LoginService);


  constructor(){
    this.loginService.removerToken();
  }


  logar() {

    if (!this.login.username || !this.login.password) {
      Swal.fire('Informe usuário e senha para entrar.', '', 'warning');
      return;
    }

    this.entrando = true;

    this.loginService.logar(this.login).subscribe({
      next: token => {
        this.entrando = false;

        if (token)
          this.loginService.addToken(token); //MUITO IMPORTANTE

        this.gerarToast().fire({ icon: "success", title: "Seja bem-vindo!" });
        this.router.navigate(['/agendamentos']);
      },
      error: erro => {
        this.entrando = false;

        if (erro.status === 401)
          Swal.fire('Usuário ou senha incorretos!', '', 'error');
        else
          Swal.fire('Não foi possível entrar.', 'Tente novamente em instantes.', 'error');
      }
    });

  }

  gerarToast() {
    return Swal.mixin({
      toast: true,
      position: "top-end",
      showConfirmButton: false,
      timer: 3000,
      timerProgressBar: true,
      didOpen: (toast) => {
        toast.onmouseenter = Swal.stopTimer;
        toast.onmouseleave = Swal.resumeTimer;
      }
    });
  }

}
