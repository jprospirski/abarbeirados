import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Observable, map, of, switchMap } from 'rxjs';

import { Agendamento, Horario } from '../../core/models/agendamento.model';
import { ItemCarrinho, ItemServico } from '../../core/models/servico.model';
import { AgendamentoService } from '../../core/services/agendamento.service';
import {
  DIAS_SEMANA,
  MESES,
  MESES_CURTOS,
  inicioDaSemana,
  paraDataHora,
  paraDataIso,
  paraHora,
  paraMinutos,
  somarDias,
} from '../../core/util/data.util';

interface DiaSemana {
  iso: string;
  rotulo: string;
  numero: number;
  mes: string;
  passado: boolean;
  hoje: boolean;
  /** Nenhum bloco livre para a duração escolhida. */
  lotado: boolean;
  indisponivel: boolean;
}

// A obrigatoriedade depende de outro campo do formulário, então a validação
// precisa ficar no grupo e não no controle.

/** Cliente da lista, ou nome + telefone quando for cadastro novo. */
function clienteInformado(grupo: AbstractControl): ValidationErrors | null {
  const { modoCliente, clienteId, novoNome, novoTelefone } = grupo.value;

  if (modoCliente === 'existente') {
    return clienteId ? null : { clienteObrigatorio: true };
  }

  return novoNome?.trim() && novoTelefone?.trim() ? null : { clienteObrigatorio: true };
}

/** Química só fecha com duração e valor preenchidos. */
function quimicaCompleta(grupo: AbstractControl): ValidationErrors | null {
  const { itens, quimicaDuracao, quimicaValor } = grupo.value;

  if (!itens?.includes('QUIMICA')) {
    return null;
  }

  return quimicaDuracao > 0 && quimicaValor > 0 ? null : { quimicaIncompleta: true };
}

