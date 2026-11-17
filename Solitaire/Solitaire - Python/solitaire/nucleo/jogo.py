"""Mecanismo central do jogo.

Cobre:
    RF01 - nova partida;           RF02 - vitória;        RF03 - pontuação;
    RF10 - localizar uma carta;    RF21 - legalidade;     RF25 - histórico;
    RF26 - notificação;            RF27 - consultas somente leitura;
    RF28 - jogada automática;      RF31 - decidir x executar;
    RNF05 - serialização fiel de cartas e sequências.

O ``Jogo`` não conhece a interface gráfica: ela se registra como observador e
consulta as regras (``pode_mover``) em vez de reimplementá-las (RNF03).
"""
from __future__ import annotations

import enum
import random
from typing import Callable, Iterable, Optional, Sequence

from .acoes import Acao, AcaoComposta, AcaoComprar, AcaoMover, AcaoRevelar, Historico
from .cartas import Baralho, Carta
from .erros import ErroDeJogo
from .estrategia import Estrategia, EstrategiaAutomatica
from .jogadas import Jogada
from .pilhas import Coluna, Descarte, Fundacao, Local, Monte


class Evento(enum.Enum):
    """Tipos de mudança notificados aos observadores (RF26)."""

    NOVA_PARTIDA = "nova_partida"
    JOGADA = "jogada"
    DESFEITA = "desfeita"


Observador = Callable[[Evento, "Jogo"], None]


class ConsultaJogo:
    """Visão somente leitura do jogo, entregue à estratégia (RF31, RNF04).

    Expõe apenas consultas; a estratégia não tem acesso a ``jogar``,
    ``desfazer`` nem a qualquer outro comando do ``Jogo``.
    """

    __slots__ = ("_jogo",)

    def __init__(self, jogo: "Jogo") -> None:
        self._jogo = jogo

    @property
    def monte(self) -> Monte:
        return self._jogo.monte

    @property
    def descarte(self) -> Descarte:
        return self._jogo.descarte

    @property
    def fundacoes(self) -> tuple[Fundacao, ...]:
        return self._jogo.fundacoes

    @property
    def colunas(self) -> tuple[Coluna, ...]:
        return self._jogo.colunas

    def pode_comprar(self) -> bool:
        return self._jogo.pode_comprar()

    def pode_mover(self, carta: Carta, destino: Local) -> bool:
        return self._jogo.pode_mover(carta, destino)

    def pontuacao(self) -> int:
        return self._jogo.pontuacao()


