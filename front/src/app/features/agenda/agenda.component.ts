import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import Swal from 'sweetalert2';

import {
  Agendamento,
  STATUS_LABEL,
  StatusAgendamento,
} from '../../core/models/agendamento.model';
import { AgendamentoService } from '../../core/services/agendamento.service';
import {
  DIAS_SEMANA,
  MESES_CURTOS,
  dataDe,
  horaDe,
  inicioDaSemana,
  paraDataIso,
  rotuloMes,
  somarDias,
} from '../../core/util/data.util';
import { ConfirmarExclusaoComponent } from '../../shared/confirmar-exclusao/confirmar-exclusao.component';

// - as três formas de olhar a mesma lista
type Visao = 'cartoes' | 'lista' | 'colunas';

// - um dia da faixa de filtro, com quantos agendamentos caem nele
interface DiaFiltro {
  iso: string;
  rotulo: string;
  numero: number;
  mes: string;
  hoje: boolean;
  total: number;
}

// - ordem em que os status aparecem no quadro de colunas
const STATUS: StatusAgendamento[] = [
  'AGENDADO',
  'CONFIRMADO',
  'CONCLUIDO',
  'CANCELADO',
];

// - 2026-08-18t09:00:00 vira 18/08
function diaCurto(dataHora: string): string {
  const [, mes, dia] = dataDe(dataHora).split('-');
  return `${dia}/${mes}`;
}

