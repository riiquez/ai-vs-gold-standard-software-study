# Requisitos Solitaire:

## 1. Partida
- RF01 — Nova partida. O jogo deve embaralhar o baralho e distribuir as cartas na posição inicial: 28 cartas no tableau, 24 no monte, e fundações e descarte vazios. Para o jogador, a opção fica disponível quando o monte estiver vazio, reaproveitando a ação de comprar.
- RF02 — Vitória. A partida está vencida quando as 52 cartas estiverem nas fundações.
- RF03 — Pontuação. A pontuação é o número de cartas nas fundações. Ela não aparece na interface; serve para detectar a vitória e para estatísticas de simulações automáticas.

## 2. Cartas e baralho
- RF04 — Composição e unicidade. O baralho tem 52 cartas: 4 naipes (Copas, Ouros, Paus, Espadas) × 13 ranks, sem repetição. Cada carta é uma única entidade; qualquer referência a ela, em qualquer parte do jogo, deve se comportar como a mesma carta.
- RF05 — Ordem dos ranks. Os ranks seguem uma ordem total (Ás, 2, ..., Valete, Dama, Rei), permitindo saber qual carta é imediatamente superior ou inferior a outra.
- RF06 — Cor dos naipes. Copas e Ouros são vermelhos; Paus e Espadas são pretos. Deve ser possível comparar a cor de dois naipes.
- RF07 — Embaralhamento. O baralho completo deve poder ser embaralhado em ordem aleatória, para que partidas diferentes não repitam necessariamente a disposição.

## 3. Pilhas e locais
- RF08 — Pilha. Monte, descarte, fundações e colunas do tableau usam a mesma estrutura de pilha:
* preserva a ordem de colocação, da base ao topo;
* só o topo pode ser jogado ou removido diretamente (exceto na movimentação de sequências do tableau, RF18).
- RF09 — Consultas à pilha. Deve ser possível saber se uma pilha está vazia e consultar a carta do topo sem removê-la.
- RF10 — Locais. Um local é uma coluna, uma fundação, o monte ou o descarte. O jogo os trata de forma equivalente quando a regra não depende do tipo (por exemplo, "está vazio?"). Deve ser possível descobrir em que local está uma carta acessível para movimentação.

## 4. Monte e descarte
- RF11 — Compra. Enquanto o monte não estiver vazio, o jogo deve permitir retirar a carta do topo e colocá-la, visível, no topo do descarte. É a única forma de as 24 cartas do monte chegarem ao tableau ou às fundações.
- RF12 — Desfazer compra. Desfazer uma compra devolve exatamente a mesma carta ao topo do monte, removendo-a do descarte.
- RF13 — Restrição do descarte. O descarte não é destino válido de movimentação direta; só recebe cartas pela compra.

## 5. Fundações
- RF14 — Estrutura. Há quatro fundações, uma por naipe, preenchidas do Ás ao Rei.
- RF15 — Regras de empilhamento.
* Fundação vazia: só aceita Ás.
* Fundação não vazia: só aceita carta do mesmo naipe e de rank imediatamente superior ao do topo.
* Apenas uma carta por vez pode ser movida para uma fundação.

## 6. Tableau
- RF16 — Organização inicial. São sete colunas, com 1 a 7 cartas. Apenas a última carta de cada coluna começa visível. (coluna 1 com 1 carta, coluna 2 com 2 e assim sucessivamente)
- RF17 — Regras de empilhamento.
* Coluna vazia: só aceita Rei.
* Coluna não vazia: aceita carta de rank imediatamente inferior ao do topo e de cor de naipe diferente (ex.: 6 preto sobre 7 vermelho).
- RF18 — Movimentação de sequências. Ao mover uma carta do tableau, todas as cartas que estão sobre ela vão junto, na mesma ordem, como uma única unidade (ex.: mover um 8 vermelho que tem um 7 preto e um 6 vermelho sobre ele move os três). Isso vale para qualquer forma de movimentação, inclusive o arraste (RF32). A regra vale para movimentos entre colunas; para fundações, só uma carta por vez (RF15).
- RF19 — Revelação automática. Quando uma carta visível sai de uma coluna e há uma carta oculta abaixo, esta é revelada como parte da mesma jogada. Desfazer a jogada oculta a carta de novo.
- RF20 — Consultas ao tableau. Sem alterar o estado, deve ser possível obter:
* as cartas de uma coluna;
* a sequência que vai de uma carta até o topo da coluna (as cartas que seriam movidas junto com ela);
* se uma carta está visível e se é a visível mais próxima da base;
* se um Rei visível já está na base da coluna (movê-lo para outra coluna vazia não traria ganho).

