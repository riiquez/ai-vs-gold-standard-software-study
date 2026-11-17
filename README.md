# Estudo comparativo: IA e Gold Standard no projeto de software

<p align="center">
  <a href="#jetuml"><img alt="JetUML" src="https://img.shields.io/badge/JetUML-diagramas%20UML-1F4E79?style=for-the-badge"></a>
  <a href="#solitaire"><img alt="Solitaire" src="https://img.shields.io/badge/Solitaire-Klondike-1B7F4E?style=for-the-badge"></a>
  <a href="#jetuml-java"><img alt="Java" src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"></a>
  <a href="#solitaire-python"><img alt="Python" src="https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white"></a>
</p>

## Introdução

Estudo comparativo sobre a qualidade do projeto de software produzido por modelos de linguagem. A partir dos mesmos requisitos, foram implementados dois sistemas — o **Solitaire** (paciência Klondike) e o **JetUML** (editor de diagramas UML) — em **Java** e em **Python**, totalizando quatro versões. Cada código foi gerado por uma LLM e depois comparado com uma versão de referência (Gold Standard).

A análise olha a arquitetura e a organização dos projetos, encapsulamento, coesão, acoplamento, agregação e tipagem, além da presença de princípios SOLID, Clean Code e padrões de projeto. O estudo também registra diferenças de qualidade e de manutenibilidade entre as linguagens e o efeito da engenharia de prompts no resultado.

**Autores do estudo:** Henrique Zeu Moura, Sergivaldo Júnior e Ricardo Vilas-Bôas.

## Navegação

