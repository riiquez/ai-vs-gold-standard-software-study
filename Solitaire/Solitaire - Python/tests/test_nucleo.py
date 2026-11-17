"""Testes do núcleo (sem Pygame). Execute: python -m unittest discover -s tests -t ."""
import random
import unittest

from solitaire.nucleo import (
    Baralho, Carta, Cor, ErroDeJogo, EstrategiaAutomatica, EstrategiaDesativada,
    Evento, Jogada, JogadaComprar, JogadaMover, JogadaNenhuma, Jogo, Naipe, Rank,
)

C, O, P, E = Naipe.COPAS, Naipe.OUROS, Naipe.PAUS, Naipe.ESPADAS


def limpar(jogo):
    """Esvazia o jogo para montar cenários específicos."""
    for local in jogo.locais:
        local.limpar()
    jogo.baralho.ocultar_todas()
    jogo._historico.limpar()


def colocar(jogo, local, *specs):
    """specs: (naipe, rank, visivel), da base ao topo."""
    for naipe, rank, visivel in specs:
        carta = jogo.baralho.carta(naipe, rank)
        carta.visivel = visivel
        local.colocar(carta)


def carta(jogo, naipe, rank):
    return jogo.baralho.carta(naipe, rank)


class TestCartasEBaralho(unittest.TestCase):
    def test_52_cartas_unicas(self):
        b = Baralho()
        self.assertEqual(len(b), 52)
        self.assertEqual(len({(c.naipe, c.rank) for c in b}), 52)
        self.assertEqual(len({id(c) for c in b}), 52)
        self.assertEqual(sorted(c.id for c in b), list(range(52)))

    def test_ordem_dos_ranks(self):
        self.assertLess(Rank.AS, Rank.DOIS)
        self.assertLess(Rank.DEZ, Rank.VALETE)
        self.assertLess(Rank.VALETE, Rank.DAMA)
        self.assertLess(Rank.DAMA, Rank.REI)
        self.assertIs(Rank.AS.superior(), Rank.DOIS)
        self.assertIs(Rank.REI.inferior(), Rank.DAMA)
        self.assertIsNone(Rank.REI.superior())
        self.assertIsNone(Rank.AS.inferior())

    def test_superior_inferior_entre_cartas(self):
        b = Baralho()
        seis, sete = b.carta(P, Rank.SEIS), b.carta(C, Rank.SETE)
        self.assertTrue(sete.e_imediatamente_superior_a(seis))
        self.assertTrue(seis.e_imediatamente_inferior_a(sete))
        self.assertFalse(seis.e_imediatamente_superior_a(sete))

    def test_cores(self):
        self.assertIs(C.cor, Cor.VERMELHA)
        self.assertIs(O.cor, Cor.VERMELHA)
        self.assertIs(P.cor, Cor.PRETA)
        self.assertIs(E.cor, Cor.PRETA)
        self.assertTrue(C.tem_mesma_cor(O))
        self.assertFalse(C.tem_mesma_cor(P))

    def test_embaralhar_varia_e_preserva_cartas(self):
        a, b = Baralho(), Baralho()
        a.embaralhar(random.Random(1))
        b.embaralhar(random.Random(2))
        self.assertNotEqual([c.id for c in a], [c.id for c in b])
        self.assertEqual(sorted(c.id for c in a), list(range(52)))

    def test_embaralhar_reprodutivel_com_semente(self):
        a, b = Baralho(), Baralho()
        a.embaralhar(random.Random(7))
        b.embaralhar(random.Random(7))
        self.assertEqual([c.id for c in a], [c.id for c in b])

    def test_serializacao_preserva_identidade_e_ordem(self):
        jogo = Jogo(semente=3)
        seq = jogo.colunas[6].cartas
        volta = jogo.reconstituir(jogo.serializar(seq))
        self.assertEqual(len(volta), len(seq))
        for original, recuperada in zip(seq, volta):
            self.assertIs(original, recuperada)
        with self.assertRaises(ErroDeJogo):
            jogo.reconstituir([99])


