# Paciência Klondike (Python 3 + pygame-ce)

Jogo completo de Paciência Klondike, com interface renderizada pelo Pygame
(cartas desenhadas por código, sem imagens externas) e funcionamento offline.

## Instalação e execução

```bash
python -m venv .venv && source .venv/bin/activate    # Windows: .venv\Scripts\activate
pip install -r requirements.txt                        # pygame-ce
python main.py                  # ou: python -m solitaire
python main.py --semente 42     # partida reproduzível
```

## Como jogar

| Ação | Como |
|---|---|
| Comprar | clique no monte (uma carta por vez, sem reciclagem) |
| Mover | arraste a carta; as cartas sobre ela vão juntas |
| Feedback | durante o arraste o destino fica verde (aceita) ou vermelho (recusa) |
| Fundação | duplo clique na carta |
| Desfazer | botão **Desfazer** ou tecla `Z` |
| Jogada automática | botão ou tecla `A` (uma jogada); **Auto-jogar** repete até acabar |
| Estratégia | botão liga/desliga o jogo automático |
| Nova partida | com o monte vazio, clique nele (pede confirmação se o jogo não foi vencido) |

A pontuação (cartas nas fundações) não é exibida: serve para detectar a vitória.

## Arquitetura

```
solitaire/
├── nucleo/            # regras e estado — NÃO importa pygame
│   ├── cartas.py      # Naipe, Cor, Rank, Carta, Baralho            (RF04–07, RNF05)
│   ├── regras.py      # regras puras de empilhamento                (RF15, RF17)
│   ├── pilhas.py      # Pilha, Local, Monte, Descarte, Fundacao, Coluna (RF08–20)
│   ├── acoes.py       # Acao, Mover, Revelar, Composta, Nula, Historico (RF22–25, RNF01–02)
│   ├── jogadas.py     # intenções: Comprar, Mover, Nenhuma
│   ├── estrategia.py  # Estrategia, Automatica, Desativada          (RF28–31, RNF04)
│   └── jogo.py        # Jogo (mecanismo central), ConsultaJogo, Evento (RF01–03, 21, 26, 27)
└── interface/         # Pygame — consulta o núcleo, nunca o contrário
    ├── tema.py  desenho.py  layout.py  widgets.py  aplicacao.py   (RF32, RNF03)
tests/                 # 64 testes
```

Decisões principais:

* **Decidir x executar.** A estratégia recebe um `ConsultaJogo` (somente leitura) e devolve
  uma `Jogada`. `Jogo.jogar` converte em `Acao`, valida a legalidade (`pode_mover`),
  executa, registra no histórico e notifica os observadores. Jogador e estratégia usam o mesmo caminho.
* **Ações uniformes.** Simples, compostas (mover + revelar), nulas e "estratégia desativada"
  compartilham `executar() -> bool` / `desfazer()`; nenhuma checagem por tipo.
* **Visibilidade (RNF01).** Ações restauram a visibilidade anterior ao desfazer.
* **Sem ciclos (RF30).** Cada jogada automática reduz lexicograficamente
  (monte, descarte, cartas fora das fundações, cartas ocultas, colunas não vazias).
* **Regras só no jogo.** A interface pergunta `Jogo.pode_mover(...)`; não há regra de legalidade nela.
* **Serialização fiel (RNF05).** O arraste guarda `Carta.id`s e o jogo os reconstitui nos mesmos objetos.

## Testes

```bash
python -m unittest discover -s tests -t .      # ou: pytest
```
Incluem 150 partidas automáticas sem repetição de estado, desfazer tudo até o estado inicial
e continuar, e eventos de mouse simulados (Pygame em modo `dummy`).
