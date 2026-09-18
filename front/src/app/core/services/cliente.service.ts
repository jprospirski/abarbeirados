import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, tap } from 'rxjs';

import { CepResponse } from '../models/cep.model';
import { Cliente, ClienteRequest } from '../models/cliente.model';
import { traduzirErro } from '../util/erro.util';

/**
 * Camada de dados de Cliente. Fala só com o ClienteController.
 *
 *   listar(nome?)        GET    /api/clientes
 *   buscarPorId(id)      GET    /api/clientes/{id}
 *   buscarPorCep(cep)    GET    /api/clientes/cep/{cep}
 *   criar(req)           POST   /api/clientes
 *   atualizar(id, req)   PUT    /api/clientes/{id}
 *   excluir(id)          DELETE /api/clientes/{id}
 *
 * A lista fica num signal para as telas lerem síncrono no template; as escritas
 * já atualizam esse signal para ninguém precisar recarregar a listagem inteira
 * depois de salvar.
 */
@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly http = inject(HttpClient);

  private readonly _clientes = signal<Cliente[]>([]);
  readonly clientes = this._clientes.asReadonly();

  listar(nome?: string): Observable<Cliente[]> {
    const params = nome ? new HttpParams().set('nome', nome) : undefined;

    return this.http.get<Cliente[]>('/api/clientes', { params }).pipe(
      tap((lista) => this._clientes.set(lista)),
      catchError(traduzirErro('Não foi possível carregar os clientes.')),
    );
  }

  /**
   * Busca direta ao servidor, usada pela tela de edição: ela pode abrir por link
   * direto, antes da listagem completa terminar de carregar.
   */
  buscarPorId(id: number): Observable<Cliente> {
    return this.http
      .get<Cliente>(`/api/clientes/${id}`)
      .pipe(catchError(traduzirErro('Não foi possível carregar o cliente.')));
  }

  /** Consulta na ViaCEP, via Feign no backend. Só preenche a tela; não persiste. */
  buscarPorCep(cep: string): Observable<CepResponse> {
    return this.http
      .get<CepResponse>(`/api/clientes/cep/${cep}`)
      .pipe(catchError(traduzirErro('Não foi possível consultar o CEP.')));
  }

  criar(request: ClienteRequest): Observable<Cliente> {
    return this.http.post<Cliente>('/api/clientes', this.corpo(request)).pipe(
      tap((cliente) => this._clientes.update((lista) => [...lista, cliente])),
      catchError(traduzirErro('Não foi possível cadastrar o cliente.')),
    );
  }

  atualizar(id: number, request: ClienteRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`/api/clientes/${id}`, this.corpo(request)).pipe(
      tap((atualizado) =>
        this._clientes.update((lista) =>
          lista.map((cliente) => (cliente.id === id ? atualizado : cliente)),
        ),
      ),
      catchError(traduzirErro('Não foi possível atualizar o cliente.')),
    );
  }

  excluir(id: number): Observable<void> {
    return this.http.delete<void>(`/api/clientes/${id}`).pipe(
      tap(() =>
        this._clientes.update((lista) => lista.filter((cliente) => cliente.id !== id)),
      ),
      catchError(traduzirErro('Não foi possível excluir o cliente.')),
    );
  }

  /** Corpo do POST/PUT: e-mail em branco vira null, nunca string vazia — ver ClienteRequest. */
  private corpo(request: ClienteRequest): ClienteRequest {
    return {
      nome: request.nome.trim(),
      email: request.email?.trim().toLowerCase() || null,
      telefone: request.telefone.trim(),
    };
  }
}
