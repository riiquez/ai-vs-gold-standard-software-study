# Paciência (Solitaire) - Java 25 + JavaFX

Aplicação desktop do jogo Paciência, baseada nos requisitos do documento `DescricaoProjeto-4-8.pdf`.

## Requisitos para executar

- JDK 25.
- Gradle 9.9.1 (ou versão posterior que suporte executar no Java 25).
- Acesso à internet na primeira compilação para baixar Gradle, JavaFX e JUnit.

O projeto utiliza JavaFX 25.0.5. **A JVM que inicia o Gradle e o JDK usado como toolchain para compilar são configurações distintas.** Para este projeto, ambos podem apontar para o JDK 25, desde que o Gradle seja 9.1.0 ou superior.

### Importante: corrigir a incompatibilidade com Gradle 8.14

Gradle 8.14 não pode ser executado usando Java 25. O suporte para executar Gradle sobre Java 25 começou no Gradle 9.1.0. O projeto agora define Gradle 9.9.1 como versão-alvo do Wrapper, mas o Wrapper ainda precisa ser gerado uma primeira vez, pois esta distribuição do projeto não inclui os arquivos `gradlew`/`gradlew.bat`.

Se sua IDE está usando Gradle 8.14 e Java 25, faça a migração inicial assim:

1. Na IDE, altere temporariamente **Gradle JVM** para um JDK entre 17 e 24 (por exemplo, JDK 21). Isso permite que o Gradle 8.14 carregue o projeto apenas para gerar o Wrapper.
2. Na raiz do projeto, execute:

   ```bash
   gradle wrapper
   ```

   A tarefa está configurada no `build.gradle` para gerar o Wrapper apontando para Gradle 9.9.1.
3. Atualize os arquivos do Wrapper usando a versão que acabou de ser gerada:

   ```bash
   ./gradlew wrapper
   ```

4. Na IDE, escolha **Gradle distribution: Wrapper** e altere **Gradle JVM** de volta para JDK 25.
5. Recarregue/sincronize o projeto. Confirme a versão com:

   ```bash
   ./gradlew --version
   ```

   A saída deve indicar Gradle 9.9.1 e JVM 25.

Depois dessa migração, use sempre `./gradlew` (macOS/Linux) ou `gradlew.bat` (Windows), em vez do Gradle instalado globalmente. Assim, a IDE e o terminal utilizarão a versão fixada pelo projeto.

## Executar

macOS/Linux:

```bash
./gradlew run
```

Windows:

```bat
gradlew.bat run
```

Executar os testes: `./gradlew test` (macOS/Linux) ou `gradlew.bat test` (Windows).

## Como jogar

- **Comprar:** clique no espaço do monte. Uma carta é movida para o descarte.
- **Reciclar:** quando o monte estiver vazio e houver cartas no descarte, clique no mesmo espaço. O descarte é invertido e forma um novo monte.
- **Nova partida:** use o botão `Nova partida`. Se monte e descarte estiverem vazios, clicar no espaço do monte também inicia outra partida.
- **Mover:** arraste uma carta visível. Ao arrastar uma carta de uma coluna, todas as cartas acima dela acompanham o movimento.
- **Desfazer:** reverte a última ação, incluindo compra, reciclagem, movimentação e revelação de carta.
- **Jogada automática:** `Próxima jogada automática` executa uma única jogada sugerida pela estratégia. A caixa `Estratégia habilitada` permite desativar a estratégia; desativada, ela retorna uma ação nula.

A pontuação interna não é exibida. A vitória é identificada quando as 52 cartas estão nas fundações.

## Organização do código

```text
src/main/java/br/com/solitaire
├── application
│   ├── DeckFactory.java
│   ├── GameListener.java
│   ├── GameReadOnlyView.java
│   ├── SolitaireGame.java
│   └── SwitchableAutoMoveStrategy.java
├── domain
│   ├── model
│   │   ├── Card, Suit, Rank, CardColor
│   │   ├── CardPile, CardSequence, GameLocation
│   │   ├── GameSnapshot, CardSnapshot, MoveSuggestion
│   │   └── GameAction, CompositeAction, TransferCardsAction,
│   │       SetCardVisibilityAction, RecycleWasteAction, NoOpAction
│   ├── rules
│   │   └── MoveRules.java
│   └── strategy
│       ├── AutoMoveStrategy.java
│       └── PriorityAutoMoveStrategy.java
└── presentation
    └── javafx
        ├── SolitaireApplication.java
        ├── SolitaireController.java
        ├── SolitaireView.java
        └── CardView.java
```

### Decisões de implementação

- **Domínio independente de JavaFX:** a interface só consulta `canMove` e encaminha intenções ao `SolitaireGame`. A regra de legalidade existe em `MoveRules`, não na interface.
- **Ações reversíveis:** compras, reciclagem, movimentações e revelações são registradas como ações. Ações compostas são desfeitas na ordem inversa.
- **Identidade das cartas:** cada carta é uma entidade com identidade por referência. `CardSequence` preserva as referências e a ordem; a serialização Java preserva referências repetidas dentro do mesmo grafo.
- **Estratégia somente de leitura:** `AutoMoveStrategy` recebe `GameReadOnlyView`. A sugestão é revalidada pelo jogo antes da execução.
- **Notificação:** qualquer alteração efetiva e qualquer desfazer publica um novo `GameSnapshot` aos observadores.
- **Sem infraestrutura desnecessária:** aplicação desktop local, sem banco de dados, rede ou microsserviços.

## Observação sobre o ambiente de geração

O código do núcleo foi compilado e exercitado com JDK 21 disponível no ambiente de geração. O projeto está configurado com toolchain Java 25 e JavaFX 25.0.5, mas a compilação integral da interface JavaFX e a execução do Gradle não puderam ser verificadas aqui porque este ambiente não possui JDK 25, Gradle ou acesso à rede para obter as dependências. Execute `gradle test` e `gradle run` em um ambiente com os requisitos acima.
