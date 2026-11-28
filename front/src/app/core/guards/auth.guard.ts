import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { LoginService } from '../../features/auth/login/login.service';

// - fecha as rotas do app para quem não tem um token jwt válido (ausente ou expirado volta ao login)
export const authGuard: CanActivateFn = () => {
  const loginService = inject(LoginService);
  const router = inject(Router);

  if (loginService.tokenValido()) {
    return true;
  }

  loginService.removerToken();
  return router.createUrlTree(['/login']);
};