| Destino | Atalhos |
| --- | --- |
| Pastas | [JetUML](#jetuml) · [Solitaire](#solitaire) · [Mapa do repositório](#organizacao) |
| Código | [JetUML Java](JetUml/Java) · [JetUML Python](JetUml/python) · [Solitaire Java](Solitaire/Solitaire%20-%20Java) · [Solitaire Python](Solitaire/Solitaire%20-%20Python) |
| Execução | [Requisitos e como rodar](#instalacao) |
| Estudo | [O que foi avaliado](#analise) · [Requisitos e conversas](#materiais) |

---

<a id="organizacao"></a>
<details>
<summary><b>Organização do repositório</b></summary>

<br>

O repositório guarda as quatro implementações geradas para o estudo. As versões de referência (Gold Standard) foram usadas na comparação e não estão nesta árvore.

```text
.
├── JetUml
│   ├── Java          editor UML em Java (Swing)
│   └── python        editor UML em Python (PyQt5)
└── Solitaire
    ├── Solitaire - Java      paciência em Java (JavaFX + Gradle)
    └── Solitaire - Python    paciência em Python (pygame-ce)
```

| Sistema | Linguagem | Pasta | Ponto de entrada |
| --- | --- | --- | --- |
| JetUML | Java | [`JetUml/Java`](JetUml/Java) | [`JetUml.java`](JetUml/Java/src/main/java/jetuml/JetUml.java) |
| JetUML | Python | [`JetUml/python`](JetUml/python) | [`main.py`](JetUml/python/main.py) |
| Solitaire | Java | [`Solitaire/Solitaire - Java`](Solitaire/Solitaire%20-%20Java) | [`SolitaireApplication.java`](Solitaire/Solitaire%20-%20Java/src/main/java/br/com/solitaire/presentation/javafx/SolitaireApplication.java) |
| Solitaire | Python | [`Solitaire/Solitaire - Python`](Solitaire/Solitaire%20-%20Python) | [`main.py`](Solitaire/Solitaire%20-%20Python/main.py) |

Pastas de compilação (`build`, `out`, `.gradle`), caches de IDE e `__pycache__` ficam de fora do histórico. O código-fonte, os testes, os requisitos e os registros de conversa estão versionados.

</details>

<a id="jetuml"></a>
<details>
<summary><b>JetUML — editor de diagramas UML</b></summary>

<br>

Ferramenta de criação de diagramas UML inspirada no JetUML. As duas versões cobrem os mesmos requisitos de produto: cinco tipos de diagrama (classe, sequência, estado, objeto e caso de uso), arquivo JSON, exportação, área de transferência, desfazer/refazer, propriedades, grade, zoom e modo escuro.

O código Java fica em [`JetUml/Java`](JetUml/Java). O código Python fica em [`JetUml/python`](JetUml/python).

<a id="jetuml-java"></a>
<details>
<summary><b>JetUML em Java</b></summary>

<br>

Aplicação desktop com **Swing**, sem dependências externas. A interface, o modelo, as regras, a persistência JSON e os comandos de desfazer ficam sob `src/main/java/jetuml`.

| Caminho | Conteúdo |
| --- | --- |
| [`src/main/java/jetuml`](JetUml/Java/src/main/java/jetuml) | Código do editor (`app`, `cmd`, `controller`, `gui`, `model`, `persist`, `rules`, `view`) |
| [`run.bat`](JetUml/Java/run.bat) | Compila e abre o editor |
| [`verificar.bat`](JetUml/Java/verificar.bat) | Confere regras, JSON e desenho dos diagramas |
| [`conversation`](JetUml/Java/conversation) | Registro da conversa usada na geração |

</details>

<a id="jetuml-python"></a>
<details>
<summary><b>JetUML em Python</b></summary>

<br>

Aplicação desktop com **PyQt5**. O pacote `jetuml` separa apresentação, aplicação, domínio e infraestrutura. A documentação local de execução está em [`README.md`](JetUml/python/README.md) e [`INSTALACAO.md`](JetUml/python/INSTALACAO.md).

| Caminho | Conteúdo |
| --- | --- |
| [`main.py`](JetUml/python/main.py) | Inicialização da aplicação |
| [`jetuml/`](JetUml/python/jetuml) | Núcleo do editor |
| [`requirements.txt`](JetUml/python/requirements.txt) | PyQt5 5.15.9 e Pillow 12.2.0 |
| [`conversation`](JetUml/python/conversation) | Registro da conversa usada na geração |

</details>

</details>

<a id="solitaire"></a>
<details>
<summary><b>Solitaire — paciência Klondike</b></summary>

<br>

Jogo de cartas Klondike: monte, descarte, quatro fundações e sete colunas. As regras de empilhamento, a revelação de cartas, o desfazer e a sugestão de jogada automática ficam no núcleo do jogo. A interface só consulta se a jogada é legal e encaminha a intenção.

O código Java fica em [`Solitaire/Solitaire - Java`](Solitaire/Solitaire%20-%20Java). O código Python fica em [`Solitaire/Solitaire - Python`](Solitaire/Solitaire%20-%20Python).

<a id="solitaire-java"></a>
<details>
<summary><b>Solitaire em Java</b></summary>

<br>

Aplicação **JavaFX** organizada em camadas (`application`, `domain`, `presentation`) e compilada com **Gradle**. O domínio não depende da interface. Há testes de regras, de serialização e do mecanismo do jogo.

| Caminho | Conteúdo |
| --- | --- |
| [`src/main/java`](Solitaire/Solitaire%20-%20Java/src/main/java/br/com/solitaire) | Código do jogo |
| [`src/test/java`](Solitaire/Solitaire%20-%20Java/src/test/java/br/com/solitaire) | Testes JUnit 5 |
| [`build.gradle`](Solitaire/Solitaire%20-%20Java/build.gradle) | Java 25, JavaFX 25.0.5 e JUnit |
| [`README.md`](Solitaire/Solitaire%20-%20Java/README.md) | Como executar e decisões de implementação |
| [`REQUIREMENTS.md`](Solitaire/Solitaire%20-%20Java/REQUIREMENTS.md) | Rastreio dos requisitos no código |
| [`Conversation`](Solitaire/Solitaire%20-%20Java/Conversation) | Registro da conversa usada na geração |

</details>

<a id="solitaire-python"></a>
<details>
<summary><b>Solitaire em Python</b></summary>

<br>

Jogo com interface desenhada pelo **pygame-ce**, sem imagens externas. O pacote `solitaire/nucleo` guarda regras e estado e não importa pygame. A pasta `solitaire/interface` só desenha e envia intenções. Os testes ficam em `tests/`.

| Caminho | Conteúdo |
| --- | --- |
| [`main.py`](Solitaire/Solitaire%20-%20Python/main.py) | Inicialização da partida |
| [`solitaire/nucleo`](Solitaire/Solitaire%20-%20Python/solitaire/nucleo) | Cartas, regras, pilhas, ações e estratégia |
| [`solitaire/interface`](Solitaire/Solitaire%20-%20Python/solitaire/interface) | Desenho e interação |
| [`tests`](Solitaire/Solitaire%20-%20Python/tests) | Testes do núcleo e de eventos |
| [`requirements.txt`](Solitaire/Solitaire%20-%20Python/requirements.txt) | pygame-ce |
| [`README.md`](Solitaire/Solitaire%20-%20Python/README.md) | Como jogar e como a arquitetura foi dividida |
| [`Requisitions`](Solitaire/Solitaire%20-%20Python/Requisitions/Solitaire_requisitos.md) | Requisitos do Solitaire |
| [`Conversation`](Solitaire/Solitaire%20-%20Python/Conversation) | Registro da conversa usada na geração |

</details>

</details>

<a id="instalacao"></a>
<details>
<summary><b>Requisitos de instalação e execução</b></summary>

<br>

Cada versão roda sozinha, a partir da pasta dela. Os comandos abaixo assumem o terminal aberto na raiz deste repositório.

<a id="instalar-jetuml-java"></a>
<details>
<summary><b>JetUML — Java</b></summary>

<br>

- JDK 17 ou mais recente (a compilação usada no estudo passou com o JDK 22).
- Nenhuma biblioteca externa. Swing e o compilador `javac` já vêm no JDK.

Windows, a partir de [`JetUml/Java`](JetUml/Java):

```bat
cd JetUml\Java
run.bat
```

A verificação das regras, do JSON e do desenho:

```bat
cd JetUml\Java
verificar.bat
```

Equivalente manual:

```bat
cd JetUml\Java
javac -encoding UTF-8 -d out -sourcepath src\main\java src\main\java\jetuml\JetUml.java
java -cp out jetuml.JetUml
```

</details>

<a id="instalar-jetuml-python"></a>
<details>
<summary><b>JetUML — Python</b></summary>

<br>

- Python 3.10 ou superior (recomendado 3.10.11) e pip.
- Bibliotecas fixadas em [`requirements.txt`](JetUml/python/requirements.txt): PyQt5 5.15.9, PyQt5-Qt5 5.15.2, PyQt5_sip 12.17.0 e Pillow 12.2.0.

```powershell
cd JetUml\python
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python main.py
```

No Linux ou no macOS, ative o ambiente com `source .venv/bin/activate`. O passo a passo completo está em [`INSTALACAO.md`](JetUml/python/INSTALACAO.md).

</details>

<a id="instalar-solitaire-java"></a>
<details>
<summary><b>Solitaire — Java</b></summary>

<br>

- JDK 25.
- Gradle 9.9.1, já apontado pelo Wrapper do projeto. Na primeira execução é preciso internet para baixar o Gradle, o JavaFX 25.0.5 e o JUnit 5.

Windows:

```bat
cd "Solitaire\Solitaire - Java"
gradlew.bat run
```

Testes:

```bat
gradlew.bat test
```

No Linux ou no macOS, troque `gradlew.bat` por `./gradlew`. Detalhes de ambiente e das regras do jogo estão no [`README`](Solitaire/Solitaire%20-%20Java/README.md) dessa pasta.

</details>

<a id="instalar-solitaire-python"></a>
<details>
<summary><b>Solitaire — Python</b></summary>

<br>

- Python 3 com pip.
- [`pygame-ce`](Solitaire/Solitaire%20-%20Python/requirements.txt) 2.5 ou superior. O jogo funciona offline depois da instalação.

```powershell
cd "Solitaire\Solitaire - Python"
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python main.py
```

Partida reproduzível: `python main.py --semente 42`. Testes: `python -m unittest discover -s tests -t .`. O restante da interface e da arquitetura está no [`README`](Solitaire/Solitaire%20-%20Python/README.md).

</details>

</details>

<a id="analise"></a>
<details>
<summary><b>O que o estudo avalia</b></summary>

<br>

As quatro implementações partem dos mesmos requisitos e servem de material para comparar o código gerado pela LLM com o Gold Standard. Os pontos observados foram:

- arquitetura e organização estrutural de cada projeto;
- encapsulamento, coesão, acoplamento, agregação e tipagem;
- aplicação de SOLID, Clean Code e padrões de projeto;
- diferenças de qualidade e de manutenibilidade entre Java e Python;
- influência da engenharia de prompts no código produzido.

</details>

<a id="materiais"></a>
<details>
<summary><b>Requisitos e registros de conversa</b></summary>

<br>

Os requisitos e as conversas que originaram cada versão ficam junto do código correspondente.

| Implementação | Requisitos | Conversa |
| --- | --- | --- |
| JetUML Java | Roteiro usado na geração, citado na [conversa](JetUml/Java/conversation/java_cursor_jetuml_application_requirements.md) | [`conversation`](JetUml/Java/conversation) |
| JetUML Python | [Requisitos exportados](JetUml/python/python_cursor_jetuml_application_requirements.md) | [`conversation`](JetUml/python/conversation) |
| Solitaire Java | [`REQUIREMENTS.md`](Solitaire/Solitaire%20-%20Java/REQUIREMENTS.md) | [`Conversation`](Solitaire/Solitaire%20-%20Java/Conversation) |
| Solitaire Python | [`Solitaire_requisitos.md`](Solitaire/Solitaire%20-%20Python/Requisitions/Solitaire_requisitos.md) | [`Conversation`](Solitaire/Solitaire%20-%20Python/Conversation) |

</details>
