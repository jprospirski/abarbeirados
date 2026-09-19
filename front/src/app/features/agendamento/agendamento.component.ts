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
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, map, of, switchMap } from 'rxjs';
import { MdbFormsModule } from 'mdb-angular-ui-kit/forms';
import { MdbRippleModule } from 'mdb-angular-ui-kit/ripple';
import Swal from 'sweetalert2';

import { Agendamento, Horario } from '../../core/models/agendamento.model';
import { ItemCarrinho, ItemServico } from '../../core/models/servico.model';
import { AgendamentoService } from '../../core/services/agendamento.service';
import { BarbeiroService } from '../../core/services/barbeiro.service';
import { ClienteService } from '../../core/services/cliente.service';
import { ServicoService } from '../../core/services/servico.service';
import {
  DIAS_SEMANA,
  MESES,
  MESES_CURTOS,
  dataDe,
  horaDe,
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
  // - nenhum bloco livre para a duração escolhida
  lotado: boolean;
  indisponivel: boolean;
}

// - a obrigatoriedade depende do modo do cliente, por isso o validador fica no grupo e não no controle
function clienteInformado(grupo: AbstractControl): ValidationErrors | null {
  const { modoCliente, clienteId, novoNome, novoTelefone } = grupo.value;

  if (modoCliente === 'existente') {
    return clienteId ? null : { clienteObrigatorio: true };
  }

  return novoNome?.trim() && novoTelefone?.trim() ? null : { clienteObrigatorio: true };
}

// - texto legível de cada motivo de bloqueio da grade
const MOTIVO_LABEL: Record<NonNullable<Horario['motivo']>, string> = {
  passado: 'Horário já passou',
  ocupado: 'Barbeiro ocupado nesse horário',
  'sem-tempo': 'Não cabe até o fechamento',
};

