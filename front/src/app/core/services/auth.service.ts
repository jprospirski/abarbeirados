import { Injectable, signal } from '@angular/core';
import { Observable, delay, of, tap } from 'rxjs';

// - sem backend de autenticação, a credencial fica fixa; trocar por login real é só reescrever login()
const USUARIO_VALIDO = 'admin';
const SENHA_VALIDA = '123456';

// - a sessão fica espelhada no localstorage para sobreviver ao f5
@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly CHAVE = 'abarbeirados:autenticado';

  readonly autenticado = signal(AuthService.sessaoSalva());

  // - o delay simula a ida ao servidor; a tela de login já tem estado de carregando
  login(usuario: string, senha: string): Observable<boolean> {
    const valido = usuario.trim() === USUARIO_VALIDO && senha === SENHA_VALIDA;

    return of(valido).pipe(
      delay(500),
      tap((ok) => {
        if (ok) {
          this.autenticado.set(true);
          this.persistir(true);
        }
      }),
    );
  }

  logout(): void {
    this.autenticado.set(false);
    this.persistir(false);
  }

  private persistir(valor: boolean): void {
    try {
      if (valor) {
        localStorage.setItem(AuthService.CHAVE, '1');
      } else {
        localStorage.removeItem(AuthService.CHAVE);
      }
    } catch {
      // - navegação privativa bloqueia o storage: a sessão só deixa de sobreviver ao refresh, não trava o login
    }
  }

  private static sessaoSalva(): boolean {
    try {
      return localStorage.getItem(AuthService.CHAVE) === '1';
    } catch {
      return false;
    }
  }
}
