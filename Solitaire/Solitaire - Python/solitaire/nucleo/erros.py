"""Exceções do núcleo do jogo."""


class ErroDeJogo(Exception):
    """Operação estrutural inválida ou violação de uma invariante do jogo.

    Jogadas *ilegais* pedidas pelo jogador não geram exceção: o ``Jogo`` apenas
    as recusa (retorna ``False``). Esta exceção sinaliza uso incorreto interno,
    como tentar retirar uma carta que não está no topo de uma pilha.
    """
