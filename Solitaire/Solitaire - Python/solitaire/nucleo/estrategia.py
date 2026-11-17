"""Estratégias de jogo automático.

Cobre RF28 (jogada automática e desativação), RF29 (ordem de prioridade),
RF30 (sem ciclos), RF31 (decidir separado de executar) e RNF04.

A estratégia só *decide*: recebe uma visão somente leitura do jogo
(``ConsultaJogo``) e devolve uma ``Jogada``. Quem verifica a legalidade e
executa é o ``Jogo``.

Garantia de término (RF30): cada jogada da estratégia reduz
lexicograficamente (cartas no monte, cartas no descarte, cartas fora das
fundações, cartas ocultas, colunas não vazias). Por isso a sequência de
jogadas automáticas sempre acaba, sem loops.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import TYPE_CHECKING, Optional

from .cartas import Carta
from .jogadas import Jogada, JogadaComprar, JogadaMover, JogadaNenhuma

if TYPE_CHECKING:
    from .jogo import ConsultaJogo


class Estrategia(ABC):
    @abstractmethod
    def sugerir(self, jogo: "ConsultaJogo") -> Jogada:
        """Analisa o jogo (somente leitura) e sugere uma jogada."""


class EstrategiaDesativada(Estrategia):
    """Jogo automático desligado: sempre sugere "nenhuma jogada" (objeto nulo).

    O restante do jogo a trata como qualquer outra estratégia (RNF02).
    """

    def sugerir(self, jogo: "ConsultaJogo") -> Jogada:
        return JogadaNenhuma()


class EstrategiaAutomatica(Estrategia):
    """Avalia as regras na ordem de prioridade do RF29 e sugere a primeira possível."""

    def sugerir(self, jogo: "ConsultaJogo") -> Jogada:
        regras = (
            self._comprar_se_descarte_vazio,   # 1
            self._descarte_para_fundacao,      # 2
            self._descarte_para_coluna,        # 3
            self._coluna_para_fundacao,        # 4
            self._mover_no_tableau,            # 5
            self._comprar,                     # 6
        )
        for regra in regras:
            jogada = regra(jogo)
            if jogada is not None:
                return jogada
        return JogadaNenhuma()                 # 7

    # ---- prioridade 1 ------------------------------------------------------
    @staticmethod
    def _comprar_se_descarte_vazio(jogo: "ConsultaJogo") -> Optional[Jogada]:
        if jogo.descarte.vazia() and jogo.pode_comprar():
            return JogadaComprar()
        return None

    # ---- prioridade 2 ------------------------------------------------------
    def _descarte_para_fundacao(self, jogo: "ConsultaJogo") -> Optional[Jogada]:
        carta = jogo.descarte.topo()
        return self._para_fundacao(jogo, carta) if carta is not None else None

    # ---- prioridade 3 ------------------------------------------------------
    @staticmethod
    def _descarte_para_coluna(jogo: "ConsultaJogo") -> Optional[Jogada]:
        carta = jogo.descarte.topo()
        if carta is None:
            return None
        for coluna in jogo.colunas:
            if jogo.pode_mover(carta, coluna):
                return JogadaMover(carta, coluna)
        return None

    # ---- prioridade 4 ------------------------------------------------------
    def _coluna_para_fundacao(self, jogo: "ConsultaJogo") -> Optional[Jogada]:
        for coluna in jogo.colunas:
            carta = coluna.topo()
            if carta is None:
                continue
            jogada = self._para_fundacao(jogo, carta)
            if jogada is not None:
                return jogada
        return None

    # ---- prioridade 5 ------------------------------------------------------
    @staticmethod
    def _mover_no_tableau(jogo: "ConsultaJogo") -> Optional[Jogada]:
        """Só considera mover a *primeira carta visível* de uma coluna (com tudo o
        que está sobre ela): é o único movimento que gera ganho concreto, pois
        ou revela uma carta oculta ou deixa a coluna vazia. Um Rei que já está
        na base não se move (RF20, RF30), o que evita trocas sem progresso.
        """
        for origem in jogo.colunas:
            carta = origem.primeira_visivel()
            if carta is None or origem.rei_ja_na_base(carta):
                continue
            for destino in jogo.colunas:
                if destino is not origem and jogo.pode_mover(carta, destino):
                    return JogadaMover(carta, destino)
        return None

    # ---- prioridade 6 ------------------------------------------------------
    @staticmethod
    def _comprar(jogo: "ConsultaJogo") -> Optional[Jogada]:
        return JogadaComprar() if jogo.pode_comprar() else None

    # ---- auxiliar ----------------------------------------------------------
    @staticmethod
    def _para_fundacao(jogo: "ConsultaJogo", carta: Carta) -> Optional[Jogada]:
        for fundacao in jogo.fundacoes:
            if jogo.pode_mover(carta, fundacao):
                return JogadaMover(carta, fundacao)
        return None
