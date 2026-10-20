import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, tap } from 'rxjs';

import { CepResponse } from '../models/cep.model';
import { Cliente, ClienteRequest } from '../models/cliente.model';
import { traduzirErro } from '../util/erro.util';

// - fala só com o ClienteController (/api/clientes)
// - a lista fica num signal e as escritas já o atualizam, sem recarregar a listagem depois de salvar
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

  // - busca direta ao servidor: a edição pode abrir por link antes da listagem carregar
  buscarPorId(id: number): Observable<Cliente> {
    return this.http
      .get<Cliente>(`/api/clientes/${id}`)
      .pipe(catchError(traduzirErro('Não foi possível carregar o cliente.')));
  }

  // - consulta na viacep via feign no backend; só preenche a tela, não persiste
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

  // - e-mail em branco vira null, nunca string vazia
  private corpo(request: ClienteRequest): ClienteRequest {
    return {
      nome: request.nome.trim(),
      email: request.email?.trim().toLowerCase() || null,
      telefone: request.telefone.trim(),
    };
  }
}
