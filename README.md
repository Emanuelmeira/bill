# Bill - Controle de Custos

Aplicação web para controle de custos pessoais, substituindo planilhas.

## Funcionalidades

- Lançar custos por categoria (valor, categoria, comentário)
- Dashboard com todos os custos organizados por categoria e mês
- Editar e excluir custos lançados
- Total por categoria e total geral
- Gerenciar categorias (adicionar/remover)
- Parcelamento de custos
- Lançamento retroativo por mês

## Tecnologias

- **Backend:** Python Flask
- **Banco de dados:** SurrealDB (via Docker)
- **Frontend:** HTML, CSS, JS (sem frameworks)

## Como rodar

1. Suba o SurrealDB:
```bash
docker compose up -d
```

2. Instale as dependências e inicie:
```bash
pip install -r requirements.txt
python app.py
```

Acesse: [http://localhost:5000](http://localhost:5000)

## Parar o banco

```bash
docker compose down
```
