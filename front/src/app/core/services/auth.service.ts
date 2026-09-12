import { Injectable, signal } from '@angular/core';
import { Observable, delay, of, tap } from 'rxjs';

/**
 * Não existe backend de autenticação ainda, então a credencial fica fixa aqui
 * mesmo. Trocar por uma chamada real de login é só reescrever o corpo de
 * {@link AuthService.login} — a sessão já fica no signal e no localStorage.
 */
const USUARIO_VALIDO = 'admin';
const SENHA_VALIDA = '123456';

/**
 * Sessão mockada da aplicação.
 *
 * O estado sobrevive a um refresh porque fica espelhado no localStorage — sem
 * isso, dar F5 na tela de agenda jogaria o atendente de volta para o login a
 * cada recarga.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private static readonly CHAVE = 'abarbeirados:autenticado';

  readonly autenticado = signal(AuthService.sessaoSalva());

  /** Delay simula a ida ao servidor — a tela de login já tem estado de carregando. */
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
      // Navegação privativa bloqueia o storage. A sessão só deixa de sobreviver
      // a um refresh — não é motivo para travar o login.
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
