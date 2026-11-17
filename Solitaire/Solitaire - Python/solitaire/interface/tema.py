"""Constantes visuais: dimensões, posições e paleta."""

# ---- janela ----------------------------------------------------------------
LARGURA_JANELA = 1000
ALTURA_JANELA = 780
FPS = 60
TITULO = "Paciência Klondike"

# ---- cartas e grade ----------------------------------------------------------
CARTA_L = 104
CARTA_A = 146
RAIO_CARTA = 9
ESPACO_H = 24
MARGEM_X = (LARGURA_JANELA - (7 * CARTA_L + 6 * ESPACO_H)) // 2

ALTURA_BARRA = 56
ALTURA_STATUS = 30
Y_LINHA_SUPERIOR = ALTURA_BARRA + 22
Y_TABLEAU = Y_LINHA_SUPERIOR + CARTA_A + 34
Y_LIMITE = ALTURA_JANELA - ALTURA_STATUS - 12   # borda inferior das colunas

PASSO_OCULTA = 12     # deslocamento entre cartas viradas para baixo
PASSO_VISIVEL = 30    # deslocamento entre cartas viradas para cima
FATOR_MINIMO = 0.45   # compressão máxima de uma coluna muito longa

# ---- interação ---------------------------------------------------------------
LIMIAR_ARRASTE = 5            # pixels até considerar que é um arraste
INTERVALO_DUPLO_CLIQUE = 400  # ms
INTERVALO_AUTOJOGAR = 380     # ms entre jogadas no modo "auto-jogar"
DURACAO_MENSAGEM = 3200       # ms
JANELA_CONFIRMACAO = 3000     # ms para confirmar nova partida com cartas em jogo

# ---- paleta ------------------------------------------------------------------
FELTRO_CENTRO = (38, 135, 80)
FELTRO_BORDA = (14, 78, 43)
BARRA = (9, 50, 29)
BARRA_LINHA = (6, 36, 20)
TEXTO = (240, 247, 241)
TEXTO_SUAVE = (160, 202, 174)

CARTA = (252, 250, 245)
CARTA_BORDA = (128, 128, 128)
NAIPE_VERMELHO = (196, 30, 46)
NAIPE_PRETO = (26, 28, 36)

VERSO_BORDA = (247, 247, 247)
VERSO_FUNDO = (30, 64, 140)
VERSO_PADRAO = (66, 106, 190)

ESPACO_PREENCHIMENTO = (0, 0, 0, 38)
ESPACO_CONTORNO = (255, 255, 255, 80)

DESTINO_OK = (120, 240, 150)
DESTINO_ERRO = (245, 100, 100)

BOTAO = (255, 255, 255, 30)
BOTAO_HOVER = (255, 255, 255, 64)
BOTAO_ATIVO = (255, 214, 102, 215)
BOTAO_TEXTO_ATIVO = (40, 30, 5)
BOTAO_DESABILITADO = (112, 152, 127)
