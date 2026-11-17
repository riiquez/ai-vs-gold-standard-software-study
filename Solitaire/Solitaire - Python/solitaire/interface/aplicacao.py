"""Aplicação Pygame: laço principal, eventos do mouse/teclado e desenho.

A interface não contém regras de legalidade (RNF03): ao arrastar, pergunta ao
``Jogo`` se o destino sob o cursor aceitaria a jogada (RF21, RF32) e, ao
soltar, apenas *pede* a jogada; o próprio ``Jogo`` a valida e executa. As
cartas arrastadas são guardadas como identificadores e reconstituídas pelo
jogo (RNF05).
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
from typing import Optional

import pygame

from ..nucleo import (EstrategiaAutomatica, EstrategiaDesativada, Evento, Jogo,
                      JogadaComprar, JogadaMover, Local)
from . import tema
from .desenho import FabricaDeCartas, retangulo_translucido
from .layout import Layout, PosicaoCarta
from .widgets import Botao


@dataclass
class Arraste:
    """Estado de um arraste em andamento."""

    ids: tuple[int, ...]                      # cartas arrastadas (serializadas), da base ao topo
    origem: Local
    deslocamentos: tuple[tuple[int, int], ...]  # posição de cada carta relativa à primeira
    ancora: tuple[int, int]                   # cursor - canto da primeira carta
    inicio: tuple[int, int]
    moveu: bool = False


class Aplicacao:
    def __init__(self, semente: Optional[int] = None) -> None:
        pygame.init()
        pygame.display.set_caption(tema.TITULO)
        self.tela = self._criar_janela()
        self.relogio = pygame.time.Clock()
        self.fabrica = FabricaDeCartas()

        self.jogo = Jogo(semente=semente)
        self.layout = Layout(self.jogo)
        self.jogo.adicionar_observador(self._ao_mudar_jogo)

        self._estrategia_ligada = EstrategiaAutomatica()
        self._estrategia_desligada = EstrategiaDesativada()
        self.estrategia_ativa = True
        self.auto_jogar = False
        self._acumulado_auto = 0

        self.arraste: Optional[Arraste] = None
        self.mouse = (0, 0)
        self._ultimo_clique: Optional[tuple[int, int]] = None   # (id da carta, instante)
        self._mensagem = ""
        self._mensagem_ate = 0
        self._confirmar_ate = 0
        self.rodando = True

        self._fundo = self._criar_fundo()
        self.botoes = self._criar_botoes()

    # =====================================================================
    # Construção
    # =====================================================================
    @staticmethod
    def _criar_janela() -> pygame.Surface:
        tamanho = (tema.LARGURA_JANELA, tema.ALTURA_JANELA)
        try:  # SCALED: janela redimensionável com coordenadas lógicas fixas
            return pygame.display.set_mode(tamanho, pygame.SCALED | pygame.RESIZABLE)
        except pygame.error:
            return pygame.display.set_mode(tamanho)

    @staticmethod
    def _criar_fundo() -> pygame.Surface:
        """Feltro verde com escurecimento suave nas bordas."""
        lw, lh = 64, 50
        pequeno = pygame.Surface((lw, lh))
        for y in range(lh):
            for x in range(lw):
                dx = (x - lw / 2) / (lw / 2)
                dy = (y - lh / 2) / (lh / 2)
                t = min(1.0, (dx * dx + dy * dy) ** 0.5 / 1.25)
                cor = tuple(round(a + (b - a) * t * t)
                            for a, b in zip(tema.FELTRO_CENTRO, tema.FELTRO_BORDA))
                pequeno.set_at((x, y), cor)
        return pygame.transform.smoothscale(pequeno, (tema.LARGURA_JANELA, tema.ALTURA_JANELA))

    def _criar_botoes(self) -> list[Botao]:
        altura, largura, folga = 34, 150, 12
        y = (tema.ALTURA_BARRA - altura) // 2
        especificacoes = [
            (lambda: "Desfazer", self.desfazer, lambda: False, lambda: self.jogo.pode_desfazer()),
            (lambda: "Jogada automática", self.jogada_automatica, lambda: False, lambda: True),
            (lambda: "Auto-jogar", self.alternar_auto_jogar, lambda: self.auto_jogar, lambda: True),
            (lambda: "Estratégia: ligada" if self.estrategia_ativa else "Estratégia: desligada",
             self.alternar_estrategia, lambda: False, lambda: True),
        ]
        larguras = [110, 180, 120, 190]
        x = tema.LARGURA_JANELA - 20 - sum(larguras) - folga * (len(larguras) - 1)
        botoes = []
        for (texto, acao, ativo, habilitado), l in zip(especificacoes, larguras):
            botoes.append(Botao(pygame.Rect(x, y, l, altura), texto, acao, ativo, habilitado))
            x += l + folga
        return botoes

    # =====================================================================
    # Ações da interface (todas delegam ao Jogo)
    # =====================================================================
    def desfazer(self) -> None:
        if self.arraste is None:
            self.jogo.desfazer()

    def jogada_automatica(self) -> None:
        if self.arraste is not None:
            return
        if not self.jogo.jogada_automatica():
            self._avisar("Estratégia desligada." if not self.estrategia_ativa
                         else "Não há jogadas automáticas disponíveis.")

    def alternar_auto_jogar(self) -> None:
        self.auto_jogar = not self.auto_jogar
        self._acumulado_auto = 0

    def alternar_estrategia(self) -> None:
        self.estrategia_ativa = not self.estrategia_ativa
        self.jogo.definir_estrategia(
            self._estrategia_ligada if self.estrategia_ativa else self._estrategia_desligada)

    def _clicar_monte(self) -> None:
        """Compra; com o monte vazio, a mesma ação inicia uma nova partida (RF01)."""
        if self.jogo.pode_comprar():
            self.jogo.jogar(JogadaComprar())
            return
        agora = pygame.time.get_ticks()
        if self.jogo.vencido() or agora <= self._confirmar_ate:
            self._confirmar_ate = 0
            self.jogo.nova_partida()
        else:  # ainda há jogo em andamento: pede confirmação para evitar cliques acidentais
            self._confirmar_ate = agora + tema.JANELA_CONFIRMACAO
            self._avisar("Clique de novo no monte para começar uma nova partida.")

    def _enviar_para_fundacao(self, carta) -> None:
        for fundacao in self.jogo.fundacoes:
            if self.jogo.jogar(JogadaMover(carta, fundacao)):
                return
        self._avisar("Essa carta ainda não pode ir para a fundação.")

    def _avisar(self, texto: str) -> None:
        self._mensagem = texto
        self._mensagem_ate = pygame.time.get_ticks() + tema.DURACAO_MENSAGEM

    def _ao_mudar_jogo(self, evento: Evento, jogo: Jogo) -> None:
        """Observador do jogo (RF26): reage a qualquer mudança de estado."""
        if evento is Evento.NOVA_PARTIDA:
            self.auto_jogar = False
            self.arraste = None
            self._ultimo_clique = None
            self._mensagem = ""
        if jogo.vencido():
            self.auto_jogar = False

    # =====================================================================
    # Eventos
    # =====================================================================
    def tratar_evento(self, ev: pygame.event.Event) -> None:
        if ev.type == pygame.QUIT:
            self.rodando = False
        elif ev.type == pygame.KEYDOWN:
            self._tecla(ev)
        elif ev.type == pygame.MOUSEMOTION:
            self._mouse_movido(ev.pos)
        elif ev.type == pygame.MOUSEBUTTONDOWN and ev.button == 1:
            self._mouse_pressionado(ev.pos)
        elif ev.type == pygame.MOUSEBUTTONUP and ev.button == 1:
            self._mouse_solto(ev.pos)

    def _tecla(self, ev: pygame.event.Event) -> None:
        if ev.key == pygame.K_ESCAPE:
            self.arraste = None
        elif ev.key == pygame.K_z:
            self.desfazer()
        elif ev.key == pygame.K_a:
            self.jogada_automatica()

    def _mouse_movido(self, pos: tuple[int, int]) -> None:
        self.mouse = pos
        a = self.arraste
        if a is not None and not a.moveu:
            if abs(pos[0] - a.inicio[0]) + abs(pos[1] - a.inicio[1]) > tema.LIMIAR_ARRASTE:
                a.moveu = True

    def _mouse_pressionado(self, pos: tuple[int, int]) -> None:
        self.mouse = pos
        if self.arraste is not None:
            return
        for botao in self.botoes:
            if botao.contem(pos):
                botao.clicar()
                return
        if self.layout.slot(self.jogo.monte).collidepoint(pos):
            self._clicar_monte()
            return
        posicao = self.layout.carta_em(pos)
        if posicao is None:
            return
        agora = pygame.time.get_ticks()
        anterior = self._ultimo_clique
        if anterior and anterior[0] == posicao.carta.id and agora - anterior[1] <= tema.INTERVALO_DUPLO_CLIQUE:
            self._ultimo_clique = None
            self._enviar_para_fundacao(posicao.carta)
            return
        self._iniciar_arraste(posicao, pos)

    def _iniciar_arraste(self, posicao: PosicaoCarta, pos: tuple[int, int]) -> None:
        sequencia = self.jogo.sequencia_da_carta(posicao.carta)   # RF18: leva as de cima
        if not sequencia:
            return
        rects = self.layout.retangulos_por_id()
        primeira = rects[sequencia[0].id]
        self.arraste = Arraste(
            ids=self.jogo.serializar(sequencia),
            origem=posicao.local,
            deslocamentos=tuple((rects[c.id].x - primeira.x, rects[c.id].y - primeira.y)
                                for c in sequencia),
            ancora=(pos[0] - primeira.x, pos[1] - primeira.y),
            inicio=pos,
        )

    def _mouse_solto(self, pos: tuple[int, int]) -> None:
        self.mouse = pos
        a = self.arraste
        if a is None:
            return
        self.arraste = None
        if not a.moveu:  # foi só um clique: guarda para detectar duplo clique
            self._ultimo_clique = (a.ids[0], pygame.time.get_ticks())
            return
        self._ultimo_clique = None
        primeira = self.jogo.reconstituir(a.ids)[0]
        alvo = self._alvo_do_arraste(a, pos)
        if alvo is None or alvo is a.origem:
            return
        if not self.jogo.jogar(JogadaMover(primeira, alvo)):
            self._avisar("Movimento inválido.")

    # =====================================================================
    # Alvo do arraste
    # =====================================================================
    def _retangulo_arrastado(self, a: Arraste, pos: tuple[int, int]) -> pygame.Rect:
        return pygame.Rect(pos[0] - a.ancora[0], pos[1] - a.ancora[1], tema.CARTA_L, tema.CARTA_A)

    def _alvo_do_arraste(self, a: Arraste, pos: tuple[int, int]) -> Optional[Local]:
        """Local sob o cursor; se nenhum, o que mais se sobrepõe à carta arrastada."""
        areas = self.layout.areas_de_soltar()
        for local, area in areas:
            if area.collidepoint(pos):
                return local
        carta = self._retangulo_arrastado(a, pos)
        melhor, maior = None, 0
        for local, area in areas:
            intersecao = area.clip(carta)
            if intersecao.w * intersecao.h > maior:
                melhor, maior = local, intersecao.w * intersecao.h
        return melhor

    # =====================================================================
    # Atualização e laço principal
    # =====================================================================
    def atualizar(self, dt_ms: int) -> None:
        if self.auto_jogar and self.arraste is None:
            self._acumulado_auto += dt_ms
            if self._acumulado_auto >= tema.INTERVALO_AUTOJOGAR:
                self._acumulado_auto = 0
                if not self.jogo.jogada_automatica():
                    self.auto_jogar = False
                    self._avisar("Estratégia desligada." if not self.estrategia_ativa
                                 else "Fim das jogadas automáticas.")

    def executar(self) -> None:
        while self.rodando:
            for ev in pygame.event.get():
                self.tratar_evento(ev)
            self.atualizar(self.relogio.get_time())
            self.desenhar()
            pygame.display.flip()
            self.relogio.tick(tema.FPS)
        pygame.quit()

    # =====================================================================
    # Desenho
    # =====================================================================
    def desenhar(self) -> None:
        tela = self.tela
        tela.blit(self._fundo, (0, 0))
        self._desenhar_espacos()
        excluir = set(self.arraste.ids) if self.arraste else set()
        for posicao in self.layout.posicoes(excluir):
            tela.blit(self.fabrica.imagem(posicao.carta), posicao.rect)
        self._desenhar_monte()
        if self.arraste is not None:
            self._desenhar_arraste()
        if self.jogo.vencido():
            self._desenhar_vitoria()
        self._desenhar_barra()
        self._desenhar_status()

    def _desenhar_espacos(self) -> None:
        tela, fab = self.tela, self.fabrica
        for local in self.jogo.locais:
            fab.desenhar_espaco(tela, self.layout.slot(local))
        # marcas discretas: Ás nas fundações, Rei nas colunas
        for fundacao in self.jogo.fundacoes:
            self._rotulo_no_espaco(fundacao, "A")
        for coluna in self.jogo.colunas:
            if coluna.vazia():
                self._rotulo_no_espaco(coluna, "K")

    def _rotulo_no_espaco(self, local: Local, texto: str) -> None:
        imagem = self.fabrica.texto(texto, 56, (255, 255, 255))
        imagem = imagem.copy()
        imagem.set_alpha(60)
        self.tela.blit(imagem, imagem.get_rect(center=self.layout.slot(local).center))

    def _desenhar_monte(self) -> None:
        slot = self.layout.slot(self.jogo.monte)
        restantes = len(self.jogo.monte)
        if restantes == 0:
            centro = slot.center
            self.fabrica.desenhar_icone_reiniciar(self.tela, (centro[0], centro[1] - 16))
            for i, linha in enumerate(("Nova", "partida")):
                imagem = self.fabrica.texto(linha, 24, tema.TEXTO)
                self.tela.blit(imagem, imagem.get_rect(center=(centro[0], centro[1] + 18 + i * 20)))
            return
        camadas = min(3, 1 + restantes // 8)
        for i in range(camadas - 1, -1, -1):
            self.tela.blit(self.fabrica.verso(), (slot.x + i * 2, slot.y + i * 2))
        contagem = self.fabrica.texto(str(restantes), 22, tema.TEXTO_SUAVE)
        self.tela.blit(contagem, contagem.get_rect(midtop=(slot.centerx, slot.bottom + 8)))

    def _desenhar_arraste(self) -> None:
        a = self.arraste
        cartas = self.jogo.reconstituir(a.ids)
        base = self._retangulo_arrastado(a, self.mouse)
        if a.moveu:
            self._desenhar_destino(a, cartas[0])
        for carta, (dx, dy) in zip(cartas, a.deslocamentos):
            rect = base.move(dx, dy)
            self.tela.blit(self.fabrica.sombra(rect.w, rect.h), rect.move(5, 7))
            self.tela.blit(self.fabrica.imagem(carta), rect)

    def _desenhar_destino(self, a: Arraste, carta) -> None:
        """Contorno verde/vermelho no destino sob o cursor, conforme as regras do jogo."""
        alvo = self._alvo_do_arraste(a, self.mouse)
        if alvo is None or alvo is a.origem:
            return
        valido = self.jogo.pode_mover(carta, alvo)           # RF21 / RF32
        cor = tema.DESTINO_OK if valido else tema.DESTINO_ERRO
        rect = self.layout.retangulo_destaque(alvo).inflate(10, 10)
        retangulo_translucido(self.tela, (*cor, 40), rect, raio=tema.RAIO_CARTA + 4)
        retangulo_translucido(self.tela, (*cor, 235), rect, raio=tema.RAIO_CARTA + 4, largura=3)

    def _desenhar_barra(self) -> None:
        tela = self.tela
        pygame.draw.rect(tela, tema.BARRA, (0, 0, tema.LARGURA_JANELA, tema.ALTURA_BARRA))
        pygame.draw.line(tela, tema.BARRA_LINHA, (0, tema.ALTURA_BARRA), (tema.LARGURA_JANELA, tema.ALTURA_BARRA), 2)
        titulo = self.fabrica.texto(tema.TITULO, 36, tema.TEXTO)
        tela.blit(titulo, titulo.get_rect(midleft=(24, tema.ALTURA_BARRA // 2)))
        for botao in self.botoes:
            botao.desenhar(tela, self.fabrica, self.mouse)

    def _desenhar_status(self) -> None:
        tela = self.tela
        y = tema.ALTURA_JANELA - tema.ALTURA_STATUS
        pygame.draw.rect(tela, tema.BARRA, (0, y, tema.LARGURA_JANELA, tema.ALTURA_STATUS))
        if self._mensagem and pygame.time.get_ticks() < self._mensagem_ate:
            texto, cor = self._mensagem, tema.TEXTO
        else:
            texto = "Arraste as cartas  |  Duplo clique envia à fundação  |  Z desfaz  |  A faz uma jogada automática"
            cor = tema.TEXTO_SUAVE
        imagem = self.fabrica.texto(texto, 22, cor)
        tela.blit(imagem, imagem.get_rect(center=(tema.LARGURA_JANELA // 2, y + tema.ALTURA_STATUS // 2)))

    def _desenhar_vitoria(self) -> None:
        painel = pygame.Rect(0, 0, 560, 150)
        painel.center = (tema.LARGURA_JANELA // 2, tema.Y_TABLEAU + 270)
        retangulo_translucido(self.tela, (6, 40, 22, 225), painel, raio=18)
        retangulo_translucido(self.tela, (255, 214, 102, 255), painel, raio=18, largura=3)
        titulo = self.fabrica.texto("Você venceu!", 72, (255, 226, 140))
        self.tela.blit(titulo, titulo.get_rect(center=(painel.centerx, painel.y + 55)))
        sub = self.fabrica.texto("Clique no monte para jogar de novo.", 28, tema.TEXTO)
        self.tela.blit(sub, sub.get_rect(center=(painel.centerx, painel.y + 112)))


def principal(argumentos: Optional[list[str]] = None) -> None:
    analisador = argparse.ArgumentParser(description="Paciência Klondike (Pygame)")
    analisador.add_argument("--semente", type=int, default=None,
                            help="semente do embaralhamento (partida reproduzível)")
    args = analisador.parse_args(argumentos)
    Aplicacao(semente=args.semente).executar()