class Jogo:
    """Estado e regras de uma partida de Paciência Klondike."""

    NUM_COLUNAS = 7
    NUM_FUNDACOES = 4
    TOTAL_CARTAS = Baralho.TOTAL

    def __init__(
        self,
        estrategia: Optional[Estrategia] = None,
        semente: Optional[int] = None,
    ) -> None:
        self._rng = random.Random(semente)
        self._baralho = Baralho()
        self.monte = Monte("Monte")
        self.descarte = Descarte("Descarte")
        self.fundacoes: tuple[Fundacao, ...] = tuple(
            Fundacao(f"Fundação {i + 1}") for i in range(self.NUM_FUNDACOES)
        )
        self.colunas: tuple[Coluna, ...] = tuple(
            Coluna(f"Coluna {i + 1}") for i in range(self.NUM_COLUNAS)
        )
        self._locais: tuple[Local, ...] = (
            self.monte, self.descarte, *self.fundacoes, *self.colunas,
        )
        self._historico = Historico()
        self._observadores: list[Observador] = []
        self._estrategia: Estrategia = estrategia or EstrategiaAutomatica()
        self._distribuir()

    # =======================================================================
    # Consultas (nunca alteram o estado - RF27)
    # =======================================================================
    @property
    def locais(self) -> tuple[Local, ...]:
        """Todos os locais, tratados de forma equivalente (RF10)."""
        return self._locais

    @property
    def baralho(self) -> Baralho:
        return self._baralho

    @property
    def consulta(self) -> ConsultaJogo:
        return ConsultaJogo(self)

    @property
    def estrategia(self) -> Estrategia:
        return self._estrategia

    def pontuacao(self) -> int:
        """Número de cartas nas fundações (RF03)."""
        return sum(len(f) for f in self.fundacoes)

    def vencido(self) -> bool:
        """Vitória: as 52 cartas estão nas fundações (RF02)."""
        return self.pontuacao() == self.TOTAL_CARTAS

    def local_da_carta(self, carta: Carta) -> Optional[Local]:
        """Local onde a carta está *acessível* para movimentação (RF10), ou ``None``."""
        for local in self._locais:
            for acessivel in local.cartas_acessiveis():
                if acessivel is carta:
                    return local
        return None

    def sequencia_da_carta(self, carta: Carta) -> tuple[Carta, ...]:
        """Cartas que se movem junto com ``carta`` (RF18); vazio se inacessível."""
        local = self.local_da_carta(carta)
        return local.sequencia_a_partir_de(carta) if local is not None else ()

    def pode_comprar(self) -> bool:
        return not self.monte.vazia()

    def pode_nova_partida(self) -> bool:
        """A opção de nova partida fica disponível com o monte vazio (RF01)."""
        return self.monte.vazia()

    def pode_desfazer(self) -> bool:
        return not self._historico.vazio()

    def pode_mover(self, carta: Carta, destino: Local) -> bool:
        """Legalidade de mover ``carta`` (e as cartas sobre ela) para ``destino`` (RF21).

        Única fonte de verdade das regras: a interface e a estratégia apenas
        chamam este método.
        """
        if not any(destino is local for local in self._locais):
            return False
        origem = self.local_da_carta(carta)
        if origem is None or origem is destino:
            return False
        sequencia = origem.sequencia_a_partir_de(carta)
        return bool(sequencia) and destino.aceita(sequencia)

    def criar_acao_comprar(self) -> Optional[Acao]:
        """Ação de compra, ou ``None`` se o monte estiver vazio. Não altera o estado."""
        if not self.pode_comprar():
            return None
        return AcaoComprar(self.monte, self.descarte)

    def criar_acao_mover(self, carta: Carta, destino: Local) -> Optional[Acao]:
        """Ação de mover, ou ``None`` se ilegal. Não altera o estado.

        Quando a saída da carta expõe uma carta oculta, a revelação entra na
        mesma jogada como ação composta (RF19, RF23).
        """
        if not self.pode_mover(carta, destino):
            return None
        origem = self.local_da_carta(carta)
        cartas = origem.sequencia_a_partir_de(carta)
        acoes: list[Acao] = [AcaoMover(origem, destino, cartas)]
        oculta = origem.carta_a_revelar_apos_retirar(cartas)
        if oculta is not None:
            acoes.append(AcaoRevelar(oculta))
        return acoes[0] if len(acoes) == 1 else AcaoComposta(acoes)

    def instantaneo(self) -> tuple:
        """Fotografia imutável do estado (ordem e visibilidade de todas as cartas)."""
        return tuple(
            tuple((c.id, c.visivel) for c in local.cartas) for local in self._locais
        )

    # ---- serialização fiel (RNF05) ----------------------------------------
    @staticmethod
    def serializar(cartas: Iterable[Carta]) -> tuple[int, ...]:
        """Converte cartas em identificadores, preservando a ordem."""
        return tuple(c.id for c in cartas)

    def reconstituir(self, ids: Iterable[int]) -> tuple[Carta, ...]:
        """Recupera *as mesmas* cartas (mesmos objetos) e na mesma ordem."""
        return tuple(self._baralho.por_id(i) for i in ids)

    # =======================================================================
    # Comandos
    # =======================================================================
    def adicionar_observador(self, observador: Observador) -> None:
        self._observadores.append(observador)

    def definir_estrategia(self, estrategia: Estrategia) -> None:
        self._estrategia = estrategia

    def nova_partida(self) -> None:
        """Embaralha e distribui as cartas na posição inicial (RF01)."""
        self._distribuir()
        self._notificar(Evento.NOVA_PARTIDA)

    def jogar(self, jogada: Jogada) -> bool:
        """Valida (RF21), executa, registra (RF25) e notifica (RF26) uma jogada.

        Funciona igual para jogadas do jogador e da estratégia. Retorna
        ``True`` se o estado mudou; ilegais e nulas retornam ``False`` sem
        tocar no histórico.
        """
        acao = jogada.converter(self)
        if acao is None:
            return False
        if not acao.executar():
            return False
        self._historico.registrar(acao)
        self._notificar(Evento.JOGADA)
        return True

    def jogada_automatica(self) -> bool:
        """Pede uma jogada à estratégia atual e a executa (RF28, RF31)."""
        return self.jogar(self._estrategia.sugerir(self.consulta))

    def desfazer(self) -> bool:
        """Desfaz a última jogada, se houver (RF25)."""
        if not self._historico.desfazer_ultima():
            return False
        self._notificar(Evento.DESFEITA)
        return True

    # =======================================================================
    # Internos
    # =======================================================================
    def _distribuir(self) -> None:
        for local in self._locais:
            local.limpar()
        self._baralho.ocultar_todas()
        self._baralho.embaralhar(self._rng)
        cartas = iter(self._baralho.cartas)
        for i, coluna in enumerate(self.colunas):
            for _ in range(i + 1):
                coluna.colocar(next(cartas))
            coluna.topo().visivel = True  # só a última de cada coluna (RF16)
        for carta in cartas:
            self.monte.colocar(carta)
        self._historico.limpar()

    def _notificar(self, evento: Evento) -> None:
        for observador in list(self._observadores):
            observador(evento, self)
