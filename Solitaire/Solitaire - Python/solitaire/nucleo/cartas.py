"""Cartas, naipes, ranks e baralho.

Cobre:
    RF04 - composição do baralho e unicidade de cada carta;
    RF05 - ordem total dos ranks;
    RF06 - cor dos naipes;
    RF07 - embaralhamento.
"""
from __future__ import annotations

import enum
import random
from typing import Iterator, Optional

from .erros import ErroDeJogo


class Cor(enum.Enum):
    """Cor de um naipe."""

    VERMELHA = "vermelha"
    PRETA = "preta"


class Naipe(enum.Enum):
    """Os quatro naipes, cada um com seu nome e sua cor (RF06)."""

    COPAS = ("Copas", Cor.VERMELHA)
    OUROS = ("Ouros", Cor.VERMELHA)
    PAUS = ("Paus", Cor.PRETA)
    ESPADAS = ("Espadas", Cor.PRETA)

    @property
    def nome(self) -> str:
        return self.value[0]

    @property
    def cor(self) -> Cor:
        return self.value[1]

    @property
    def indice(self) -> int:
        """Posição do naipe na ordem de declaração (0 a 3)."""
        return _ORDEM_NAIPES.index(self)

    def tem_mesma_cor(self, outro: "Naipe") -> bool:
        """Indica se os dois naipes têm a mesma cor."""
        return self.cor is outro.cor


_ORDEM_NAIPES = tuple(Naipe)


class Rank(enum.IntEnum):
    """Ranks em ordem total: Ás (1) < 2 < ... < Valete < Dama < Rei (13)."""

    AS = 1
    DOIS = 2
    TRES = 3
    QUATRO = 4
    CINCO = 5
    SEIS = 6
    SETE = 7
    OITO = 8
    NOVE = 9
    DEZ = 10
    VALETE = 11
    DAMA = 12
    REI = 13

    @property
    def rotulo(self) -> str:
        """Texto impresso na carta (A, 2..10, J, Q, K)."""
        return _ROTULOS.get(self, str(int(self)))

    def superior(self) -> Optional["Rank"]:
        """Rank imediatamente acima, ou ``None`` para o Rei."""
        return Rank(self + 1) if self is not Rank.REI else None

    def inferior(self) -> Optional["Rank"]:
        """Rank imediatamente abaixo, ou ``None`` para o Ás."""
        return Rank(self - 1) if self is not Rank.AS else None


_ROTULOS = {Rank.AS: "A", Rank.VALETE: "J", Rank.DAMA: "Q", Rank.REI: "K"}

_SIMBOLOS = {
    Naipe.COPAS: "\u2665",
    Naipe.OUROS: "\u2666",
    Naipe.PAUS: "\u2663",
    Naipe.ESPADAS: "\u2660",
}


class Carta:
    """Uma carta do baralho.

    Cada uma das 52 cartas existe uma única vez (RF04); por isso a comparação
    é sempre por identidade (``is``) e não há ``__eq__`` customizado. Qualquer
    pilha ou ação que referencie a carta aponta para este mesmo objeto.

    Atributos:
        naipe, rank: identificam a carta (imutáveis por convenção).
        visivel: ``True`` quando a face está virada para cima. Só é alterada
            pelas ações do jogo (RNF01).
    """

    __slots__ = ("naipe", "rank", "visivel")

    def __init__(self, naipe: Naipe, rank: Rank, visivel: bool = False) -> None:
        self.naipe = naipe
        self.rank = rank
        self.visivel = visivel

    @property
    def id(self) -> int:
        """Identificador estável de 0 a 51, usado na serialização (RNF05)."""
        return self.naipe.indice * len(Rank) + (int(self.rank) - 1)

    @property
    def cor(self) -> Cor:
        return self.naipe.cor

    def tem_cor_diferente_de(self, outra: "Carta") -> bool:
        return not self.naipe.tem_mesma_cor(outra.naipe)

    def e_imediatamente_superior_a(self, outra: "Carta") -> bool:
        """``True`` se o rank desta carta é exatamente o de ``outra`` + 1."""
        return int(self.rank) == int(outra.rank) + 1

    def e_imediatamente_inferior_a(self, outra: "Carta") -> bool:
        """``True`` se o rank desta carta é exatamente o de ``outra`` - 1."""
        return int(self.rank) + 1 == int(outra.rank)

    def __repr__(self) -> str:
        estado = "" if self.visivel else "*"
        return f"{self.rank.rotulo}{_SIMBOLOS[self.naipe]}{estado}"


class Baralho:
    """As 52 cartas, em uma ordem que pode ser embaralhada (RF07)."""

    TOTAL = 52

    def __init__(self) -> None:
        self._ordem: list[Carta] = [Carta(n, r) for n in Naipe for r in Rank]
        self._por_id = {c.id: c for c in self._ordem}

    @property
    def cartas(self) -> tuple[Carta, ...]:
        """Cartas na ordem atual do baralho (cópia imutável)."""
        return tuple(self._ordem)

    def __len__(self) -> int:
        return len(self._ordem)

    def __iter__(self) -> Iterator[Carta]:
        return iter(self.cartas)

    def embaralhar(self, rng: Optional[random.Random] = None) -> None:
        """Embaralha em ordem aleatória. Passe um ``Random`` para reprodutibilidade."""
        (rng or random).shuffle(self._ordem)

    def ocultar_todas(self) -> None:
        """Vira todas as cartas para baixo (usado ao iniciar uma partida)."""
        for carta in self._ordem:
            carta.visivel = False

    def carta(self, naipe: Naipe, rank: Rank) -> Carta:
        """Retorna *a* carta única com esse naipe e rank."""
        return self._por_id[naipe.indice * len(Rank) + (int(rank) - 1)]

    def por_id(self, identificador: int) -> Carta:
        """Retorna *a* carta de um identificador (ver ``Carta.id``)."""
        try:
            return self._por_id[identificador]
        except KeyError:
            raise ErroDeJogo(f"Identificador de carta inválido: {identificador!r}") from None
