# Bill - Controle de Custos

Aplicação web para controle de custos pessoais, substituindo planilhas.

## Funcionalidades

- Lançar custos por categoria (valor, categoria, comentário)
- Dashboard com todos os custos organizados por categoria
- Editar e excluir custos lançados
- Total por categoria e total geral
- Gerenciar categorias (adicionar/remover)

## Tecnologias

- **Backend:** Python Flask
- **Banco de dados:** SQLite
- **Frontend:** HTML, CSS, JS (sem frameworks)

## Como rodar

```bash
pip install -r requirements.txt
flask db upgrade
python app.py
```

Acesse: [http://localhost:5000](http://localhost:5000)

## Migrations

O projeto usa **Flask-Migrate** (Alembic) para controle de versão do banco de dados.

```bash
# Aplicar migrations pendentes
flask db upgrade

# Gerar nova migration após alterar um modelo
flask db migrate -m "descricao da mudanca"

# Ver migration atual
flask db current

# Reverter última migration
flask db downgrade
```
