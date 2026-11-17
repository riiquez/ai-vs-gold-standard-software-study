"""Regras de empilhamento do Klondike, como funções puras.

Cobre RF15 (fundações) e RF17 (colunas do tableau). São funções sem estado:
recebem cartas e respondem se o empilhamento é permitido. Os locais
(``pilhas.py``) as usam em ``aceita``; a interface nunca as reimplementa
(RF21, RNF03).
"""
from __future__ import annotations

from typing import Optional, Sequence

from .cartas import Carta, Rank


def pode_ir_para_fundacao(carta: Carta, topo: Optional[Carta]) -> bool:
    """Fundação vazia só aceita Ás; senão, mesmo naipe e rank imediatamente superior."""
    if topo is None:
        return carta.rank is Rank.AS
    return carta.naipe is topo.naipe and carta.e_imediatamente_superior_a(topo)


def pode_ir_para_coluna(carta: Carta, topo: Optional[Carta]) -> bool:
    """Coluna vazia só aceita Rei; senão, rank imediatamente inferior e cor diferente."""
    if topo is None:
        return carta.rank is Rank.REI
    return carta.e_imediatamente_inferior_a(topo) and carta.tem_cor_diferente_de(topo)


def e_sequencia_valida(cartas: Sequence[Carta]) -> bool:
    """Verifica se as cartas formam uma sequência do tableau (ranks decrescentes,
    cores alternadas), da base para o topo."""
    return all(
        proxima.e_imediatamente_inferior_a(anterior)
        and proxima.tem_cor_diferente_de(anterior)
        for anterior, proxima in zip(cartas, cartas[1:])
    )