## 7. Legalidade das jogadas
- RF21 — Legalidade. Uma carta (ou sequência) só pode ser movida se respeitar as regras de empilhamento do destino (RF15 ou RF17), independentemente de como a jogada foi solicitada. As regras ficam no jogo, e a interface só as consulta.

## 8. Ações, histórico e estado
- RF22 — Ações reversíveis. Toda ação deve poder ser desfeita, restaurando o estado anterior.
- RF23 — Ação composta. Um conjunto de ações relacionadas pode ser tratado como uma única jogada. Ela executa as ações na ordem e, ao ser desfeita, reverte-as na ordem inversa. Exemplo: mover + revelar carta; desfazer oculta a carta e depois desfaz o movimento.
- RF24 — Ação nula. Deve existir a representação de uma ação sem efeito, por exemplo quando o jogo automático não encontra jogada.
- RF25 — Histórico e desfazer. O jogo mantém o histórico das jogadas na ordem real e permite desfazer a última, quando houver alguma. Desfazer todas as jogadas deve devolver o jogo exatamente ao estado inicial, pronto para continuar (inclusive com jogo automático), mesmo após uma sequência longa.
- RF26 — Notificação. Toda mudança efetiva de estado (cartas, visibilidade, pontuação etc.), inclusive ao desfazer, notifica os interessados, como a interface.
- RF27 — Consulta somente leitura. Todas as consultas ao estado (pilhas, colunas, fundações, descarte etc.) nunca alteram o estado do jogo.

## 9. Jogo automático
- RF28 — Jogada automática. A qualquer momento, o jogo deve ser capaz de escolher uma jogada legal para fazer sem intervenção do jogador. Se não houver nenhuma, nada acontece (ação nula, RF24). O recurso pode ser desativado, de modo que nenhuma jogada automática ocorra, sem exigir tratamento especial no resto do jogo.
- RF29 — Ordem de prioridade. Quando houver mais de uma jogada possível, o jogo avalia as opções abaixo na ordem em que aparecem e executa a primeira que for possível. As seguintes só são consideradas se as anteriores não forem possíveis.
* 1. Comprar uma carta do monte: O descarte está vazio e há cartas no monte.
* 2. Descarte → fundação: A carta do topo do descarte pode ir para uma fundação.
* 3. Descarte → tableau: A carta do topo do descarte pode ir para uma coluna.
* 4. Coluna do tableau → fundação: A carta do topo de alguma coluna pode ir para uma fundação.
* 5. Mover dentro do tableau: O movimento revela uma carta oculta ou deixa uma coluna vazia.
* 6. Comprar uma carta do monte: Nenhuma das jogadas anteriores é possível e há cartas no monte.
* 7. Nenhuma ação: Não há nenhuma jogada possível.
- RF30 — Sem ciclos. A sequência de jogadas automáticas não pode entrar em loop sem progresso. Por isso:
* não se move um Rei que já está na base de sua coluna para outra coluna vazia (RF20);
* na prioridade 5, só se move dentro do tableau se houver ganho concreto (revelar carta ou esvaziar coluna), o que impede mover a mesma carta de uma coluna para outra e de volta sem avançar.
- RF31 — Separação entre decidir e executar. O jogo automático funciona em duas etapas separadas:
* 1. Decidir: a estratégia analisa o estado do jogo usando apenas consultas somente leitura (RF27) e sugere uma jogada. Ela não altera o estado.
* 2. Executar: o mecanismo central do jogo recebe a sugestão, verifica a legalidade como faria com qualquer jogada do jogador (RF21) e a executa, registrando-a no histórico (RF25) e notificando a interface (RF26).

## 10. Interação 
- RF32 — Movimentação direta. O jogador pode iniciar e concluir uma movimentação arrastando com o mouse. Ao arrastar uma carta do tableau, todas as cartas sobre ela são arrastadas juntas (RF18). Durante o arraste, a interface indica se o destino sob o cursor aceitaria a jogada, consultando as regras do jogo (RF21).

# Requisitos Não Funcionais
- RNF01 — Consistência de visibilidade. A visibilidade de uma carta deve sempre refletir a carta que está de fato na posição naquele momento, nunca a de uma jogada anterior.
- RNF02 — Tratamento uniforme de ações. Ações simples, compostas, nulas e "estratégia desativada" devem ser tratadas de forma equivalente pelo restante do jogo, sem verificações especiais.
- RNF03 — Separação entre apresentação e regras. A interface não contém regras de legalidade; ela as consulta no jogo (RF21), evitando a mesma regra em dois lugares.
- RNF04 — Separação entre estratégia e regras. A estratégia automática depende só de consultas somente leitura (RF31).
- RNF05 — Fidelidade na serialização. Converter cartas ou sequências para armazenamento ou transmissão (ex.: ao arrastar uma sequência) e reconstituí-las não pode alterar a identidade ou a ordem das cartas.
