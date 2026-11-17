"""Ações reversíveis e histórico de jogadas.

Cobre:
    RF22 - toda ação pode ser desfeita;
    RF23 - ação composta (executa na ordem, desfaz na ordem inversa);
    RF24 - ação nula;
    RF25 - histórico na ordem real e desfazer;
    RNF01 - visibilidade sempre coerente (ações restauram o valor anterior);
    RNF02 - todas as ações compartilham a mesma interface.

Interface comum (``Acao``): ``executar() -> bool`` e ``desfazer() -> None``.
``executar`` retorna ``True`` quando houve mudança efetiva de estado; é assim
que o jogo decide registrar no histórico e notificar (RF26) sem precisar
conhecer o tipo da ação.
"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Sequence

from .cartas import Carta
from .pilhas import Descarte, Local, Monte


class Acao(ABC):
    """Uma mudança de estado reversível."""

    @abstractmethod
    def executar(self) -> bool:
        """Aplica a ação. Retorna ``True`` se o estado mudou de fato."""

    @abstractmethod
    def desfazer(self) -> None:
        """Restaura exatamente o estado anterior à execução."""


class AcaoNula(Acao):
    """Ação sem efeito (RF24): usada quando não há jogada ou a estratégia está desativada."""

    def executar(self) -> bool:
        return False

    def desfazer(self) -> None:
        pass


class AcaoMover(Acao):
    """Move uma carta ou sequência de um local para outro.

    A legalidade já foi verificada pelo ``Jogo`` quando a ação foi criada.
    """

    def __init__(self, origem: Local, destino: Local, cartas: Sequence[Carta]) -> None:
        self._origem = origem
        self._destino = destino
        self._cartas = tuple(cartas)

    def executar(self) -> bool:
        self._origem.retirar(self._cartas)
        self._destino.depositar(self._cartas)
        return True

    def desfazer(self) -> None:
        self._destino.retirar(self._cartas)
        self._origem.depositar(self._cartas)


class AcaoRevelar(Acao):
    """Vira uma carta para cima. Ao desfazer, restaura a visibilidade que ela tinha
    antes (RNF01), em vez de assumir um valor fixo."""

    def __init__(self, carta: Carta) -> None:
        self._carta = carta
        self._visibilidade_anterior = carta.visivel

    def executar(self) -> bool:
        self._visibilidade_anterior = self._carta.visivel
        self._carta.visivel = True
        return not self._visibilidade_anterior

    def desfazer(self) -> None:
        self._carta.visivel = self._visibilidade_anterior


class AcaoComposta(Acao):
    """Conjunto de ações tratado como uma única jogada (RF23)."""

    def __init__(self, acoes: Sequence[Acao]) -> None:
        self._acoes = tuple(acoes)

    def executar(self) -> bool:
        efetiva = False
        for acao in self._acoes:
            efetiva = acao.executar() or efetiva  # nunca "curto-circuita"
        return efetiva

    def desfazer(self) -> None:
        for acao in reversed(self._acoes):
            acao.desfazer()


class AcaoComprar(AcaoComposta):
    """Compra: move a carta do topo do monte para o descarte, virada para cima
    (RF11). Desfazer devolve exatamente a mesma carta ao monte (RF12)."""

    def __init__(self, monte: Monte, descarte: Descarte) -> None:
        carta = monte.topo()
        super().__init__([AcaoMover(monte, descarte, (carta,)), AcaoRevelar(carta)])


class Historico:
    """Pilha de ações já executadas, na ordem real (RF25)."""

    def __init__(self) -> None:
        self._acoes: list[Acao] = []

    def __len__(self) -> int:
        return len(self._acoes)

    def vazio(self) -> bool:
        return not self._acoes

    def registrar(self, acao: Acao) -> None:
        self._acoes.append(acao)

    def desfazer_ultima(self) -> bool:
        """Desfaz a última ação registrada. Retorna ``False`` se não havia nenhuma."""
        if not self._acoes:
            return False
        self._acoes.pop().desfazer()
        return True

    def limpar(self) -> None:
        self._acoes.clear()
