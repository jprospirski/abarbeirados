import { Injectable, effect, signal } from '@angular/core';

export type Tema = 'escuro' | 'claro';

// - só escreve data-tema no <html>; o styles.scss redefine as variáveis css sob esse seletor
@Injectable({ providedIn: 'root' })
export class TemaService {
  private static readonly CHAVE = 'abarbeirados:tema';

  readonly tema = signal<Tema>(TemaService.temaInicial());

  constructor() {
    effect(() => {
      const tema = this.tema();
      document.documentElement.dataset['tema'] = tema;

      try {
        localStorage.setItem(TemaService.CHAVE, tema);
      } catch {
        // - navegação privativa bloqueia o storage; a preferência é opcional e perdê-la não derruba a aplicação
      }
    });
  }

  definir(tema: Tema): void {
    this.tema.set(tema);
  }

  alternar(): void {
    this.tema.update((t) => (t === 'escuro' ? 'claro' : 'escuro'));
  }

  private static temaInicial(): Tema {
    try {
      const salvo = localStorage.getItem(TemaService.CHAVE);
      if (salvo === 'claro' || salvo === 'escuro') {
        return salvo;
      }
    } catch {
      // - sem acesso ao storage: cai no padrão
    }
    return 'escuro';
  }
}
