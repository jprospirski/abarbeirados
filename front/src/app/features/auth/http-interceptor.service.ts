import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { LoginService } from './login/login.service';

// - rotas públicas da api: não levam token, e um 401 nelas é "senha errada", não "sessão expirada"
const ROTAS_PUBLICAS = ['/api/login', '/api/register'];

export const meuhttpInterceptor: HttpInterceptorFn = (request, next) => {

  let router = inject(Router);
  let loginService = inject(LoginService);

  let token = loginService.getToken();
  let publica = ROTAS_PUBLICAS.some((rota) => request.url.includes(rota));

  if (token && !publica) {
    request = request.clone({
      setHeaders: { Authorization: 'Bearer ' + token },
    });
  }

  return next(request).pipe(
    catchError((err: any) => {
      // - 401/403 numa rota protegida: token ausente, vencido ou inválido; encerra a sessão e volta ao login
      if (err instanceof HttpErrorResponse && !publica && (err.status === 401 || err.status === 403)) {
        loginService.removerToken();
        router.navigate(['/login']);
      }

      return throwError(() => err);
    })
  );
};
