"""Widgets simples da barra superior."""
from __future__ import annotations

from typing import Callable

import pygame

from . import tema
from .desenho import FabricaDeCartas, retangulo_translucido


class Botao:
    def __init__(
        self,
        rect: pygame.Rect,
        texto: Callable[[], str] | str,
        acao: Callable[[], None],
        ativo: Callable[[], bool] = lambda: False,
        habilitado: Callable[[], bool] = lambda: True,
    ) -> None:
        self.rect = rect
        self._texto = texto
        self._acao = acao
        self._ativo = ativo
        self._habilitado = habilitado

    @property
    def texto(self) -> str:
        return self._texto() if callable(self._texto) else self._texto

    def contem(self, ponto: tuple[int, int]) -> bool:
        return self.rect.collidepoint(ponto)

    def clicar(self) -> None:
        if self._habilitado():
            self._acao()

    def desenhar(self, tela: pygame.Surface, fabrica: FabricaDeCartas, mouse: tuple[int, int]) -> None:
        habilitado = self._habilitado()
        ativo = self._ativo()
        if ativo:
            cor_fundo, cor_texto = tema.BOTAO_ATIVO, tema.BOTAO_TEXTO_ATIVO
        else:
            sobre = habilitado and self.contem(mouse)
            cor_fundo = tema.BOTAO_HOVER if sobre else tema.BOTAO
            cor_texto = tema.TEXTO if habilitado else tema.BOTAO_DESABILITADO
        if not habilitado:
            cor_texto = (*cor_texto[:3],)
        retangulo_translucido(tela, cor_fundo, self.rect, raio=8)
        if not habilitado:
            retangulo_translucido(tela, (0, 0, 0, 45), self.rect, raio=8)
        imagem = fabrica.texto(self.texto, 23, cor_texto)
        tela.blit(imagem, imagem.get_rect(center=self.rect.center))