class TestPartida(unittest.TestCase):
    def test_distribuicao_inicial(self):
        jogo = Jogo(semente=5)
        self.assertEqual([len(c) for c in jogo.colunas], [1, 2, 3, 4, 5, 6, 7])
        self.assertEqual(len(jogo.monte), 24)
        self.assertTrue(jogo.descarte.vazia())
        self.assertTrue(all(f.vazia() for f in jogo.fundacoes))
        self.assertEqual(jogo.pontuacao(), 0)
        todas = [c for l in jogo.locais for c in l.cartas]
        self.assertEqual(len({id(c) for c in todas}), 52)

    def test_so_a_ultima_de_cada_coluna_visivel(self):
        jogo = Jogo(semente=5)
        for coluna in jogo.colunas:
            visiveis = [c.visivel for c in coluna.cartas]
            self.assertEqual(visiveis, [False] * (len(coluna) - 1) + [True])
        self.assertFalse(any(c.visivel for c in jogo.monte.cartas))

    def test_nova_partida_embaralha_e_limpa_historico(self):
        jogo = Jogo(semente=5)
        antes = jogo.instantaneo()
        jogo.jogar(JogadaComprar())
        jogo.nova_partida()
        self.assertNotEqual(jogo.instantaneo(), antes)
        self.assertFalse(jogo.pode_desfazer())
        self.assertEqual(len(jogo.monte), 24)
        self.assertTrue(jogo.descarte.vazia())

    def test_nova_partida_so_com_monte_vazio(self):
        jogo = Jogo(semente=5)
        self.assertFalse(jogo.pode_nova_partida())
        while jogo.pode_comprar():
            jogo.jogar(JogadaComprar())
        self.assertTrue(jogo.pode_nova_partida())

    def test_vitoria_e_pontuacao(self):
        jogo = Jogo(semente=1)
        limpar(jogo)
        for f, naipe in zip(jogo.fundacoes, Naipe):
            colocar(jogo, f, *[(naipe, r, True) for r in Rank])
        self.assertEqual(jogo.pontuacao(), 52)
        self.assertTrue(jogo.vencido())
        jogo.fundacoes[3]._cartas.pop()
        self.assertFalse(jogo.vencido())


class TestCompra(unittest.TestCase):
    def test_comprar_e_desfazer(self):
        jogo = Jogo(semente=2)
        topo = jogo.monte.topo()
        estado = jogo.instantaneo()
        self.assertTrue(jogo.jogar(JogadaComprar()))
        self.assertIs(jogo.descarte.topo(), topo)
        self.assertTrue(topo.visivel)
        self.assertEqual(len(jogo.monte), 23)
        self.assertTrue(jogo.desfazer())
        self.assertIs(jogo.monte.topo(), topo)
        self.assertFalse(topo.visivel)
        self.assertTrue(jogo.descarte.vazia())
        self.assertEqual(jogo.instantaneo(), estado)

    def test_comprar_com_monte_vazio_falha(self):
        jogo = Jogo(semente=2)
        while jogo.pode_comprar():
            jogo.jogar(JogadaComprar())
        self.assertFalse(jogo.jogar(JogadaComprar()))

    def test_descarte_nao_e_destino_valido(self):
        jogo = Jogo(semente=2)
        limpar(jogo)
        colocar(jogo, jogo.colunas[0], (C, Rank.REI, True))
        self.assertFalse(jogo.pode_mover(carta(jogo, C, Rank.REI), jogo.descarte))
        self.assertFalse(jogo.pode_mover(carta(jogo, C, Rank.REI), jogo.monte))


class TestFundacoes(unittest.TestCase):
    def setUp(self):
        self.jogo = Jogo(semente=2)
        limpar(self.jogo)

    def test_vazia_so_aceita_as(self):
        j = self.jogo
        colocar(j, j.colunas[0], (C, Rank.DOIS, True))
        colocar(j, j.colunas[1], (C, Rank.AS, True))
        self.assertFalse(j.pode_mover(carta(j, C, Rank.DOIS), j.fundacoes[0]))
        self.assertTrue(j.pode_mover(carta(j, C, Rank.AS), j.fundacoes[0]))

    def test_mesmo_naipe_e_rank_seguinte(self):
        j = self.jogo
        colocar(j, j.fundacoes[0], (C, Rank.AS, True))
        colocar(j, j.colunas[0], (C, Rank.DOIS, True))
        colocar(j, j.colunas[1], (O, Rank.DOIS, True))
        colocar(j, j.colunas[2], (C, Rank.TRES, True))
        self.assertTrue(j.pode_mover(carta(j, C, Rank.DOIS), j.fundacoes[0]))
        self.assertFalse(j.pode_mover(carta(j, O, Rank.DOIS), j.fundacoes[0]))
        self.assertFalse(j.pode_mover(carta(j, C, Rank.TRES), j.fundacoes[0]))

    def test_so_uma_carta_por_vez(self):
        j = self.jogo
        colocar(j, j.fundacoes[0], (C, Rank.AS, True))
        colocar(j, j.colunas[0], (C, Rank.DOIS, True), (P, Rank.AS, True))
        # 2♥ tem uma carta sobre ela: levaria a sequência inteira
        self.assertFalse(j.pode_mover(carta(j, C, Rank.DOIS), j.fundacoes[0]))