@Component({
  selector: 'app-agendamento',
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './agendamento.component.html',
  styleUrl: './agendamento.component.scss',
})
export class AgendamentoComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly service = inject(AgendamentoService);

  protected readonly hoje = paraDataIso(new Date());

  protected readonly erro = signal<string | null>(null);
  protected readonly confirmado = signal<Agendamento | null>(null);
  /** Trava o botão enquanto o POST está em voo, para não agendar em duplicata. */
  protected readonly salvando = signal(false);
  /** Começa recolhida — é o campo menos usado da tela. */
  protected readonly mostrarObs = signal(false);

  /** Domingo da semana que está aparecendo na faixa de datas. */
  private readonly inicioSemana = signal(inicioDaSemana(new Date()));

  protected readonly form = this.fb.nonNullable.group(
    {
      modoCliente: 'existente' as 'existente' | 'novo',
      clienteId: null as number | null,
      novoNome: '',
      novoEmail: '',
      novoTelefone: '',
      itens: [[] as ItemServico[], Validators.required],
      quimicaDuracao: null as number | null,
      quimicaValor: null as number | null,
      data: [this.hoje, Validators.required],
      hora: ['', Validators.required],
      observacoes: '',
    },
    { validators: [clienteInformado, quimicaCompleta] },
  );

  private readonly valor = toSignal(
    this.form.valueChanges.pipe(map(() => this.form.getRawValue())),
    { initialValue: this.form.getRawValue() },
  );

  protected readonly modoCliente = computed(() => this.valor().modoCliente);
  protected readonly itens = computed(() => this.valor().itens);
  protected readonly temQuimica = computed(() => this.itens().includes('QUIMICA'));

  /** O Serviço único que representa a combinação marcada no carrinho. */
  protected readonly servicoResolvido = computed(() =>
    this.itens().length ? this.service.resolverServico(this.itens()) : undefined,
  );

  protected readonly duracao = computed(() => this.duracaoDe(this.valor()));

  protected readonly total = computed(() => {
    const v = this.valor();
    return this.temQuimica() ? (v.quimicaValor ?? 0) : (this.servicoResolvido()?.valor ?? 0);
  });

  /** Quanto o combo economiza em relação aos itens avulsos. */
  protected readonly economia = computed(() => {
    const itens = this.itens();
    if (itens.length < 2 || this.temQuimica()) {
      return 0;
    }
    return this.service.somaAvulsa(itens) - (this.servicoResolvido()?.valor ?? 0);
  });

  // ----------------------------------------------------------- faixa semanal

  /**
   * A semana aparece sempre inteira, de domingo a sábado. Depende da duração
   * porque um serviço longo pode lotar um dia que estava livre.
   */
  protected readonly semana = computed<DiaSemana[]>(() => {
    const inicio = this.inicioSemana();
    const duracao = this.duracao();

    return Array.from({ length: 7 }, (_, i) => {
      const dia = somarDias(inicio, i);
      const iso = paraDataIso(dia);
      const passado = iso < this.hoje;

      const lotado =
        !passado &&
        !this.service.horariosDoDia(iso, duracao).some((h) => h.disponivel);

      return {
        iso,
        rotulo: DIAS_SEMANA[dia.getDay()],
        numero: dia.getDate(),
        mes: MESES_CURTOS[dia.getMonth()],
        passado,
        hoje: iso === this.hoje,
        lotado,
        indisponivel: passado || lotado,
      };
    });
  });

  /** 'Agosto 2026', ou os dois meses quando a semana cai na virada. */
  protected readonly tituloMes = computed(() => {
    const inicio = this.inicioSemana();
    const fim = somarDias(inicio, 6);
    const ano = fim.getFullYear();

    if (inicio.getMonth() === fim.getMonth()) {
      return `${MESES[inicio.getMonth()]} ${ano}`;
    }

    return `${MESES[inicio.getMonth()]} — ${MESES[fim.getMonth()]} ${ano}`;
  });

  /** Trava a seta de voltar em semanas que já passaram por inteiro. */
  protected readonly podeVoltar = computed(
    () => paraDataIso(this.inicioSemana()) > paraDataIso(inicioDaSemana(new Date())),
  );

  // ------------------------------------------------------------- comprovante

  protected readonly nomeCliente = computed(() => {
    const v = this.valor();
    return v.modoCliente === 'existente'
      ? (this.service.buscarCliente(v.clienteId ?? 0)?.nome ?? '')
      : v.novoNome.trim();
  });

  /** 'seg, 17 de agosto' — montado à mão para não depender do locale. */
  protected readonly dataExtenso = computed(() => {
    const [ano, mes, dia] = this.valor().data.split('-').map(Number);
    const rotulo = DIAS_SEMANA[new Date(ano, mes - 1, dia).getDay()];
    return `${rotulo}, ${dia} de ${MESES[mes - 1].toLowerCase()}`;
  });

  /**
   * Só os horários reserváveis vão para a tela. O service continua calculando a
   * grade completa: o motivo de cada bloqueio ainda é útil para a tela de
   * listagem, mas aqui não é exibido.
   */
  protected readonly horarios = computed<Horario[]>(() =>
    this.service
      .horariosDoDia(this.valor().data, this.duracao())
      .filter((h) => h.disponivel),
  );

  protected readonly vagasLivres = computed(() => this.horarios().length);

  protected readonly horaFim = computed(() => {
    const hora = this.valor().hora;
    const duracao = this.duracao();
    return hora && duracao ? paraHora(paraMinutos(hora) + duracao) : '';
  });

  constructor() {
    // Mudar serviço, química ou data pode invalidar o horário já escolhido.
    this.form.controls.itens.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.revalidarHorario());

    this.form.controls.quimicaDuracao.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.revalidarHorario());

    this.form.controls.data.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.revalidarHorario());

    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.erro.set(null);
      this.confirmado.set(null);
    });
  }

  // ------------------------------------------------------------------ ações

  ngOnInit(): void {
    this.service.carregar();
  }

  protected definirModoCliente(modo: 'existente' | 'novo'): void {
    this.form.patchValue({ modoCliente: modo });
  }

  protected marcado(chave: ItemServico): boolean {
    return this.itens().includes(chave);
  }

  /** Química não soma com os outros: marcá-la limpa o resto, e vice-versa. */
  protected alternarItem(item: ItemCarrinho): void {
    const atuais = this.form.controls.itens.value;

    let proximos: ItemServico[];

    if (atuais.includes(item.chave)) {
      proximos = atuais.filter((i) => i !== item.chave);
    } else if (item.exclusivo) {
      proximos = [item.chave];
    } else {
      proximos = [...atuais.filter((i) => !this.ehExclusivo(i)), item.chave];
    }

    this.form.controls.itens.setValue(proximos);

    if (!proximos.includes('QUIMICA')) {
      this.form.patchValue({ quimicaDuracao: null, quimicaValor: null });
    }
  }

  protected semanaAnterior(): void {
    if (this.podeVoltar()) {
      this.inicioSemana.update((d) => somarDias(d, -7));
    }
  }

  protected semanaSeguinte(): void {
    this.inicioSemana.update((d) => somarDias(d, 7));
  }

  protected selecionarDia(dia: DiaSemana): void {
    if (!dia.indisponivel) {
      this.form.patchValue({ data: dia.iso });
    }
  }

  /** Apaga o texto junto: remover pela metade enviaria a observação escondida. */
  protected removerObs(): void {
    this.form.patchValue({ observacoes: '' });
    this.mostrarObs.set(false);
  }

  protected selecionarHorario(horario: Horario): void {
    if (horario.disponivel) {
      this.form.patchValue({ hora: horario.hora });
    }
  }

  protected confirmar(): void {
    this.erro.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.erro.set('Preencha os campos destacados para concluir o agendamento.');
      return;
    }

    const v = this.form.getRawValue();
    const servico = this.servicoResolvido();

    if (!servico) {
      this.erro.set('Essa combinação de serviços ainda não tem cadastro.');
      return;
    }

    this.salvando.set(true);

    // Cliente novo precisa existir no banco antes do agendamento apontar para ele.
    const clienteId$: Observable<number> =
      v.modoCliente === 'existente'
        ? of(v.clienteId!)
        : this.service
            .criarCliente({
              nome: v.novoNome,
              email: v.novoEmail.trim() || null,
              telefone: v.novoTelefone,
            })
            .pipe(map((cliente) => cliente.id));

    clienteId$
      .pipe(
        switchMap((clienteId) =>
          this.service.criar(
            {
              clienteId,
              servicoId: servico.id,
              dataHora: paraDataHora(v.data, v.hora),
              observacoes: v.observacoes,
            },
            this.duracao(),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (criado) => {
          // Mantém o dia escolhido: o normal é agendar vários clientes seguidos.
          this.form.reset({ modoCliente: 'existente', itens: [], data: v.data });
          this.mostrarObs.set(false);
          this.confirmado.set(criado);
          this.salvando.set(false);
        },
        error: (e: unknown) => {
          this.erro.set(e instanceof Error ? e.message : 'Não foi possível agendar.');
          this.salvando.set(false);
        },
      });
  }

  // ------------------------------------------------------------- validação

  protected get servicoInvalido(): boolean {
    const controle = this.form.controls.itens;
    return (
      (controle.invalid && controle.touched) ||
      (!!this.form.errors?.['quimicaIncompleta'] && this.form.touched)
    );
  }

  protected get horaInvalida(): boolean {
    const controle = this.form.controls.hora;
    return controle.invalid && controle.touched;
  }

  protected get clienteInvalido(): boolean {
    return !!this.form.errors?.['clienteObrigatorio'] && this.form.touched;
  }

  // ------------------------------------------------------------- auxiliares

  private ehExclusivo(chave: ItemServico): boolean {
    return !!this.service.itensCarrinho.find((i) => i.chave === chave)?.exclusivo;
  }

  private duracaoDe(v: ReturnType<typeof this.form.getRawValue>): number {
    if (v.itens.includes('QUIMICA')) {
      return v.quimicaDuracao ?? 0;
    }
    return this.service.resolverServico(v.itens)?.duracaoMinutos ?? 0;
  }

  /**
   * Lê a grade direto do service em vez do computed: roda dentro de uma
   * subscrição do formulário, quando o signal ainda não foi atualizado.
   */
  private revalidarHorario(): void {
    const v = this.form.getRawValue();
    if (!v.hora) {
      return;
    }

    const grade = this.service.horariosDoDia(v.data, this.duracaoDe(v));

    if (!grade.find((h) => h.hora === v.hora)?.disponivel) {
      this.form.controls.hora.setValue('');
    }
  }
}
