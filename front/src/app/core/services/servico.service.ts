import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, tap } from 'rxjs';

import { Servico, ServicoRequest } from '../models/servico.model';
import { traduzirErro } from '../util/erro.util';

// - fala só com o ServicoController (/api/servicos)
@Injectable({ providedIn: 'root' })
export class ServicoService {
  private readonly http = inject(HttpClient);

  private readonly _servicos = signal<Servico[]>([]);
  readonly servicos = this._servicos.asReadonly();

  // - o agendamento pede apenasAtivos; a tela de serviços precisa ver os desativados para religá-los
  listar(apenasAtivos?: boolean, nome?: string): Observable<Servico[]> {
    let params = new HttpParams();
    if (apenasAtivos) {
      params = params.set('apenasAtivos', true);
    }
    if (nome) {
      params = params.set('nome', nome);
    }

    return this.http.get<Servico[]>('/api/servicos', { params }).pipe(
      tap((lista) => this._servicos.set(lista)),
      catchError(traduzirErro('Não foi possível carregar os serviços.')),
    );
  }

  buscarPorId(id: number): Observable<Servico> {
    return this.http
      .get<Servico>(`/api/servicos/${id}`)
      .pipe(catchError(traduzirErro('Não foi possível carregar o serviço.')));
  }

  criar(request: ServicoRequest): Observable<Servico> {
    return this.http.post<Servico>('/api/servicos', request).pipe(
      tap((servico) => this._servicos.update((lista) => [...lista, servico])),
      catchError(traduzirErro('Não foi possível cadastrar o serviço.')),
    );
  }

  atualizar(id: number, request: ServicoRequest): Observable<Servico> {
    return this.http.put<Servico>(`/api/servicos/${id}`, request).pipe(
      tap((atualizado) =>
        this._servicos.update((lista) =>
          lista.map((servico) => (servico.id === id ? atualizado : servico)),
        ),
      ),
      catchError(traduzirErro('Não foi possível atualizar o serviço.')),
    );
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/servicos/${id}`).pipe(
      tap(() =>
        this._servicos.update((lista) => lista.filter((servico) => servico.id !== id)),
      ),
      catchError(traduzirErro('Não foi possível excluir o serviço.')),
    );
  }
}
