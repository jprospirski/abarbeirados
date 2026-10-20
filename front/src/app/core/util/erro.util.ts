import { Observable, throwError } from 'rxjs';

// - formato único de erro da api (GlobalException); fields só vem em erro de validação, o resto usa message
export interface ApiError {
  message?: string;
  fields?: Record<string, string> | null;
}

// - converte o apierror em error com a mensagem do backend; único ponto do front que conhece esse formato
export function traduzirErro(padrao: string) {
  return (resposta: unknown): Observable<never> => {
    const corpo = (resposta as { error?: ApiError })?.error;

    const detalhe = corpo?.fields
      ? Object.values(corpo.fields).join(' ')
      : corpo?.message;

    return throwError(() => new Error(detalhe || padrao));
  };
}
