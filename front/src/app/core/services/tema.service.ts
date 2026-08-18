import { Injectable, effect, signal } from '@angular/core';

export type Tema = 'escuro' | 'claro';

/**
 * Alterna a paleta da interface.
 *
 * A troca é instantânea porque nada recarrega: o serviço só escreve `data-tema`
 * no <html> e o styles.scss redefine as variáveis CSS sob esse seletor. Como
 * todo componente lê as mesmas variáveis, nenhum precisa saber qual tema está
 * ativo.
 */
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
        // Navegação privativa bloqueia o storage. A preferência é opcional,
        // então perder ela não pode derrubar a aplicação.
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
      // Sem acesso ao storage: cai no padrão.
    }
    return 'escuro';
  }
}