@Component({
  selector: 'app-agenda',
  imports: [CurrencyPipe, RouterLink, ConfirmarExclusaoComponent, MdbFormsModule, MdbRippleModule],
  templateUrl: './agenda.component.html',
  styleUrl: './agenda.component.scss',
})
export class AgendaComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  protected readonly service = inject(AgendamentoService);

  protected readonly STATUS = STATUS;
  protected readonly STATUS_LABEL = STATUS_LABEL;

  protected readonly hoje = paraDataIso(new Date());

  protected readonly visao = signal<Visao>('cartoes');
  protected readonly busca = signal('');
  // - em tela estreita só o cartão lê bem, então as outras visões somem
  protected readonly ehEstreito = signal(false);
  // - o que a tela realmente desenha: no estreito, sempre cartões
  protected readonly visaoEfetiva = computed<Visao>(() =>
    this.ehEstreito() ? 'cartoes' : this.visao(),
  );
  protected readonly erro = signal<string | null>(null);
  // - id em gravação, para travar só a linha que está mudando
  protected readonly salvandoId = signal<number | null>(null);

  // - domingo da semana que está aparecendo na faixa
  private readonly inicioSemana = signal(inicioDaSemana(new Date()));
  // - iso do dia filtrado, ou null para a agenda inteira
  protected readonly diaSelecionado = signal<string | null>(null);

  // - recorte por texto antes do recorte por dia: a contagem da faixa sai daqui, então a busca mostra em quais dias o cliente tem horário
  private readonly porBusca = computed(() => {
    const termo = this.busca().trim().toLowerCase();

    if (!termo) {
      return this.service.agendamentos();
    }

    return this.service
      .agendamentos()
      .filter(
        (a) =>
          a.clienteNome.toLowerCase().includes(termo) ||
          a.servicoNome.toLowerCase().includes(termo),
      );
  });

  // - os sete dias da semana em exibição, com a contagem de cada um
  protected readonly semana = computed<DiaFiltro[]>(() => {
    const inicio = this.inicioSemana();
    const lista = this.porBusca();

    return Array.from({ length: 7 }, (_, i) => {
      const dia = somarDias(inicio, i);
      const iso = paraDataIso(dia);

      return {
        iso,
        rotulo: DIAS_SEMANA[dia.getDay()],
        numero: dia.getDate(),
        mes: MESES_CURTOS[dia.getMonth()],
        hoje: iso === this.hoje,
        total: lista.filter((a) => dataDe(a.dataHora) === iso).length,
      };
    });
  });

  protected readonly tituloMes = computed(() => rotuloMes(this.inicioSemana()));

  // - ordenados por data e hora, com os campos de exibição já prontos
  protected readonly agendamentos = computed(() => {
    const dia = this.diaSelecionado();

    return this.porBusca()
      .filter((a) => !dia || dataDe(a.dataHora) === dia)
      .map((a) => ({
        ...a,
        dia: diaCurto(a.dataHora),
        hora: horaDe(a.dataHora),
      }))
      .sort((a, b) => a.dataHora.localeCompare(b.dataHora));
  });

  // - uma lista por status, para o quadro de colunas
  protected readonly porStatus = computed(() => {
    const lista = this.agendamentos();

    return STATUS.map((status) => ({
      status,
      rotulo: STATUS_LABEL[status],
      itens: lista.filter((a) => a.status === status),
    }));
  });

  protected readonly resumo = computed(() => {
    const lista = this.agendamentos();

    return {
      total: lista.length,
      ativos: lista.filter((a) => a.status !== 'CANCELADO').length,
    };
  });

  constructor() {
    // - mesmo ponto de corte do @media que esconde o alternador no scss
    const consulta = window.matchMedia('(max-width: 720px)');
    const aoMudar = (evento: MediaQueryListEvent) => this.ehEstreito.set(evento.matches);

    this.ehEstreito.set(consulta.matches);
    consulta.addEventListener('change', aoMudar);
    this.destroyRef.onDestroy(() => consulta.removeEventListener('change', aoMudar));
  }

  ngOnInit(): void {
    this.service.carregar();
  }

  protected definirVisao(visao: Visao): void {
    this.visao.set(visao);
  }

  protected atualizarBusca(evento: Event): void {
    this.busca.set((evento.target as HTMLInputElement).value);
  }

  // - clicar no dia já selecionado desmarca e volta a mostrar a agenda inteira
  protected selecionarDia(iso: string): void {
    this.diaSelecionado.update((atual) => (atual === iso ? null : iso));
  }

  protected limparDia(): void {
    this.diaSelecionado.set(null);
  }

  // - a faixa anda livremente para trás, ao contrário da do formulário: aqui o passado é histórico
  protected semanaAnterior(): void {
    this.inicioSemana.update((d) => somarDias(d, -7));
  }

  protected semanaSeguinte(): void {
    this.inicioSemana.update((d) => somarDias(d, 7));
  }

  // - volta a faixa para a semana corrente e mostra o dia de hoje
  protected irParaHoje(): void {
    this.inicioSemana.set(inicioDaSemana(new Date()));
    this.diaSelecionado.set(this.hoje);
  }

  // - agendamento com o modal de confirmação aberto, ou null quando fechado
  protected readonly exclusaoAlvo = signal<Agendamento | null>(null);
  // - frase que vai em negrito no modal: "o agendamento de fulano"
  protected readonly alvoExclusao = computed(() => {
    const alvo = this.exclusaoAlvo();
    return alvo ? `o agendamento de ${alvo.clienteNome}` : '';
  });

  protected readonly excluindo = signal(false);
  protected readonly erroExclusao = signal<string | null>(null);

  protected pedirExclusao(agendamento: Agendamento): void {
    this.erroExclusao.set(null);
    this.exclusaoAlvo.set(agendamento);
  }

  protected cancelarExclusao(): void {
    if (this.excluindo()) {
      return;
    }
    this.exclusaoAlvo.set(null);
    this.erroExclusao.set(null);
  }

  protected confirmarExclusao(): void {
    const alvo = this.exclusaoAlvo();
    if (!alvo) {
      return;
    }

    this.excluindo.set(true);
    this.erroExclusao.set(null);

    this.service
      .excluir(alvo.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.excluindo.set(false);
          this.exclusaoAlvo.set(null);
          Swal.fire({ icon: 'success', title: 'Agendamento excluído!', timer: 1600, showConfirmButton: false });
        },
        error: (e: unknown) => {
          this.erroExclusao.set(
            e instanceof Error ? e.message : 'Não foi possível excluir o agendamento.',
          );
          this.excluindo.set(false);
        },
      });
  }

  protected mudarStatus(agendamento: Agendamento, evento: Event): void {
    const status = (evento.target as HTMLSelectElement).value as StatusAgendamento;

    if (status === agendamento.status) {
      return;
    }

    this.erro.set(null);
    this.salvandoId.set(agendamento.id);

    this.service
      .atualizarStatus(agendamento.id, status)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.salvandoId.set(null),
        error: (e: unknown) => {
          this.erro.set(
            e instanceof Error ? e.message : 'Não foi possível alterar o status.',
          );
          this.salvandoId.set(null);
          // - o <select> já mostra a opção nova mas a lista não mudou: recarrega para voltar ao que está no banco
          this.service.carregar(true);
        },
      });
  }
}