class TestTableau(unittest.TestCase):
    def setUp(self):
        self.jogo = Jogo(semente=2)
        limpar(self.jogo)

    def test_coluna_vazia_so_aceita_rei(self):
        j = self.jogo
        colocar(j, j.colunas[0], (C, Rank.REI, True))
        colocar(j, j.colunas[1], (P, Rank.DAMA, True))
        self.assertFalse(j.pode_mover(carta(j, P, Rank.DAMA), j.colunas[3]))
        self.assertTrue(j.pode_mover(carta(j, C, Rank.REI), j.colunas[3]))

    def test_rank_inferior_e_cor_diferente(self):
        j = self.jogo
        colocar(j, j.colunas[0], (C, Rank.SETE, True))
        colocar(j, j.colunas[1], (P, Rank.SEIS, True))   # preto sobre vermelho: ok
        colocar(j, j.colunas[2], (O, Rank.SEIS, True))   # mesma cor: não
        colocar(j, j.colunas[3], (P, Rank.CINCO, True))  # rank errado: não
        self.assertTrue(j.pode_mover(carta(j, P, Rank.SEIS), j.colunas[0]))
        self.assertFalse(j.pode_mover(carta(j, O, Rank.SEIS), j.colunas[0]))
        self.assertFalse(j.pode_mover(carta(j, P, Rank.CINCO), j.colunas[0]))

    def test_move_sequencia_inteira_na_mesma_ordem(self):
        j = self.jogo
        colocar(j, j.colunas[0], (P, Rank.NOVE, True))
        colocar(j, j.colunas[1], (C, Rank.OITO, True), (P, Rank.SETE, True), (O, Rank.SEIS, True))
        oito = carta(j, C, Rank.OITO)
        self.assertEqual(len(j.sequencia_da_carta(oito)), 3)
        self.assertTrue(j.jogar(JogadaMover(oito, j.colunas[0])))
        self.assertEqual([(c.naipe, int(c.rank)) for c in j.colunas[0].cartas],
                         [(P, 9), (C, 8), (P, 7), (O, 6)])
        self.assertTrue(j.colunas[1].vazia())

    def test_mover_carta_no_meio_leva_as_de_cima(self):
        j = self.jogo
        colocar(j, j.colunas[0], (O, Rank.OITO, True))
        colocar(j, j.colunas[1], (C, Rank.OITO, True), (P, Rank.SETE, True), (O, Rank.SEIS, True))
        sete = carta(j, P, Rank.SETE)
        self.assertTrue(j.jogar(JogadaMover(sete, j.colunas[0])))
        self.assertEqual([c.rank for c in j.colunas[0].cartas], [Rank.OITO, Rank.SETE, Rank.SEIS])
        self.assertEqual(len(j.colunas[1]), 1)

    def test_revelacao_automatica_e_desfazer(self):
        j = self.jogo
        colocar(j, j.colunas[0], (C, Rank.SETE, True))
        colocar(j, j.colunas[1], (E, Rank.REI, False), (P, Rank.SEIS, True))
        oculta = carta(j, E, Rank.REI)
        antes = j.instantaneo()
        self.assertTrue(j.jogar(JogadaMover(carta(j, P, Rank.SEIS), j.colunas[0])))
        self.assertTrue(oculta.visivel)                  # revelada na mesma jogada
        self.assertEqual(len(j._historico), 1)           # uma única jogada
        self.assertTrue(j.desfazer())
        self.assertFalse(oculta.visivel)                 # oculta de novo
        self.assertEqual(j.instantaneo(), antes)

    def test_nao_revela_quando_abaixo_ja_visivel(self):
        j = self.jogo
        colocar(j, j.colunas[0], (C, Rank.SETE, True))
        colocar(j, j.colunas[1], (P, Rank.OITO, True), (O, Rank.SETE, True))  # sem oculta
        colocar(j, j.colunas[2], (C, Rank.REI, True), (P, Rank.SEIS, True))
        j.jogar(JogadaMover(carta(j, P, Rank.SEIS), j.colunas[0]))
        self.assertTrue(carta(j, C, Rank.REI).visivel)

    def test_cartas_ocultas_nao_sao_acessiveis(self):
        j = self.jogo
        colocar(j, j.colunas[0], (E, Rank.REI, False), (P, Rank.SEIS, True))
        self.assertIsNone(j.local_da_carta(carta(j, E, Rank.REI)))
        self.assertIs(j.local_da_carta(carta(j, P, Rank.SEIS)), j.colunas[0])
        self.assertFalse(j.pode_mover(carta(j, E, Rank.REI), j.colunas[1]))

    def test_consultas_do_tableau(self):
        j = self.jogo
        col = j.colunas[0]
        colocar(j, col, (E, Rank.DAMA, False), (C, Rank.REI, True), (P, Rank.DAMA, True))
        rei, dama = carta(j, C, Rank.REI), carta(j, P, Rank.DAMA)
        self.assertEqual(len(col.cartas), 3)
        self.assertEqual(col.sequencia_a_partir_de(rei), (rei, dama))
        self.assertTrue(col.carta_visivel(rei))
        self.assertTrue(col.e_primeira_visivel(rei))
        self.assertFalse(col.e_primeira_visivel(dama))
        self.assertFalse(col.rei_ja_na_base(rei))     # há uma carta oculta sob ele
        colocar(j, j.colunas[1], (O, Rank.REI, True))
        self.assertTrue(j.colunas[1].rei_ja_na_base(carta(j, O, Rank.REI)))


