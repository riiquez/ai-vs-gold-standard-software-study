# Rastreabilidade dos requisitos

## Requisitos funcionais

| Requisito | Implementação |
|---|---|
| RF01 - Nova partida | `SolitaireGame.newGame()` embaralha e distribui. O clique no monte compra, recicla o descarte quando o monte está vazio e inicia outra partida quando ambos estão vazios. |
| RF02 - Vitória | `SolitaireGame.isWon()` verifica se as fundações contêm 52 cartas. |
| RF03 - Pontuação | `SolitaireGame.score()` conta as cartas nas fundações. A interface não exibe a pontuação. |
| RF04 - Composição e unicidade | `Card` é uma entidade sem igualdade por valor; `DeckFactory` cria uma instância para cada combinação de naipe e rank. |
| RF05 - Ordem dos ranks | `Rank` define os valores de Ás a Rei e as relações de adjacência. |
| RF06 - Cor dos naipes | `Suit` define a cor e permite comparar a cor de dois naipes. |
| RF07 - Embaralhamento | `DeckFactory.shuffledDeck()` embaralha o baralho completo usando um `RandomGenerator`. |
| RF08 e RF09 - Pilhas | `CardPile` preserva a ordem base-topo, consulta topo/vazio e remove cartas do topo. |
| RF10 - Locais | `GameLocation` identifica monte, descarte, fundação e coluna. `findAccessibleCardLocation()` localiza cartas acessíveis. |
| RF11 e RF12 - Compra e desfazer | `drawFromStock()` usa ação composta de transferência e revelação; desfazer restaura a carta ao monte e oculta novamente. |
| RF13 - Restrição do descarte | `MoveRules` não permite o descarte como destino. |
| RF14 e RF15 - Fundações | Existe uma fundação por naipe; `MoveRules` valida Ás inicial, mesmo naipe, rank seguinte e uma carta por movimento. |
| RF16 - Organização inicial | `SolitaireGame.newGame()` distribui sete colunas com uma a sete cartas e somente o topo visível. |
| RF17 e RF18 - Tableau e sequências | `MoveRules` valida Rei em coluna vazia e alternância de cores/ordem. `sequenceFrom()` retorna a sequência completa até o topo. |
| RF19 - Revelação automática | A movimentação no tableau é composta com a revelação da carta que ficou no topo da origem; desfazer reverte ambas. |
| RF20 - Consultas ao tableau | `snapshot()`, `sequenceFrom()`, `CardSnapshot.faceUp()`, `isVisibleClosestToBase()` e `isVisibleKingAtColumnBase()` fornecem consultas sem mutação. |
| RF21 - Legalidade | `SolitaireGame.canMove()` consulta `MoveRules`; toda execução volta a validar a jogada no mecanismo central. |
| RF22 e RF23 - Ações reversíveis/compostas | `GameAction` e `CompositeAction` executam e revertem ações na ordem adequada. |
| RF24 - Ação nula | `NoOpAction` e `MoveSuggestion.NoOp` representam uma operação sem efeito. |
| RF25 - Histórico e desfazer | `SolitaireGame` registra ações efetivas em ordem e desfaz a última. O teste de histórico longo valida o retorno exato ao estado inicial. |
| RF26 - Notificação | `GameListener` recebe um novo `GameSnapshot` depois de toda alteração efetiva e de cada desfazer. |
| RF27 - Consulta somente leitura | Consultas retornam snapshots imutáveis e não alteram pilhas ou visibilidade. |
| RF28 - Jogo automático | `AutoMoveStrategy` escolhe uma sugestão; `SwitchableAutoMoveStrategy` retorna ação nula quando desabilitada. |
| RF29 - Ordem de prioridade | `PriorityAutoMoveStrategy` implementa as prioridades de compra, descarte para fundação/tableau, tableau para fundação, movimentos com progresso, compra final e ação nula. |
| RF30 - Sem ciclos | A estratégia só considera movimentos de tableau que revelem carta ou esvaziem a coluna; não move Rei da base para coluna vazia. |
| RF31 - Decidir e executar | A estratégia recebe somente `GameReadOnlyView`; `SolitaireGame.executeSuggestion()` revalida e registra a sugestão. |
| RF32 - Arrastar e soltar | `SolitaireView` inicia drag em cartas visíveis, consulta `canMove()` durante o arraste e encaminha o destino ao jogo. O jogo move a sequência inteira. |

## Requisitos não funcionais

| Requisito | Implementação |
|---|---|
| RNF01 - Visibilidade consistente | `GameSnapshot` captura a visibilidade no instante da consulta; a visibilidade real é atualizada pelo domínio e restaurada pelas ações. |
| RNF02 - Tratamento uniforme | Ações simples, compostas e nulas compartilham `GameAction`; estratégia desabilitada retorna sugestão nula. |
| RNF03 - Separação apresentação/regras | JavaFX apenas consulta legalidade e envia intenções; regras ficam em `MoveRules` e `SolitaireGame`. |
| RNF04 - Separação estratégia/regras | A estratégia depende de `GameReadOnlyView`, não da implementação mutável do jogo. |
| RNF05 - Fidelidade na serialização | `Card` e `CardSequence` são serializáveis; a sequência copia a lista sem clonar as cartas. Teste verifica identidade repetida e ordem após serialização. |

## Observação sobre reciclagem do descarte

A decisão confirmada para esta implementação é: quando o monte está vazio e o descarte contém cartas, o clique no espaço do monte inverte o descarte para formar o novo monte. A próxima compra ocorre no clique seguinte. Quando monte e descarte estão vazios, o mesmo espaço inicia uma nova partida.
