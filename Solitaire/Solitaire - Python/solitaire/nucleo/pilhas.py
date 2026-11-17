"""Pilhas e locais do jogo.

Cobre:
    RF08 - estrutura de pilha comum a monte, descarte, fundações e colunas;
    RF09 - consultas (``vazia``, ``topo``);
    RF10 - locais tratados de forma equivalente;
    RF13 - o descarte não aceita movimentação direta;
    RF14/RF15 - fundações;
    RF16/RF17/RF18/RF20 - colunas do tableau.

Convenção: métodos de consulta nunca alteram a pilha (RF27). Os métodos
estruturais (``colocar``, ``remover_topo``, ``retirar``, ``depositar``,
``limpar``) existem para as *ações* do jogo; a legalidade é decidida por
``aceita`` e verificada pelo ``Jogo`` antes de qualquer ação ser criada.
"""
from __future__ import annotations

from typing import Optional, Sequence

from . import regras
from .cartas import Carta, Rank
from .erros import ErroDeJogo


class Pilha:
    """Pilha ordenada da base ao topo (RF08)."""

    def __init__(self) -> None:
        self._cartas: list[Carta] = []

    # ---- consultas (somente leitura) -------------------------------------
    @property
    def cartas(self) -> tuple[Carta, ...]:
        """Cartas da base ao topo (cópia imutável)."""
        return tuple(self._cartas)

    def __len__(self) -> int:
        return len(self._cartas)

    def vazia(self) -> bool:
        return not self._cartas

    def topo(self) -> Optional[Carta]:
        """Carta do topo sem removê-la, ou ``None`` se a pilha estiver vazia."""
        return self._cartas[-1] if self._cartas else None

    def indice_de(self, carta: Carta) -> int:
        """Posição da carta (0 = base) ou -1. Compara por identidade."""
        for i, c in enumerate(self._cartas):
            if c is carta:
                return i
        return -1

    def contem(self, carta: Carta) -> bool:
        return self.indice_de(carta) >= 0

    # ---- operações estruturais (uso das ações) ---------------------------
    def colocar(self, carta: Carta) -> None:
        self._cartas.append(carta)

    def remover_topo(self) -> Carta:
        if not self._cartas:
            raise ErroDeJogo("Pilha vazia.")
        return self._cartas.pop()

    def limpar(self) -> None:
        self._cartas.clear()


class Local(Pilha):
    """Uma pilha que é um local do jogo: coluna, fundação, monte ou descarte (RF10).

    Define a interface que o jogo usa sem se importar com o tipo concreto.
    """

    def __init__(self, nome: str) -> None:
        super().__init__()
        self.nome = nome

    # ---- regras ----------------------------------------------------------
    def aceita(self, cartas: Sequence[Carta]) -> bool:
        """Este local aceita receber ``cartas`` (da base ao topo) por movimentação?"""
        return False

    # ---- consultas -------------------------------------------------------
    def cartas_acessiveis(self) -> tuple[Carta, ...]:
        """Cartas que o jogador pode pegar neste local (por padrão, só o topo)."""
        topo = self.topo()
        return (topo,) if topo is not None else ()

    def sequencia_a_partir_de(self, carta: Carta) -> tuple[Carta, ...]:
        """Cartas que se movem junto com ``carta`` (vazio se não for acessível)."""
        return (carta,) if self.topo() is carta else ()

    def carta_a_revelar_apos_retirar(self, cartas: Sequence[Carta]) -> Optional[Carta]:
        """Carta oculta que ficaria exposta se ``cartas`` fossem retiradas (RF19)."""
        return None

    # ---- operações estruturais -------------------------------------------
    def retirar(self, cartas: Sequence[Carta]) -> None:
        """Remove ``cartas`` deste local. Por padrão, só o topo pode sair (RF08)."""
        if len(cartas) != 1 or self.topo() is not cartas[0]:
            raise ErroDeJogo(f"Só o topo de '{self.nome}' pode ser retirado.")
        self._cartas.pop()

    def depositar(self, cartas: Sequence[Carta]) -> None:
        """Coloca ``cartas`` no topo, sem checar regras (a legalidade é do ``Jogo``)."""
        if len(cartas) != 1:
            raise ErroDeJogo(f"'{self.nome}' recebe uma carta por vez.")
        self._cartas.append(cartas[0])

    def __repr__(self) -> str:
        return f"<{type(self).__name__} {self.nome!r} {list(self._cartas)}>"


class Monte(Local):
    """Cartas ainda não compradas, viradas para baixo (RF11)."""

    def cartas_acessiveis(self) -> tuple[Carta, ...]:
        return ()  # só se interage com o monte pela ação de comprar

    def sequencia_a_partir_de(self, carta: Carta) -> tuple[Carta, ...]:
        return ()


class Descarte(Local):
    """Cartas compradas, viradas para cima. Só recebe cartas pela compra (RF13)."""

    def aceita(self, cartas: Sequence[Carta]) -> bool:
        return False


class Fundacao(Local):
    """Fundação: do Ás ao Rei, um naipe (RF14, RF15)."""

    def aceita(self, cartas: Sequence[Carta]) -> bool:
        return len(cartas) == 1 and regras.pode_ir_para_fundacao(cartas[0], self.topo())


class Coluna(Local):
    """Coluna do tableau (RF16 a RF20)."""

    def aceita(self, cartas: Sequence[Carta]) -> bool:
        return (
            len(cartas) > 0
            and regras.e_sequencia_valida(cartas)
            and regras.pode_ir_para_coluna(cartas[0], self.topo())
        )

    # ---- consultas ao tableau (RF20) --------------------------------------
    def cartas_visiveis(self) -> tuple[Carta, ...]:
        return tuple(c for c in self._cartas if c.visivel)

    def cartas_acessiveis(self) -> tuple[Carta, ...]:
        return self.cartas_visiveis()

    def carta_visivel(self, carta: Carta) -> bool:
        """A carta está nesta coluna e está virada para cima?"""
        return carta.visivel and self.contem(carta)

    def primeira_visivel(self) -> Optional[Carta]:
        """Carta visível mais próxima da base, ou ``None``."""
        for c in self._cartas:
            if c.visivel:
                return c
        return None

    def e_primeira_visivel(self, carta: Carta) -> bool:
        return self.primeira_visivel() is carta

    def rei_ja_na_base(self, carta: Carta) -> bool:
        """Rei visível que já está na base da coluna: movê-lo para outra coluna
        vazia não traria ganho algum."""
        return carta.rank is Rank.REI and carta.visivel and self.indice_de(carta) == 0

    def sequencia_a_partir_de(self, carta: Carta) -> tuple[Carta, ...]:
        """Da ``carta`` até o topo: as cartas que seriam movidas junto (RF18)."""
        i = self.indice_de(carta)
        if i < 0 or not carta.visivel:
            return ()
        return tuple(self._cartas[i:])

    def carta_a_revelar_apos_retirar(self, cartas: Sequence[Carta]) -> Optional[Carta]:
        i = self.indice_de(cartas[0])
        if i > 0 and not self._cartas[i - 1].visivel:
            return self._cartas[i - 1]
        return None

    # ---- operações estruturais -------------------------------------------
    def retirar(self, cartas: Sequence[Carta]) -> None:
        n = len(cartas)
        cauda = self._cartas[-n:] if n else []
        if n == 0 or len(cauda) != n or any(a is not b for a, b in zip(cauda, cartas)):
            raise ErroDeJogo(f"As cartas não são o final de '{self.nome}'.")
        del self._cartas[-n:]

    def depositar(self, cartas: Sequence[Carta]) -> None:
        self._cartas.extend(cartas)