class TestReadOnly(unittest.TestCase):
    def test_consultas_nao_alteram_estado(self):
        jogo = Jogo(semente=11)
        antes = jogo.instantaneo()
        for local in jogo.locais:
            local.vazia(); local.topo(); local.cartas; local.cartas_acessiveis()
            for c in local.cartas:
                jogo.local_da_carta(c)
                jogo.sequencia_da_carta(c)
                for destino in jogo.locais:
                    jogo.pode_mover(c, destino)
                    jogo.criar_acao_mover(c, destino)
        jogo.criar_acao_comprar()
        jogo.pontuacao(); jogo.vencido()
        self.assertEqual(jogo.instantaneo(), antes)
        self.assertFalse(jogo.pode_desfazer())

    def test_estrategia_so_decide_e_nao_altera(self):
        for semente in range(30):
            jogo = Jogo(semente=semente)
            for _ in range(40):
                antes = jogo.instantaneo()
                tamanho = len(jogo._historico)
                jogada = jogo.estrategia.sugerir(jogo.consulta)
                self.assertIsInstance(jogada, Jogada)
                self.assertEqual(jogo.instantaneo(), antes)
                self.assertEqual(len(jogo._historico), tamanho)
                if not jogo.jogar(jogada):
                    break

    def test_consulta_nao_expoe_comandos(self):
        consulta = Jogo(semente=1).consulta
        for comando in ("jogar", "desfazer", "nova_partida", "jogada_automatica"):
            self.assertFalse(hasattr(consulta, comando))


class TestHistoricoENotificacao(unittest.TestCase):
    def test_desfazer_sem_historico(self):
        self.assertFalse(Jogo(semente=1).desfazer())

    def test_historico_em_ordem_real(self):
        jogo = Jogo(semente=4)
        estados = [jogo.instantaneo()]
        for _ in range(8):
            jogo.jogada_automatica()
            estados.append(jogo.instantaneo())
        for esperado in reversed(estados[:-1]):
            jogo.desfazer()
            self.assertEqual(jogo.instantaneo(), esperado)

    def test_notifica_jogada_desfazer_e_nova_partida(self):
        jogo = Jogo(semente=4)
        eventos = []
        jogo.adicionar_observador(lambda ev, j: eventos.append(ev))
        jogo.jogar(JogadaComprar())
        jogo.desfazer()
        jogo.nova_partida()
        self.assertEqual(eventos, [Evento.JOGADA, Evento.DESFEITA, Evento.NOVA_PARTIDA])

    def test_jogada_ilegal_ou_nula_nao_notifica_nem_registra(self):
        jogo = Jogo(semente=4)
        eventos = []
        jogo.adicionar_observador(lambda ev, j: eventos.append(ev))
        topo = jogo.colunas[0].topo()
        self.assertFalse(jogo.jogar(JogadaMover(topo, jogo.descarte)))
        self.assertFalse(jogo.jogar(JogadaNenhuma()))
        self.assertEqual(eventos, [])
        self.assertFalse(jogo.pode_desfazer())