// - cadastro e edição no mesmo componente: o :id da rota decide; na edição o próprio id sai da checagem de conflito
@Component({
  selector: 'app-agendamento',
  imports: [ReactiveFormsModule, CurrencyPipe, RouterLink, MdbFormsModule, MdbRippleModule],
  templateUrl: './agendamento.component.html',
  styleUrl: './agendamento.component.scss',
})
export class AgendamentoComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly service = inject(AgendamentoService);
  protected readonly barbeiroService = inject(BarbeiroService);
  // - cada service cuida do próprio dado: cliente e catálogo de serviços vêm dos donos
  protected readonly clienteService = inject(ClienteService);
  protected readonly servicoService = inject(ServicoService);

  protected readonly MOTIVO_LABEL = MOTIVO_LABEL;

  protected readonly hoje = paraDataIso(new Date());

  // - id do agendamento em edição, ou null no cadastro
  protected readonly editandoId = signal<number | null>(null);
  protected readonly modoEdicao = computed(() => this.editandoId() !== null);

  protected readonly erro = signal<string | null>(null);
  protected readonly confirmado = signal<Agendamento | null>(null);
  // - trava o botão enquanto o post está em voo, para não agendar em duplicata
  protected readonly salvando = signal(false);
  // - começa recolhida por ser o campo menos usado
  protected readonly mostrarObs = signal(false);

  // - domingo da semana que está aparecendo na faixa de datas
  private readonly inicioSemana = signal(inicioDaSemana(new Date()));

  protected readonly form = this.fb.nonNullable.group(
    {
      modoCliente: 'existente' as 'existente' | 'novo',
      clienteId: null as number | null,
      novoNome: '',
      novoEmail: '',
      novoTelefone: '',
      barbeiroId: [null as number | null, Validators.required],
      itens: [[] as ItemServico[], Validators.required],
      data: [this.hoje, Validators.required],
      hora: ['', Validators.required],
      observacoes: '',
    },
    { validators: [clienteInformado] },
  );

  private readonly valor = toSignal(
    this.form.valueChanges.pipe(map(() => this.form.getRawValue())),
    { initialValue: this.form.getRawValue() },
  );

  protected readonly modoCliente = computed(() => this.valor().modoCliente);
  protected readonly barbeiroId = computed(() => this.valor().barbeiroId);
  protected readonly itens = computed(() => this.valor().itens);

  // - resolverServico busca duração/valor no catálogo, nunca aceita valor customizado
  protected readonly servicoResolvido = computed(() =>
    this.itens().length
      ? this.service.resolverServico(this.itens(), this.servicoService.servicos())
      : undefined,
  );

  protected readonly duracao = computed(() => this.duracaoDe(this.valor()));

  protected readonly total = computed(() => this.servicoResolvido()?.valor ?? 0);

  // - quanto o combo economiza em relação aos itens avulsos
  protected readonly economia = computed(() => {
    const itens = this.itens();
    if (itens.length < 2) {
      return 0;
    }
    return this.service.somaAvulsa(itens) - (this.servicoResolvido()?.valor ?? 0);
  });

  // - semana inteira de domingo a sábado; depende da duração porque um serviço longo pode lotar um dia livre
  protected readonly semana = computed<DiaSemana[]>(() => {
    const inicio = this.inicioSemana();
    const duracao = this.duracao();
    const barbeiroId = this.barbeiroId();
    const ignorarId = this.editandoId() ?? undefined;

    return Array.from({ length: 7 }, (_, i) => {
      const dia = somarDias(inicio, i);
      const iso = paraDataIso(dia);
      const passado = iso < this.hoje;

      const lotado =
        !passado &&
        !this.service
          .horariosDoDia(iso, duracao, ignorarId, barbeiroId)
          .some((h) => h.disponivel);

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

  // - 'agosto 2026', ou os dois meses quando a semana cai na virada
  protected readonly tituloMes = computed(() => {
    const inicio = this.inicioSemana();
    const fim = somarDias(inicio, 6);
    const ano = fim.getFullYear();

    if (inicio.getMonth() === fim.getMonth()) {
      return `${MESES[inicio.getMonth()]} ${ano}`;
    }

    return `${MESES[inicio.getMonth()]} — ${MESES[fim.getMonth()]} ${ano}`;
  });

  // - trava a seta de voltar em semanas que já passaram por inteiro
  protected readonly podeVoltar = computed(
    () => paraDataIso(this.inicioSemana()) > paraDataIso(inicioDaSemana(new Date())),
  );

  protected readonly nomeCliente = computed(() => {
    const v = this.valor();
    return v.modoCliente === 'existente'
      ? (this.clienteService.clientes().find((c) => c.id === v.clienteId)?.nome ?? '')
      : v.novoNome.trim();
  });

  protected readonly nomeBarbeiro = computed(
    () => this.barbeiroService.barbeiros().find((b) => b.id === this.barbeiroId())?.nome ?? '',
  );

  // - 'seg, 17 de agosto', montado à mão para não depender do locale
  protected readonly dataExtenso = computed(() => {
    const [ano, mes, dia] = this.valor().data.split('-').map(Number);
    const rotulo = DIAS_SEMANA[new Date(ano, mes - 1, dia).getDay()];
    return `${rotulo}, ${dia} de ${MESES[mes - 1].toLowerCase()}`;
  });

  // - grade inteira do dia, bloqueados inclusive, para o atendente ver o motivo de cada horário fechado
  protected readonly horarios = computed<Horario[]>(() =>
    this.service.horariosDoDia(
      this.valor().data,
      this.duracao(),
      this.editandoId() ?? undefined,
      this.barbeiroId(),
    ),
  );

  protected readonly vagasLivres = computed(
    () => this.horarios().filter((h) => h.disponivel).length,
  );

  protected readonly horaFim = computed(() => {
    const hora = this.valor().hora;
    const duracao = this.duracao();
    return hora && duracao ? paraHora(paraMinutos(hora) + duracao) : '';
  });

  constructor() {
    // - mudar serviço, barbeiro ou data pode invalidar o horário já escolhido
    this.form.controls.itens.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.revalidarHorario());

    this.form.controls.data.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.revalidarHorario());

    this.form.controls.barbeiroId.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.revalidarHorario());

    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.erro.set(null);
      this.confirmado.set(null);
    });
  }

  ngOnInit(): void {
    this.service.carregar();
    this.barbeiroService.listar(true).pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
    this.clienteService.listar().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      error: (e: unknown) => this.erro.set(
        e instanceof Error ? e.message : 'Não foi possível carregar os clientes.',
      ),
    });
    // - só os ativos: o carrinho não deve resolver para um serviço desligado
    this.servicoService.listar(true).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      error: (e: unknown) => this.erro.set(
        e instanceof Error ? e.message : 'Não foi possível carregar os serviços.',
      ),
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      return;
    }

    const id = Number(idParam);
    this.editandoId.set(id);

    this.service
      .obterPorId(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (agendamento) => this.preencher(agendamento),
        error: (e: unknown) => {
          this.erro.set(
            e instanceof Error ? e.message : 'Não foi possível carregar o agendamento.',
          );
        },
      });
  }

  // - leva o agendamento gravado para o formulário; o carrinho é remontado pelo nome do serviço
  private preencher(agendamento: Agendamento): void {
    const itens = this.service.itensDoServico(agendamento.servicoNome);
    const data = dataDe(agendamento.dataHora);

    // - a faixa precisa mostrar a semana do agendamento, senão a data selecionada fica fora da vista
    const [ano, mes, dia] = data.split('-').map(Number);
    this.inicioSemana.set(inicioDaSemana(new Date(ano, mes - 1, dia)));

    this.form.reset({
      modoCliente: 'existente',
      clienteId: agendamento.clienteId,
      barbeiroId: agendamento.barbeiroId,
      itens,
      data,
      hora: horaDe(agendamento.dataHora),
      observacoes: agendamento.observacoes ?? '',
    });
    this.mostrarObs.set(!!agendamento.observacoes);
  }

  protected definirModoCliente(modo: 'existente' | 'novo'): void {
    this.form.patchValue({ modoCliente: modo });
  }

  // - telefone só com dígitos, como o clienterequest exige
  protected somenteDigitos(evento: Event): void {
    const digitos = (evento.target as HTMLInputElement).value.replace(/\D/g, '').slice(0, 11);
    this.form.controls.novoTelefone.setValue(digitos);
  }

  protected marcado(chave: ItemServico): boolean {
    return this.itens().includes(chave);
  }

  // - química não soma com os outros: marcá-la limpa o resto, e vice-versa
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

  // - apaga o texto junto: remover só o campo enviaria a observação escondida
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

    // - cliente novo precisa existir no banco antes do agendamento apontar para ele
    const clienteId$: Observable<number> =
      v.modoCliente === 'existente'
        ? of(v.clienteId!)
        : this.clienteService
            .criar({
              nome: v.novoNome,
              email: v.novoEmail.trim() || null,
              telefone: v.novoTelefone,
            })
            .pipe(map((cliente) => cliente.id));

    const id = this.editandoId();

    clienteId$
      .pipe(
        switchMap((clienteId) => {
          const request = {
            clienteId,
            servicoId: servico.id,
            barbeiroId: v.barbeiroId!,
            dataHora: paraDataHora(v.data, v.hora),
            observacoes: v.observacoes,
          };

          return id
            ? this.service.atualizar(id, request, this.duracao())
            : this.service.criar(request, this.duracao());
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (salvo) => {
          this.salvando.set(false);

          if (id) {
            // - editar é correção pontual: volta para a agenda, como os outros cadastros
            Swal.fire({ icon: 'success', title: 'Agendamento atualizado!', timer: 1600, showConfirmButton: false });
            this.router.navigate(['/agendamentos']);
            return;
          }

          // - mantém o dia escolhido: o normal é agendar vários clientes seguidos
          this.form.reset({ modoCliente: 'existente', itens: [], data: v.data });
          this.mostrarObs.set(false);
          this.confirmado.set(salvo);
          Swal.fire({ icon: 'success', title: 'Agendamento criado!', timer: 1600, showConfirmButton: false });
        },
        error: (e: unknown) => {
          const mensagem = e instanceof Error ? e.message : 'Não foi possível agendar.';

          this.erro.set(mensagem);
          this.salvando.set(false);
          Swal.fire({ icon: 'error', title: 'Não deu para agendar', text: mensagem });
        },
      });
  }

  protected get servicoInvalido(): boolean {
    const controle = this.form.controls.itens;
    return controle.invalid && controle.touched;
  }

  protected get barbeiroInvalido(): boolean {
    const controle = this.form.controls.barbeiroId;
    return controle.invalid && controle.touched;
  }

  protected get horaInvalida(): boolean {
    const controle = this.form.controls.hora;
    return controle.invalid && controle.touched;
  }

  protected get clienteInvalido(): boolean {
    return !!this.form.errors?.['clienteObrigatorio'] && this.form.touched;
  }

  private ehExclusivo(chave: ItemServico): boolean {
    return !!this.service.itensCarrinho.find((i) => i.chave === chave)?.exclusivo;
  }

  private duracaoDe(v: ReturnType<typeof this.form.getRawValue>): number {
    return this.service.resolverServico(v.itens, this.servicoService.servicos())?.duracaoMinutos ?? 0;
  }

  // - lê a grade direto do service e não do computed: roda numa subscrição do form, antes do signal atualizar
  private revalidarHorario(): void {
    const v = this.form.getRawValue();
    if (!v.hora) {
      return;
    }

    const grade = this.service.horariosDoDia(
      v.data,
      this.duracaoDe(v),
      this.editandoId() ?? undefined,
      v.barbeiroId,
    );

    if (!grade.find((h) => h.hora === v.hora)?.disponivel) {
      this.form.controls.hora.setValue('');
    }
  }
}
