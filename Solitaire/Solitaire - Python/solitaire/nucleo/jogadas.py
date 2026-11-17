"""Jogadas: *intenções* de movimento, ainda não validadas nem executadas.

Tanto o jogador (via interface) quanto a estratégia automática produzem
objetos ``Jogada``. O ``Jogo`` é quem as converte em ``Acao`` verificando a
legalidade (RF21) e as executa, registra e notifica (RF31). Assim o fluxo é
idêntico para qualquer origem da jogada, inclusive a jogada "nenhuma" da
estratégia desativada (RNF02).
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import TYPE_CHECKING, Optional

from .acoes import Acao, AcaoNula
from .cartas import Carta
from .pilhas import Local

if TYPE_CHECKING:  # evita importação circular; só para anotações
    from .jogo import Jogo


class Jogada(ABC):
    @abstractmethod
    def converter(self, jogo: "Jogo") -> Optional[Acao]:
        """Cria a ação correspondente, ou ``None`` se a jogada for ilegal.

        Não altera o estado do jogo.
        """


@dataclass(frozen=True)
class JogadaComprar(Jogada):
    """Comprar a carta do topo do monte."""

    def converter(self, jogo: "Jogo") -> Optional[Acao]:
        return jogo.criar_acao_comprar()


@dataclass(frozen=True)
class JogadaMover(Jogada):
    """Mover ``carta`` (e as cartas sobre ela, se estiver no tableau) para ``destino``."""

    carta: Carta
    destino: Local

    def converter(self, jogo: "Jogo") -> Optional[Acao]:
        return jogo.criar_acao_mover(self.carta, self.destino)


@dataclass(frozen=True)
class JogadaNenhuma(Jogada):
    """Nada a fazer (não há jogada possível ou a estratégia está desativada)."""

    def converter(self, jogo: "Jogo") -> Optional[Acao]:
        return AcaoNula()
