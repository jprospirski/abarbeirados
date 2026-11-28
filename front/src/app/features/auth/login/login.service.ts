import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { jwtDecode, JwtPayload } from "jwt-decode";
import { Login } from './login';
import { Usuario } from '../usuario';

@Injectable({
  providedIn: 'root'
})
export class LoginService {

  http = inject(HttpClient);
  // - caminho relativo, como nos outros services: em dev o proxy do angular leva ao backend, em produção o Apache
  API = "/api/login";
  API_REGISTRO = "/api/register";

  // - espelha "há token válido" para o menu lateral reagir a login e logout
  readonly autenticado = signal(this.tokenValido());

  constructor() { }


  logar(login: Login): Observable<string> {
    return this.http.post<string>(this.API, login, {responseType: 'text' as 'json'});
  }

  registrar(login: Login): Observable<void> {
    return this.http.post<void>(this.API_REGISTRO, login);
  }

  addToken(token: string) {
    localStorage.setItem('token', token);
    this.autenticado.set(true);
  }

  removerToken() {
    localStorage.removeItem('token');
    this.autenticado.set(false);
  }

  getToken() {
    return localStorage.getItem('token');
  }

  // - token ausente, ilegível ou com exp vencido conta como sessão encerrada
  tokenValido(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }

    try {
      const exp = jwtDecode<JwtPayload>(token).exp;
      return exp !== undefined && exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  jwtDecode() {
    let token = this.getToken();
    if (token) {
      return jwtDecode<JwtPayload>(token);
    }
    return "";
  }

  hasRole(role: string) {
    let user = this.jwtDecode() as Usuario;
    if (user.role == role)
      return true;
    else
      return false;
  }

  getUsuarioLogado() {
    return this.jwtDecode() as Usuario;
  }


}
