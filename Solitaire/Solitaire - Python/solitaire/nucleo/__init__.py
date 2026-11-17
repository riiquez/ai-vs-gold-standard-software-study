"""Núcleo do jogo: regras, cartas, pilhas, ações e estratégia.

Este pacote NÃO depende de Pygame nem de qualquer interface gráfica.
"""
from .acoes import (Acao, AcaoComposta, AcaoComprar, AcaoMover, AcaoNula,
                    AcaoRevelar, Historico)
from .cartas import Baralho, Carta, Cor, Naipe, Rank
from .erros import ErroDeJogo
from .estrategia import Estrategia, EstrategiaAutomatica, EstrategiaDesativada
from .jogadas import Jogada, JogadaComprar, JogadaMover, JogadaNenhuma
from .jogo import ConsultaJogo, Evento, Jogo
from .pilhas import Coluna, Descarte, Fundacao, Local, Monte, Pilha

__all__ = [
    "Acao", "AcaoComposta", "AcaoComprar", "AcaoMover", "AcaoNula", "AcaoRevelar",
    "Historico", "Baralho", "Carta", "Cor", "Naipe", "Rank", "ErroDeJogo",
    "Estrategia", "EstrategiaAutomatica", "EstrategiaDesativada", "Jogada",
    "JogadaComprar", "JogadaMover", "JogadaNenhuma", "ConsultaJogo", "Evento",
    "Jogo", "Coluna", "Descarte", "Fundacao", "Local", "Monte", "Pilha",
]
