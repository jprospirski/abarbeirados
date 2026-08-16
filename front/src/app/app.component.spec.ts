import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit } from '@angular/core';

interface Servico {
  id: number;
  nome: string;
  valor: number;
  duracaoMinutos: number;
  ativo: boolean;
}

@Component({
  selector: 'app-root',
  imports: [DecimalPipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/servicos';

  servicos: Servico[] = [];
  busca = '';
  carregando = true;
  erro = '';

  get servicosFiltrados(): Servico[] {
    const termo = this.busca.trim().toLowerCase();
    return termo
      ? this.servicos.filter((servico) => servico.nome.toLowerCase().includes(termo))
      : this.servicos;
  }

  ngOnInit(): void {
    this.http.get<Servico[]>(this.apiUrl).subscribe({
      next: (servicos) => {
        this.servicos = servicos;
        this.carregando = false;
      },
      error: () => {
        this.erro = 'Não foi possível carregar os serviços.';
        this.carregando = false;
      }
    });
  }

  atualizarBusca(event: Event): void {
    this.busca = (event.target as HTMLInputElement).value;
  }
}
