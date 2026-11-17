"""Desenho das cartas e dos elementos gráficos, inteiramente por código.

Nenhuma imagem externa é necessária: naipes são polígonos e círculos
desenhados em escala 4x e reduzidos (suavização), e o texto usa a fonte
padrão que já acompanha o Pygame. Funciona totalmente offline.
"""
from __future__ import annotations

import math
from typing import Optional

import pygame

from ..nucleo import Carta, Cor, Naipe, Rank
from . import tema

SUPERAMOSTRAGEM = 4

# Posições dos "pips" (símbolos centrais): (coluna 0..2, linha 0..1)
_PIPS = {
    2: [(1, 0), (1, 1)],
    3: [(1, 0), (1, .5), (1, 1)],
    4: [(0, 0), (2, 0), (0, 1), (2, 1)],
    5: [(0, 0), (2, 0), (1, .5), (0, 1), (2, 1)],
    6: [(0, 0), (2, 0), (0, .5), (2, .5), (0, 1), (2, 1)],
    7: [(0, 0), (2, 0), (1, .25), (0, .5), (2, .5), (0, 1), (2, 1)],
    8: [(0, 0), (2, 0), (1, .25), (0, .5), (2, .5), (1, .75), (0, 1), (2, 1)],
    9: [(0, 0), (2, 0), (0, 1 / 3), (2, 1 / 3), (1, .5),
        (0, 2 / 3), (2, 2 / 3), (0, 1), (2, 1)],
    10: [(0, 0), (2, 0), (1, 1 / 6), (0, 1 / 3), (2, 1 / 3),
         (0, 2 / 3), (2, 2 / 3), (1, 5 / 6), (0, 1), (2, 1)],
}
_TAMANHO_PIP = {2: 26, 3: 26, 4: 24, 5: 24, 6: 22, 7: 20, 8: 20, 9: 17, 10: 17}


def _cor_do_naipe(naipe: Naipe) -> tuple[int, int, int]:
    return tema.NAIPE_VERMELHO if naipe.cor is Cor.VERMELHA else tema.NAIPE_PRETO


def retangulo_translucido(
    destino: pygame.Surface, cor: tuple, rect: pygame.Rect, raio: int = 10, largura: int = 0
) -> None:
    """Desenha um retângulo arredondado com transparência."""
    camada = pygame.Surface(rect.size, pygame.SRCALPHA)
    pygame.draw.rect(camada, cor, camada.get_rect(), width=largura, border_radius=raio)
    destino.blit(camada, rect.topleft)


