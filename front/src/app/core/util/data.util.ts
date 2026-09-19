export function paraDataIso(data: Date): string {
  const mes = `${data.getMonth() + 1}`.padStart(2, '0');
  const dia = `${data.getDate()}`.padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}

export function paraMinutos(hora: string): number {
  const [h, m] = hora.split(':').map(Number);
  return h * 60 + m;
}

export function paraHora(minutos: number): string {
  const h = `${Math.floor(minutos / 60)}`.padStart(2, '0');
  const m = `${minutos % 60}`.padStart(2, '0');
  return `${h}:${m}`;
}

export function paraDataHora(data: string, hora: string): string {
  return `${data}T${hora}`;
}

export function horaDe(dataHora: string): string {
  return dataHora.slice(11, 16);
}

export function dataDe(dataHora: string): string {
  return dataHora.slice(0, 10);
}

export function inicioDaSemana(data: Date): Date {
  const d = new Date(data.getFullYear(), data.getMonth(), data.getDate());
  d.setDate(d.getDate() - d.getDay());
  return d;
}

export function somarDias(data: Date, dias: number): Date {
  const d = new Date(data.getFullYear(), data.getMonth(), data.getDate());
  d.setDate(d.getDate() + dias);
  return d;
}

export const DIAS_SEMANA = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];

export const MESES_CURTOS = [
  'jan', 'fev', 'mar', 'abr', 'mai', 'jun',
  'jul', 'ago', 'set', 'out', 'nov', 'dez',
];

export const MESES = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
];

export function rotuloMes(inicio: Date): string {
  const fim = somarDias(inicio, 6);
  const ano = fim.getFullYear();

  if (inicio.getMonth() === fim.getMonth()) {
    return `${MESES[inicio.getMonth()]} ${ano}`;
  }

  return `${MESES[inicio.getMonth()]} — ${MESES[fim.getMonth()]} ${ano}`;
}

export function haConflito(
  inicioA: number,
  duracaoA: number,
  inicioB: number,
  duracaoB: number,
): boolean {
  return inicioA < inicioB + duracaoB && inicioB < inicioA + duracaoA;
}