class TestEstrategia(unittest.TestCase):
    def setUp(self):
        self.jogo = Jogo(semente=1)
        limpar(self.jogo)
        self.est = EstrategiaAutomatica()

    def sugerir(self):
        return self.est.sugerir(self.jogo.consulta)

    def test_1_comprar_com_descarte_vazio_antes_de_tudo(self):
        j = self.jogo
        colocar(j, j.monte, (O, Rank.REI, False))
        colocar(j, j.colunas[0], (C, Rank.AS, True))      # poderia ir à fundação
        self.assertIsInstance(self.sugerir(), JogadaComprar)

    def test_2_descarte_para_fundacao_antes_de_coluna(self):
        j = self.jogo
        colocar(j, j.descarte, (P, Rank.AS, True))
        colocar(j, j.colunas[0], (C, Rank.AS, True))
        jogada = self.sugerir()
        self.assertIsInstance(jogada, JogadaMover)
        self.assertIs(jogada.carta, carta(j, P, Rank.AS))
        self.assertIn(jogada.destino, j.fundacoes)

    def test_3_descarte_para_coluna_antes_de_coluna_para_fundacao(self):
        j = self.jogo
        colocar(j, j.monte, (O, Rank.REI, False))
        colocar(j, j.descarte, (C, Rank.CINCO, True))
        colocar(j, j.colunas[0], (P, Rank.SEIS, True))
        colocar(j, j.colunas[1], (O, Rank.AS, True))
        jogada = self.sugerir()
        self.assertIs(jogada.carta, carta(j, C, Rank.CINCO))
        self.assertIs(jogada.destino, j.colunas[0])

    def test_4_coluna_para_fundacao_antes_de_mover_no_tableau(self):
        j = self.jogo
        colocar(j, j.monte, (O, Rank.REI, False))
        colocar(j, j.descarte, (E, Rank.QUATRO, True))    # injogável
        colocar(j, j.colunas[0], (C, Rank.AS, True))
        colocar(j, j.colunas[1], (E, Rank.NOVE, False), (O, Rank.OITO, True))
        colocar(j, j.colunas[2], (P, Rank.NOVE, True))    # 8♦ poderia ir aqui (prioridade 5)
        jogada = self.sugerir()
        self.assertIs(jogada.carta, carta(j, C, Rank.AS))
        self.assertIn(jogada.destino, j.fundacoes)

    def test_5_mover_no_tableau_antes_de_comprar(self):
        j = self.jogo
        colocar(j, j.monte, (O, Rank.DOIS, False))
        colocar(j, j.descarte, (E, Rank.DOIS, True))
        colocar(j, j.colunas[0], (E, Rank.NOVE, False), (O, Rank.OITO, True))
        colocar(j, j.colunas[1], (P, Rank.NOVE, True))
        jogada = self.sugerir()
        self.assertIsInstance(jogada, JogadaMover)
        self.assertIs(jogada.carta, carta(j, O, Rank.OITO))     # revela o 9♠
        self.assertIs(jogada.destino, j.colunas[1])

    def test_5_so_move_com_ganho_concreto(self):
        j = self.jogo
        # 7♦ poderia ir sobre 8♠ da coluna 1, mas não revelaria nem esvaziaria nada
        colocar(j, j.colunas[0], (P, Rank.OITO, True), (O, Rank.SETE, True))
        colocar(j, j.colunas[1], (E, Rank.OITO, True))
        self.assertTrue(j.pode_mover(carta(j, O, Rank.SETE), j.colunas[1]))
        self.assertIsInstance(self.sugerir(), JogadaNenhuma)

    def test_5_esvaziar_coluna_conta_como_ganho(self):
        j = self.jogo
        colocar(j, j.colunas[0], (P, Rank.NOVE, True), (O, Rank.OITO, True))
        colocar(j, j.colunas[1], (C, Rank.DEZ, False), (O, Rank.DEZ, True))
        jogada = self.sugerir()
        self.assertIsInstance(jogada, JogadaMover)
        self.assertIs(jogada.carta, carta(j, P, Rank.NOVE))
        self.assertIs(jogada.destino, j.colunas[1])

    def test_rei_na_base_nao_vai_para_outra_coluna_vazia(self):
        j = self.jogo
        colocar(j, j.colunas[0], (C, Rank.REI, True), (P, Rank.DAMA, True))
        self.assertIsInstance(self.sugerir(), JogadaNenhuma)

    def test_rei_com_oculta_abaixo_vai_para_coluna_vazia(self):
        j = self.jogo
        colocar(j, j.colunas[0], (O, Rank.DOIS, False), (C, Rank.REI, True))
        jogada = self.sugerir()
        self.assertIsInstance(jogada, JogadaMover)
        self.assertIs(jogada.carta, carta(j, C, Rank.REI))
        self.assertIsNot(jogada.destino, j.colunas[0])

    def test_6_comprar_quando_nada_mais_e_possivel(self):
        j = self.jogo
        colocar(j, j.monte, (O, Rank.DOIS, False))
        colocar(j, j.descarte, (E, Rank.QUATRO, True))
        colocar(j, j.colunas[0], (C, Rank.REI, True))
        self.assertIsInstance(self.sugerir(), JogadaComprar)

    def test_7_nenhuma_acao(self):
        j = self.jogo
        colocar(j, j.descarte, (E, Rank.QUATRO, True))
        colocar(j, j.colunas[0], (C, Rank.REI, True))
        self.assertIsInstance(self.sugerir(), JogadaNenhuma)
        self.assertFalse(j.jogada_automatica())
        self.assertFalse(j.pode_desfazer())

    def test_estrategia_desativada(self):
        jogo = Jogo(semente=1, estrategia=EstrategiaDesativada())
        antes = jogo.instantaneo()
        self.assertFalse(jogo.jogada_automatica())
        self.assertEqual(jogo.instantaneo(), antes)
        self.assertFalse(jogo.pode_desfazer())
        jogo.definir_estrategia(EstrategiaAutomatica())
        self.assertTrue(jogo.jogada_automatica())


