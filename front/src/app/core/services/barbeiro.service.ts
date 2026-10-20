import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, tap } from 'rxjs';

import { Barbeiro, BarbeiroRequest } from '../models/barbeiro.model';
import { Servico } from '../models/servico.model';
import { traduzirErro } from '../util/erro.util';

// - fala só com o BarbeiroController (/api/barbeiros)
@Injectable({ providedIn: 'root' })
export class BarbeiroService {
  private readonly http = inject(HttpClient);

  private readonly _barbeiros = signal<Barbeiro[]>([]);
  readonly barbeiros = this._barbeiros.asReadonly();

  listar(apenasAtivos?: boolean, nome?: string): Observable<Barbeiro[]> {
    let params = new HttpParams();
    if (apenasAtivos) {
      params = params.set('apenasAtivos', true);
    }
    if (nome) {
      params = params.set('nome', nome);
    }

    return this.http.get<Barbeiro[]>('/api/barbeiros', { params }).pipe(
      tap((lista) => this._barbeiros.set(lista)),
      catchError(traduzirErro('Não foi possível carregar os barbeiros.')),
    );
  }

  buscarPorId(id: number): Observable<Barbeiro> {
    return this.http
      .get<Barbeiro>(`/api/barbeiros/${id}`)
      .pipe(catchError(traduzirErro('Não foi possível carregar o barbeiro.')));
  }

  // - serviços na forma completa de /api/servicos, com valor e duração que o resumo em Barbeiro.servicos não traz
  servicosDoBarbeiro(id: number): Observable<Servico[]> {
    return this.http
      .get<Servico[]>(`/api/barbeiros/${id}/servicos`)
      .pipe(catchError(traduzirErro('Não foi possível carregar os serviços do barbeiro.')));
  }

  criar(request: BarbeiroRequest): Observable<Barbeiro> {
    return this.http.post<Barbeiro>('/api/barbeiros', request).pipe(
      tap((barbeiro) => this._barbeiros.update((lista) => [...lista, barbeiro])),
      catchError(traduzirErro('Não foi possível cadastrar o barbeiro.')),
    );
  }

  atualizar(id: number, request: BarbeiroRequest): Observable<Barbeiro> {
    return this.http.put<Barbeiro>(`/api/barbeiros/${id}`, request).pipe(
      tap((atualizado) =>
        this._barbeiros.update((lista) =>
          lista.map((barbeiro) => (barbeiro.id === id ? atualizado : barbeiro)),
        ),
      ),
      catchError(traduzirErro('Não foi possível atualizar o barbeiro.')),
    );
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/barbeiros/${id}`).pipe(
      tap(() =>
        this._barbeiros.update((lista) => lista.filter((barbeiro) => barbeiro.id !== id)),
      ),
      catchError(traduzirErro('Não foi possível excluir o barbeiro.')),
    );
  }
}