class FabricaDeCartas:
    """Gera e guarda em cache as imagens de cartas, versos e espaços vazios."""

    def __init__(self) -> None:
        self._fontes: dict[int, pygame.font.Font] = {}
        self._textos: dict[tuple, pygame.Surface] = {}
        self._formas: dict[tuple, pygame.Surface] = {}
        self._faces: dict[int, pygame.Surface] = {}
        self._verso: Optional[pygame.Surface] = None
        self._sombra: dict[tuple, pygame.Surface] = {}

    # ---- texto -------------------------------------------------------------
    def fonte(self, tamanho: int) -> pygame.font.Font:
        if tamanho not in self._fontes:
            self._fontes[tamanho] = pygame.font.Font(None, tamanho)
        return self._fontes[tamanho]

    def texto(self, conteudo: str, tamanho: int, cor: tuple) -> pygame.Surface:
        chave = (conteudo, tamanho, cor)
        if chave not in self._textos:
            self._textos[chave] = self.fonte(tamanho).render(conteudo, True, cor)
        return self._textos[chave]

    # ---- naipes ------------------------------------------------------------
    def forma_naipe(self, naipe: Naipe, lado: int) -> pygame.Surface:
        """Símbolo do naipe em um quadrado ``lado`` x ``lado`` (suavizado)."""
        chave = (naipe, lado)
        if chave in self._formas:
            return self._formas[chave]
        n = lado * SUPERAMOSTRAGEM
        cor = _cor_do_naipe(naipe)
        tela = pygame.Surface((n, n), pygame.SRCALPHA)
        tela.fill((*cor, 0))
        c = (*cor, 255)

        def p(x: float, y: float) -> tuple[int, int]:
            return int(x * n), int(y * n)

        def circulo(x: float, y: float, r: float) -> None:
            pygame.draw.circle(tela, c, p(x, y), int(r * n))

        if naipe is Naipe.COPAS:
            circulo(.30, .30, .25)
            circulo(.70, .30, .25)
            pygame.draw.polygon(tela, c, [p(.065, .41), p(.935, .41), p(.5, .96)])
        elif naipe is Naipe.OUROS:
            pygame.draw.polygon(tela, c, [p(.5, .02), p(.88, .5), p(.5, .98), p(.12, .5)])
        elif naipe is Naipe.ESPADAS:
            circulo(.30, .62, .25)
            circulo(.70, .62, .25)
            pygame.draw.polygon(tela, c, [p(.065, .59), p(.935, .59), p(.5, .04)])
            pygame.draw.polygon(tela, c, [p(.44, .68), p(.56, .68), p(.68, .97), p(.32, .97)])
        else:  # PAUS
            circulo(.50, .27, .21)
            circulo(.26, .57, .21)
            circulo(.74, .57, .21)
            circulo(.50, .52, .15)
            pygame.draw.polygon(tela, c, [p(.45, .55), p(.55, .55), p(.67, .97), p(.33, .97)])

        forma = pygame.transform.smoothscale(tela, (lado, lado))
        self._formas[chave] = forma
        return forma

    # ---- cartas ------------------------------------------------------------
    @staticmethod
    def _mascara(largura: int, altura: int, raio: int) -> pygame.Surface:
        """Máscara de cantos arredondados com bordas suaves."""
        k = SUPERAMOSTRAGEM
        grande = pygame.Surface((largura * k, altura * k), pygame.SRCALPHA)
        grande.fill((255, 255, 255, 0))
        pygame.draw.rect(grande, (255, 255, 255, 255), grande.get_rect(), border_radius=raio * k)
        return pygame.transform.smoothscale(grande, (largura, altura))

    def _base(self, fundo: tuple, borda: tuple) -> pygame.Surface:
        w, h, r = tema.CARTA_L, tema.CARTA_A, tema.RAIO_CARTA
        superficie = pygame.Surface((w, h), pygame.SRCALPHA)
        superficie.fill((*borda, 255))
        interna = pygame.Surface((w - 2, h - 2), pygame.SRCALPHA)
        interna.fill((*fundo, 255))
        interna.blit(self._mascara(w - 2, h - 2, r - 1), (0, 0), special_flags=pygame.BLEND_RGBA_MIN)
        superficie.blit(interna, (1, 1))
        superficie.blit(self._mascara(w, h, r), (0, 0), special_flags=pygame.BLEND_RGBA_MIN)
        return superficie

    def face(self, carta: Carta) -> pygame.Surface:
        """Imagem da carta virada para cima."""
        if carta.id not in self._faces:
            self._faces[carta.id] = self._criar_face(carta)
        return self._faces[carta.id]

    def _criar_face(self, carta: Carta) -> pygame.Surface:
        w, h = tema.CARTA_L, tema.CARTA_A
        superficie = self._base(tema.CARTA, tema.CARTA_BORDA)
        cor = _cor_do_naipe(carta.naipe)
        rank = int(carta.rank)

        # índice no canto superior esquerdo e cópia girada no inferior direito
        rotulo = self.texto(carta.rank.rotulo, 26, cor)
        pequeno = self.forma_naipe(carta.naipe, 16)
        cw = max(rotulo.get_width(), pequeno.get_width()) + 2
        ch = rotulo.get_height() + pequeno.get_height() - 3
        canto = pygame.Surface((cw, ch), pygame.SRCALPHA)
        canto.blit(rotulo, ((cw - rotulo.get_width()) // 2, 0))
        canto.blit(pequeno, ((cw - pequeno.get_width()) // 2, rotulo.get_height() - 3))
        superficie.blit(canto, (6, 5))
        superficie.blit(pygame.transform.rotate(canto, 180), (w - 6 - cw, h - 5 - ch))

        if carta.rank is Rank.AS:
            grande = self.forma_naipe(carta.naipe, 58)
            superficie.blit(grande, grande.get_rect(center=(w // 2, h // 2)))
        elif rank <= 10:
            self._desenhar_pips(superficie, carta.naipe, rank)
        else:
            self._desenhar_figura(superficie, carta, cor)
        return superficie

    def _desenhar_pips(self, superficie: pygame.Surface, naipe: Naipe, rank: int) -> None:
        w, h = tema.CARTA_L, tema.CARTA_A
        lado = _TAMANHO_PIP[rank]
        forma = self.forma_naipe(naipe, lado)  # sempre na vertical: copas invertidas lembram espadas
        colunas_x = (0.33 * w, 0.5 * w, 0.67 * w)
        y0, y1 = 0.21 * h, 0.79 * h
        for coluna, linha in _PIPS[rank]:
            centro = (int(colunas_x[coluna]), int(y0 + linha * (y1 - y0)))
            superficie.blit(forma, forma.get_rect(center=centro))

    def _desenhar_figura(self, superficie: pygame.Surface, carta: Carta, cor: tuple) -> None:
        """Valete, Dama e Rei: moldura com a letra grande e o naipe em cima e embaixo."""
        w, h = tema.CARTA_L, tema.CARTA_A
        moldura = pygame.Rect(0, 0, 58, 98)
        moldura.center = (w // 2, h // 2)
        retangulo_translucido(superficie, (*cor, 22), moldura, raio=6)
        retangulo_translucido(superficie, (*cor, 150), moldura, raio=6, largura=2)
        letra = self.texto(carta.rank.rotulo, 74, cor)
        superficie.blit(letra, letra.get_rect(center=moldura.center))
        naipe = self.forma_naipe(carta.naipe, 17)
        superficie.blit(naipe, naipe.get_rect(midtop=(moldura.centerx, moldura.top + 6)))
        superficie.blit(naipe, naipe.get_rect(midbottom=(moldura.centerx, moldura.bottom - 6)))

    def verso(self) -> pygame.Surface:
        """Imagem da carta virada para baixo."""
        if self._verso is None:
            w, h = tema.CARTA_L, tema.CARTA_A
            superficie = self._base(tema.VERSO_BORDA, tema.CARTA_BORDA)
            interno = pygame.Rect(6, 6, w - 12, h - 12)
            pygame.draw.rect(superficie, tema.VERSO_FUNDO, interno, border_radius=5)
            superficie.set_clip(interno.inflate(-2, -2))
            for deslocamento in range(-h, w + h, 12):
                pygame.draw.line(superficie, tema.VERSO_PADRAO,
                                 (interno.left + deslocamento, interno.top),
                                 (interno.left + deslocamento - h, interno.bottom), 1)
                pygame.draw.line(superficie, tema.VERSO_PADRAO,
                                 (interno.left + deslocamento, interno.top),
                                 (interno.left + deslocamento + h, interno.bottom), 1)
            superficie.set_clip(None)
            pygame.draw.rect(superficie, tema.VERSO_PADRAO, interno, width=2, border_radius=5)
            self._verso = superficie
        return self._verso

    def imagem(self, carta: Carta) -> pygame.Surface:
        """Face ou verso, conforme a visibilidade *atual* da carta (RNF01)."""
        return self.face(carta) if carta.visivel else self.verso()

    # ---- elementos auxiliares ----------------------------------------------
    def sombra(self, largura: int, altura: int) -> pygame.Surface:
        chave = (largura, altura)
        if chave not in self._sombra:
            s = pygame.Surface((largura, altura), pygame.SRCALPHA)
            pygame.draw.rect(s, (0, 0, 0, 70), s.get_rect(), border_radius=tema.RAIO_CARTA + 2)
            self._sombra[chave] = s
        return self._sombra[chave]

    @staticmethod
    def desenhar_espaco(destino: pygame.Surface, rect: pygame.Rect) -> None:
        """Espaço vazio de uma pilha."""
        retangulo_translucido(destino, tema.ESPACO_PREENCHIMENTO, rect, raio=tema.RAIO_CARTA + 2)
        retangulo_translucido(destino, tema.ESPACO_CONTORNO, rect, raio=tema.RAIO_CARTA + 2, largura=2)

    def desenhar_icone_reiniciar(self, destino: pygame.Surface, centro: tuple[int, int], raio: int = 15) -> None:
        """Seta circular indicando "nova partida"."""
        cor = tema.TEXTO
        caixa = pygame.Rect(0, 0, raio * 2, raio * 2)
        caixa.center = centro
        pygame.draw.arc(destino, cor, caixa, math.radians(40), math.radians(330), 3)
        ponta = (centro[0] + raio * math.cos(math.radians(40)),
                 centro[1] - raio * math.sin(math.radians(40)))
        pygame.draw.polygon(destino, cor, [
            (ponta[0] + 7, ponta[1] - 1), (ponta[0] - 5, ponta[1] - 5), (ponta[0] - 2, ponta[1] + 7)])
