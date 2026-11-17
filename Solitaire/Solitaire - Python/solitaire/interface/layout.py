"""Layout: onde cada local e cada carta aparece na tela.

Só calcula geometria a partir do estado do jogo (somente leitura). Não contém
nenhuma regra: para saber se um destino aceita uma jogada, a interface
pergunta ao ``Jogo`` (RNF03).
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Collection, Optional

import pygame

from ..nucleo import Carta, Coluna, Jogo, Local
from . import tema


@dataclass(frozen=True)
class PosicaoCarta:
    carta: Carta
    local: Local
    rect: pygame.Rect


class Layout:
    def __init__(self, jogo: Jogo) -> None:
        self._jogo = jogo
        self._slots: dict[int, pygame.Rect] = {}

        def x(indice: int) -> int:
            return tema.MARGEM_X + indice * (tema.CARTA_L + tema.ESPACO_H)

        def slot(indice: int, y: int) -> pygame.Rect:
            return pygame.Rect(x(indice), y, tema.CARTA_L, tema.CARTA_A)

        self._slots[id(jogo.monte)] = slot(0, tema.Y_LINHA_SUPERIOR)
        self._slots[id(jogo.descarte)] = slot(1, tema.Y_LINHA_SUPERIOR)
        for i, fundacao in enumerate(jogo.fundacoes):
            self._slots[id(fundacao)] = slot(3 + i, tema.Y_LINHA_SUPERIOR)
        for i, coluna in enumerate(jogo.colunas):
            self._slots[id(coluna)] = slot(i, tema.Y_TABLEAU)

    # ---- geometria dos locais ---------------------------------------------
    def slot(self, local: Local) -> pygame.Rect:
        return self._slots[id(local)]

    def retangulo_destaque(self, local: Local) -> pygame.Rect:
        """Região que cobre o local (para colunas, inclui todas as cartas)."""
        base = self.slot(local).copy()
        if isinstance(local, Coluna) and not local.vazia():
            ultima = self._rects_da_coluna(local)[-1]
            base.union_ip(ultima)
        return base

    def areas_de_soltar(self) -> list[tuple[Local, pygame.Rect]]:
        """Região em que soltar uma carta conta como "sobre" cada local."""
        areas = []
        meio_espaco = tema.ESPACO_H // 2
        limite_superior = tema.Y_TABLEAU - 8
        for local in self._jogo.locais:
            slot = self.slot(local)
            if isinstance(local, Coluna):
                area = pygame.Rect(slot.x - meio_espaco, limite_superior,
                                   slot.w + tema.ESPACO_H, tema.ALTURA_JANELA - limite_superior)
            else:
                area = pygame.Rect(slot.x - meio_espaco, tema.ALTURA_BARRA,
                                   slot.w + tema.ESPACO_H, limite_superior - tema.ALTURA_BARRA)
            areas.append((local, area))
        return areas

    # ---- geometria das cartas ----------------------------------------------
    def _rects_da_coluna(self, coluna: Coluna) -> list[pygame.Rect]:
        """Posição de cada carta da coluna, comprimindo-a se ficar alta demais."""
        slot = self.slot(coluna)
        cartas = coluna.cartas
        passos = [tema.PASSO_OCULTA if not c.visivel else tema.PASSO_VISIVEL for c in cartas[:-1]]
        necessario = sum(passos)
        disponivel = tema.Y_LIMITE - slot.y - tema.CARTA_A
        fator = 1.0
        if necessario > disponivel > 0:
            fator = max(tema.FATOR_MINIMO, disponivel / necessario)
        rects = []
        y = float(slot.y)
        for i in range(len(cartas)):
            rects.append(pygame.Rect(slot.x, round(y), tema.CARTA_L, tema.CARTA_A))
            if i < len(passos):
                y += passos[i] * fator
        return rects

    def posicoes(self, excluir: Collection[int] = ()) -> list[PosicaoCarta]:
        """Cartas a desenhar, de trás para frente. ``excluir`` são ids de cartas
        que estão sendo arrastadas e não devem aparecer no lugar de origem."""
        resultado: list[PosicaoCarta] = []
        for local in (self._jogo.descarte, *self._jogo.fundacoes):
            visiveis = [c for c in local.cartas if c.id not in excluir]
            if visiveis:
                resultado.append(PosicaoCarta(visiveis[-1], local, self.slot(local)))
        for coluna in self._jogo.colunas:
            for carta, rect in zip(coluna.cartas, self._rects_da_coluna(coluna)):
                if carta.id not in excluir:
                    resultado.append(PosicaoCarta(carta, coluna, rect))
        return resultado

    def retangulos_por_id(self) -> dict[int, pygame.Rect]:
        return {p.carta.id: p.rect for p in self.posicoes()}

    def carta_em(self, ponto: tuple[int, int]) -> Optional[PosicaoCarta]:
        """Carta *acessível* mais à frente sob ``ponto``, ou ``None``.

        Se a carta mais à frente for uma carta oculta, nada é retornado (ela
        cobre as de trás), em vez de "atravessar" para a carta de baixo.
        """
        for posicao in reversed(self.posicoes()):
            if posicao.rect.collidepoint(ponto):
                acessiveis = posicao.local.cartas_acessiveis()
                if any(posicao.carta is c for c in acessiveis):
                    return posicao
                return None
        return None
