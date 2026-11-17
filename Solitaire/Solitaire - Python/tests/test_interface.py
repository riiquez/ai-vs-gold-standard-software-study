"""Testes da interface (Pygame em modo "dummy", sem abrir janela)."""
import os
import subprocess
import sys
import unittest

os.environ.setdefault("SDL_VIDEODRIVER", "dummy")
os.environ.setdefault("SDL_AUDIODRIVER", "dummy")

try:
    import pygame
except ImportError:  # pragma: no cover
    pygame = None

from tests.test_nucleo import C, E, O, P, carta, colocar, limpar
from solitaire.nucleo import Rank


class TestIndependenciaDoNucleo(unittest.TestCase):
    def test_nucleo_nao_importa_pygame(self):
        codigo = (
            "import sys; import solitaire.nucleo as n; "
            "j = n.Jogo(semente=1); j.jogada_automatica(); "
            "sys.exit(1 if 'pygame' in sys.modules else 0)"
        )
        resultado = subprocess.run([sys.executable, "-c", codigo], cwd=os.getcwd())
        self.assertEqual(resultado.returncode, 0, "o núcleo não pode depender de Pygame")


@unittest.skipIf(pygame is None, "pygame-ce não instalado")
class TestInterface(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        from solitaire.interface.aplicacao import Aplicacao
        cls.Aplicacao = Aplicacao

    def setUp(self):
        self.app = self.Aplicacao(semente=3)
        self.jogo = self.app.jogo

    def tearDown(self):
        pygame.quit()

    # ---- utilitários ------------------------------------------------------
    def evento(self, tipo, pos):
        self.app.tratar_evento(pygame.event.Event(tipo, pos=pos, button=1))

    def arrastar(self, origem, destino, passos=4):
        self.evento(pygame.MOUSEBUTTONDOWN, origem)
        for i in range(1, passos + 1):
            ponto = (origem[0] + (destino[0] - origem[0]) * i // passos,
                     origem[1] + (destino[1] - origem[1]) * i // passos)
            self.app.tratar_evento(pygame.event.Event(pygame.MOUSEMOTION, pos=ponto, rel=(0, 0), buttons=(1, 0, 0)))
            self.app.desenhar()  # exercita o desenho durante o arraste
        self.evento(pygame.MOUSEBUTTONUP, destino)

    def centro(self, c):
        """Ponto na faixa visível do topo da carta (o centro pode estar coberto por outras)."""
        rect = self.app.layout.retangulos_por_id()[c.id]
        return (rect.centerx, rect.y + 8)

    def centro_slot(self, local):
        return self.app.layout.slot(local).center

    def montar_basico(self):
        j = self.jogo
        limpar(j)
        colocar(j, j.colunas[0], (P, Rank.NOVE, True))
        colocar(j, j.colunas[1], (E, Rank.REI, False), (C, Rank.OITO, True),
                (P, Rank.SETE, True), (O, Rank.SEIS, True))
        colocar(j, j.monte, (O, Rank.DOIS, False), (E, Rank.TRES, False))

    # ---- desenho ----------------------------------------------------------
    def test_desenha_todas_as_cartas(self):
        from solitaire.interface import tema
        for c in self.jogo.baralho:
            self.assertEqual(self.app.fabrica.face(c).get_size(), (tema.CARTA_L, tema.CARTA_A))
        self.assertEqual(self.app.fabrica.verso().get_size(), (tema.CARTA_L, tema.CARTA_A))
        self.app.desenhar()

    def test_imagem_reflete_visibilidade_atual(self):
        c = self.jogo.monte.topo()
        self.assertIs(self.app.fabrica.imagem(c), self.app.fabrica.verso())
        c.visivel = True
        self.assertIs(self.app.fabrica.imagem(c), self.app.fabrica.face(c))

    # ---- arraste ----------------------------------------------------------
    def test_arrastar_sequencia_valida_move_todas_e_revela(self):
        self.montar_basico()
        j = self.jogo
        oito = carta(j, C, Rank.OITO)
        self.arrastar(self.centro(oito), self.centro_slot(j.colunas[0]))
        self.assertEqual([c.rank for c in j.colunas[0].cartas],
                         [Rank.NOVE, Rank.OITO, Rank.SETE, Rank.SEIS])
        self.assertTrue(carta(j, E, Rank.REI).visivel)
        self.assertIsNone(self.app.arraste)

    def test_arrastar_para_destino_invalido_nao_move(self):
        self.montar_basico()
        j = self.jogo
        antes = j.instantaneo()
        oito = carta(j, C, Rank.OITO)
        self.arrastar(self.centro(oito), self.centro_slot(j.colunas[4]))    # coluna vazia, não-Rei
        self.assertEqual(j.instantaneo(), antes)
        self.arrastar(self.centro(oito), self.centro_slot(j.descarte))      # descarte nunca aceita
        self.assertEqual(j.instantaneo(), antes)
        self.arrastar(self.centro(oito), self.centro_slot(j.fundacoes[0]))  # fundação vazia, não-Ás
        self.assertEqual(j.instantaneo(), antes)
        self.assertFalse(j.pode_desfazer())

    def test_nao_arrasta_carta_oculta(self):
        self.montar_basico()
        j = self.jogo
        antes = j.instantaneo()
        rei_oculto = carta(j, E, Rank.REI)
        self.arrastar(self.app.layout.retangulos_por_id()[rei_oculto.id].move(0, 3).midtop, self.centro_slot(j.colunas[4]))
        self.assertEqual(j.instantaneo(), antes)

    def test_arrastar_do_meio_da_sequencia_leva_as_de_cima(self):
        j = self.jogo
        limpar(j)
        colocar(j, j.colunas[0], (O, Rank.OITO, True))
        colocar(j, j.colunas[1], (C, Rank.OITO, True), (P, Rank.SETE, True), (O, Rank.SEIS, True))
        self.arrastar(self.centro(carta(j, P, Rank.SETE)), self.centro_slot(j.colunas[0]))
        self.assertEqual([c.rank for c in j.colunas[0].cartas], [Rank.OITO, Rank.SETE, Rank.SEIS])

    def test_arraste_do_descarte_para_fundacao(self):
        j = self.jogo
        limpar(j)
        colocar(j, j.descarte, (C, Rank.AS, True))
        self.arrastar(self.centro_slot(j.descarte), self.centro_slot(j.fundacoes[2]))
        self.assertEqual(len(j.fundacoes[2]), 1)
        self.assertTrue(j.descarte.vazia())
        self.assertEqual(j.pontuacao(), 1)

    def test_durante_arraste_cartas_ficam_ocultas_na_origem(self):
        self.montar_basico()
        j = self.jogo
        oito = carta(j, C, Rank.OITO)
        self.evento(pygame.MOUSEBUTTONDOWN, self.centro(oito))
        ids = {c.id for c in j.sequencia_da_carta(oito)}
        self.assertEqual(set(self.app.arraste.ids), ids)
        desenhadas = {p.carta.id for p in self.app.layout.posicoes(excluir=ids)}
        self.assertFalse(ids & desenhadas)
        self.evento(pygame.MOUSEBUTTONUP, self.centro(oito))

    # ---- cliques ----------------------------------------------------------
    def test_clique_no_monte_compra(self):
        topo = self.jogo.monte.topo()
        self.evento(pygame.MOUSEBUTTONDOWN, self.centro_slot(self.jogo.monte))
        self.assertIs(self.jogo.descarte.topo(), topo)

    def test_monte_vazio_pede_confirmacao_e_inicia_nova_partida(self):
        j = self.jogo
        while j.pode_comprar():
            self.evento(pygame.MOUSEBUTTONDOWN, self.centro_slot(j.monte))
        estado = j.instantaneo()
        self.evento(pygame.MOUSEBUTTONDOWN, self.centro_slot(j.monte))   # 1º clique: só avisa
        self.assertEqual(j.instantaneo(), estado)
        self.evento(pygame.MOUSEBUTTONDOWN, self.centro_slot(j.monte))   # 2º clique: nova partida
        self.assertEqual(len(j.monte), 24)
        self.assertNotEqual(j.instantaneo(), estado)

    def test_vitoria_inicia_nova_partida_sem_confirmar(self):
        j = self.jogo
        limpar(j)
        from solitaire.nucleo import Naipe
        for f, naipe in zip(j.fundacoes, Naipe):
            colocar(j, f, *[(naipe, r, True) for r in Rank])
        self.app.desenhar()   # desenha a faixa de vitória
        self.evento(pygame.MOUSEBUTTONDOWN, self.centro_slot(j.monte))
        self.assertEqual(len(j.monte), 24)

    def test_duplo_clique_envia_para_fundacao(self):
        j = self.jogo
        limpar(j)
        colocar(j, j.colunas[0], (E, Rank.REI, False), (C, Rank.AS, True))
        ponto = self.centro(carta(j, C, Rank.AS))
        for _ in range(2):
            self.evento(pygame.MOUSEBUTTONDOWN, ponto)
            self.evento(pygame.MOUSEBUTTONUP, ponto)
        self.assertEqual(j.pontuacao(), 1)
        self.assertTrue(carta(j, E, Rank.REI).visivel)

    # ---- botões e teclado --------------------------------------------------
    def test_botao_desfazer_e_tecla_z(self):
        j = self.jogo
        antes = j.instantaneo()
        self.evento(pygame.MOUSEBUTTONDOWN, self.centro_slot(j.monte))
        self.assertTrue(j.pode_desfazer())
        self.app.tratar_evento(pygame.event.Event(pygame.KEYDOWN, key=pygame.K_z))
        self.assertEqual(j.instantaneo(), antes)

    def test_jogada_automatica_pelo_botao_e_estrategia_desligada(self):
        j = self.jogo
        botoes = {b.texto.split(":")[0]: b for b in self.app.botoes}
        self.evento(pygame.MOUSEBUTTONDOWN, botoes["Jogada automática"].rect.center)
        self.assertTrue(j.pode_desfazer())          # jogou (comprou uma carta)
        botoes["Estratégia"].clicar()
        self.assertFalse(self.app.estrategia_ativa)
        antes = j.instantaneo()
        self.evento(pygame.MOUSEBUTTONDOWN, botoes["Jogada automática"].rect.center)
        self.assertEqual(j.instantaneo(), antes)    # nenhuma jogada automática
        botoes["Estratégia"].clicar()
        self.assertTrue(self.app.estrategia_ativa)

    def test_auto_jogar_continuo_termina_sozinho(self):
        self.app.alternar_auto_jogar()
        for _ in range(400):
            self.app.atualizar(1000)
            if not self.app.auto_jogar:
                break
        self.assertFalse(self.app.auto_jogar)
        self.assertFalse(self.jogo.jogada_automatica())


if __name__ == "__main__":
    unittest.main()
