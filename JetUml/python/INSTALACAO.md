# Instalação — JetUML (Python)

## Requisitos

- **Python** 3.10 ou superior (recomendado 3.10.11)
- **pip** atualizado

## Bibliotecas e versões

| Biblioteca   | Versão   | Uso no projeto                                      |
|-------------|----------|-----------------------------------------------------|
| PyQt5       | 5.15.9   | Interface gráfica, canvas, menus, atalhos, export   |
| PyQt5-Qt5   | 5.15.2   | Binários Qt5 (dependência do PyQt5)                  |
| PyQt5_sip   | 12.17.0  | Bindings SIP (dependência do PyQt5)                 |
| Pillow      | 12.2.0   | Suporte a exportação JPEG e manipulação de imagens  |

Todas estão listadas em `requirements.txt` com versões fixas.

## Passos

```powershell
cd "c:\Users\MEUPC\Desktop\Estudos python\JetUml\python"
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

Se você já tem PyQt5 e Pillow nas versões acima, basta:

```powershell
pip install -r requirements.txt
```

## Executar

```powershell
python main.py
```

## Formato de arquivo

Diagramas são salvos em **JSON**. A estrutura está documentada em `docs/FORMATO_JSON.md`.