import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import {
  Agendamento,
  STATUS_LABEL,
  StatusAgendamento,
} from '../../core/models/agendamento.model';
import { AgendamentoService } from '../../core/services/agendamento.service';
import {
  DIAS_SEMANA,
  MESES,
  MESES_CURTOS,
  dataDe,
  horaDe,
  inicioDaSemana,
  paraDataIso,
  somarDias,
} from '../../core/util/data.util';

/** As três formas de olhar a mesma lista. */
type Visao = 'cartoes' | 'lista' | 'colunas';

/** Um dia da faixa de filtro, com quantos agendamentos caem nele. */
interface DiaFiltro {
  iso: string;
  rotulo: string;
  numero: number;
  mes: string;
  hoje: boolean;
  total: number;
}

/** Ordem em que os status aparecem no quadro de colunas. */
const STATUS: StatusAgendamento[] = [
  'AGENDADO',
  'CONFIRMADO',
  'CONCLUIDO',
  'CANCELADO',
];

/** 2026-08-18T09:00:00 vira 18/08. */
function diaCurto(dataHora: string): string {
  const [, mes, dia] = dataDe(dataHora).split('-');
  return `${dia}/${mes}`;
}

@Component({
  selector: 'app-agenda',
  imports: [CurrencyPipe, RouterLink],
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
  /**
   * Em tela estreita não faz sentido oferecer as três visões: a lista vira
   * colunas espremidas e o quadro não cabe. O cartão é o único formato que lê
   * bem no celular, então ele passa a ser o único.
   */
  protected readonly ehEstreito = signal(false);
  /** O que a tela realmente desenha: no estreito, sempre cartões. */
  protected readonly visaoEfetiva = computed<Visao>(() =>
    this.ehEstreito() ? 'cartoes' : this.visao(),
  );
  protected readonly erro = signal<string | null>(null);
  /** Id em gravação, para travar só a linha que está mudando. */
  protected readonly salvandoId = signal<number | null>(null);

  /** Domingo da semana que está aparecendo na faixa. */
  private readonly inicioSemana = signal(inicioDaSemana(new Date()));
  /** ISO do dia filtrado, ou null para a agenda inteira. */
  protected readonly diaSelecionado = signal<string | null>(null);

  /**
   * Recorte por texto, aplicado antes do recorte por dia.
   *
   * A ordem importa: a contagem que aparece em cada dia da faixa sai daqui, e
   * não da lista final — assim, ao pesquisar um cliente, a faixa mostra em quais
   * dias ele tem horário, em vez de repetir o total de sempre.
   */
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

  /** Os sete dias da semana em exibição, com a contagem de cada um. */
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

  protected readonly tituloMes = computed(() => {
    const inicio = this.inicioSemana();
    const fim = somarDias(inicio, 6);
    const ano = fim.getFullYear();

    if (inicio.getMonth() === fim.getMonth()) {
      return `${MESES[inicio.getMonth()]} ${ano}`;
    }

    return `${MESES[inicio.getMonth()]} — ${MESES[fim.getMonth()]} ${ano}`;
  });

  /** Ordenados por data e hora, com os campos de exibição já prontos. */
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

  /** Uma lista por status, para o quadro de colunas. */
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
    // Mesmo ponto de corte do @media que esconde o alternador no scss.
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

  // ------------------------------------------------------------ filtro de dia

  /** Clicar no dia já selecionado desmarca e volta a mostrar a agenda inteira. */
  protected selecionarDia(iso: string): void {
    this.diaSelecionado.update((atual) => (atual === iso ? null : iso));
  }

  protected limparDia(): void {
    this.diaSelecionado.set(null);
  }

  /*
   * A faixa anda livremente para trás, ao contrário da do formulário: lá o
   * passado não pode ser reservado, aqui ele é justamente o histórico que a
   * agenda precisa mostrar.
   */
  protected semanaAnterior(): void {
    this.inicioSemana.update((d) => somarDias(d, -7));
  }

  protected semanaSeguinte(): void {
    this.inicioSemana.update((d) => somarDias(d, 7));
  }

  /** Volta a faixa para a semana corrente e mostra o dia de hoje. */
  protected irParaHoje(): void {
    this.inicioSemana.set(inicioDaSemana(new Date()));
    this.diaSelecionado.set(this.hoje);
  }

  // ------------------------------------------------------------------- status

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
          // A lista não mudou, mas o <select> já está mostrando a opção nova.
          // Recarrega para o que aparece na tela voltar a ser o que está no banco.
          this.service.carregar(true);
        },
      });
  }
}
