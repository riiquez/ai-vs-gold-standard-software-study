# JetUML (Python)

Editor de diagramas UML no estilo do JetUML, implementado com **PyQt5**.

## Instalação e execução

Consulte [INSTALACAO.md](INSTALACAO.md) para bibliotecas, versões e comandos.

```powershell
python main.py
```

## Requisitos atendidos

O roteiro em `roteiro.txt` orienta o produto. Principais capacidades:

- Cinco tipos de diagrama em abas (RF01)
- JSON, Recent Files, Duplicate, Export, Clipboard (RF02–RF06)
- Barra de ferramentas, atalhos 1–9/0/A…, menu de contexto, tooltips (RF07–RF10)
- Nós, notas, pacotes, arestas com validação (RF11–RF20)
- Undo/Redo, propriedades, estereótipos Ctrl+Q, formatação `/` e `_` no modelo de texto (RF21–RF24 parcial na UI de propriedades)
- Regras de objeto, sequência, seleção e clipboard interno (RF25–RF35)
- Grade, zoom, tamanho, fonte, dark mode, notificações (RF36–RF42)

Diagrama de **sequência** (lifelines, activations, fluxo RF27–RF31) é a parte mais especializada; a base de validação e ferramentas Call/Return está presente, com evolução contínua recomendada.

## Arquitetura

Camadas (apresentação, aplicação, domínio, infraestrutura) e MVC conforme o Cap. 7 de *Engenharia de Software Moderna*: [docs/ARQUITETURA.md](docs/ARQUITETURA.md).

## Formato de arquivo

Ver [docs/FORMATO_JSON.md](docs/FORMATO_JSON.md).