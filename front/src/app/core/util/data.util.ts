// Utilitários de data e hora usados pela grade e pela faixa semanal.

/**
 * Date -> 'yyyy-MM-dd' no fuso local.
 *
 * Feito à mão de propósito: toISOString() converte para UTC e, à noite, devolve
 * o dia seguinte para quem está no Brasil.
 */
export function paraDataIso(data: Date): string {
  const mes = `${data.getMonth() + 1}`.padStart(2, '0');
  const dia = `${data.getDate()}`.padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}

// A grade trabalha em minutos desde a meia-noite: comparar número é mais
// simples e menos sujeito a erro do que comparar 'HH:mm'.

export function paraMinutos(hora: string): number {
  const [h, m] = hora.split(':').map(Number);
  return h * 60 + m;
}

export function paraHora(minutos: number): string {
  const h = `${Math.floor(minutos / 60)}`.padStart(2, '0');
  const m = `${minutos % 60}`.padStart(2, '0');
  return `${h}:${m}`;
}

// Conversões de/para o LocalDateTime do Java: 'yyyy-MM-ddTHH:mm'.

export function paraDataHora(data: string, hora: string): string {
  return `${data}T${hora}`;
}

export function horaDe(dataHora: string): string {
  return dataHora.slice(11, 16);
}

export function dataDe(dataHora: string): string {
  return dataHora.slice(0, 10);
}

/** Domingo da semana que contém a data, zerado na meia-noite local. */
export function inicioDaSemana(data: Date): Date {
  const d = new Date(data.getFullYear(), data.getMonth(), data.getDate());
  d.setDate(d.getDate() - d.getDay());
  return d;
}

/** Nova data deslocada em N dias, sem mexer na original. */
export function somarDias(data: Date, dias: number): Date {
  const d = new Date(data.getFullYear(), data.getMonth(), data.getDate());
  d.setDate(d.getDate() + dias);
  return d;
}

// Rótulos fixos em vez de Intl: previsíveis e sem depender do locale do
// navegador, que varia entre as máquinas do grupo.

export const DIAS_SEMANA = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];

export const MESES_CURTOS = [
  'jan', 'fev', 'mar', 'abr', 'mai', 'jun',
  'jul', 'ago', 'set', 'out', 'nov', 'dez',
];

export const MESES = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
];

/** Os intervalos [início, início+duração) se sobrepõem? */
export function haConflito(
  inicioA: number,
  duracaoA: number,
  inicioB: number,
  duracaoB: number,
): boolean {
  return inicioA < inicioB + duracaoB && inicioB < inicioA + duracaoA;
}
