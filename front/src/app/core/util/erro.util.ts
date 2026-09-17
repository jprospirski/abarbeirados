import { Observable, throwError } from 'rxjs';

/**
 * Formato único de erro da API, montado pelo GlobalException do backend.
 *
 * `fields` só vem preenchido em erro de validação (`@Valid`), com o motivo campo
 * a campo; nos demais casos — regra de negócio, recurso não encontrado — a
 * mensagem útil está em `message`.
 */
export interface ApiError {
  message?: string;
  fields?: Record<string, string> | null;
}

/**
 * Converte o ApiError do backend em Error com a mensagem que ele mandou.
 *
 * Nasceu dentro do AgendamentoService e subiu para cá quando os services de
 * Cliente, Serviço e Barbeiro passaram a precisar do mesmo tratamento — é o
 * único ponto do frontend que sabe o formato de erro da API.
 */
export function traduzirErro(padrao: string) {
  return (resposta: unknown): Observable<never> => {
    const corpo = (resposta as { error?: ApiError })?.error;

    const detalhe = corpo?.fields
      ? Object.values(corpo.fields).join(' ')
      : corpo?.message;

    return throwError(() => new Error(detalhe || padrao));
  };
}