class TestJogoAutomaticoLongo(unittest.TestCase):
    def jogar_ate_o_fim(self, jogo, limite=2000):
        estados = [jogo.instantaneo()]
        for _ in range(limite):
            if not jogo.jogada_automatica():
                return estados
            estados.append(jogo.instantaneo())
        self.fail("jogo automático entrou em loop")

    def test_sempre_termina_sem_ciclos(self):
        for semente in range(150):
            estados = self.jogar_ate_o_fim(Jogo(semente=semente))
            self.assertEqual(len(estados), len(set(estados)), f"estado repetido (semente {semente})")

    def test_desfazer_tudo_volta_ao_inicio_e_permite_continuar(self):
        for semente in range(60):
            jogo = Jogo(semente=semente)
            inicial = jogo.instantaneo()
            primeira = self.jogar_ate_o_fim(jogo)
            while jogo.desfazer():
                pass
            self.assertEqual(jogo.instantaneo(), inicial)
            self.assertFalse(jogo.pode_desfazer())
            # visibilidade restaurada: só a última de cada coluna aparece
            for coluna in jogo.colunas:
                self.assertEqual([c.visivel for c in coluna.cartas],
                                 [False] * (len(coluna) - 1) + [True])
            segunda = self.jogar_ate_o_fim(jogo)       # continua com jogo automático
            self.assertEqual(primeira, segunda)

    def test_pontuacao_coerente_com_fundacoes(self):
        jogo = Jogo(semente=8)
        self.jogar_ate_o_fim(jogo)
        self.assertEqual(jogo.pontuacao(), sum(len(f) for f in jogo.fundacoes))
        self.assertEqual(sum(len(l) for l in jogo.locais), 52)


if __name__ == "__main__":
    unittest.main()
