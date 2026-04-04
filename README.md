# Bill - Controle de Custos

Aplicação web para controle de custos pessoais, substituindo planilhas.

## Funcionalidades

- Lançar custos por categoria (valor, categoria, comentário)
- Dashboard com todos os custos organizados por categoria e mês
- Editar e excluir custos lançados
- Total por categoria e total geral
- Gerenciar categorias (adicionar/remover)
- Parcelamento de custos (até 48x)
- Lançamento retroativo por mês
- API JSON para consulta de custos

## Tecnologias

- **Backend:** Java 21 + Spring Boot 3.4
- **Banco de dados:** MongoDB (via Docker)
- **Templates:** Thymeleaf
- **Frontend:** HTML, CSS, JS (sem frameworks)

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose

## Como rodar (desenvolvimento)

1. Suba o MongoDB:
```bash
docker compose up mongodb -d
```

2. Compile e inicie a aplicação:
```bash
mvn spring-boot:run -DskipDocker
```

Acesse: [http://localhost:8080](http://localhost:8080)

## Build + Docker

O `mvn package` compila o projeto e cria a imagem Docker automaticamente:
```bash
mvn clean package -DskipTests
```

Isso gera as imagens `bill:1.0.0` e `bill:latest`.

### Rodar tudo via Docker Compose

```bash
docker compose up -d
```

### Push para Docker Hub

```bash
# Build com o nome do seu repositório
mvn clean package -DskipTests -Ddocker.image.name=SEU_USUARIO/bill

# Push
docker push SEU_USUARIO/bill:1.0.0
docker push SEU_USUARIO/bill:latest
```

### Compilar sem Docker

```bash
mvn clean package -DskipTests -DskipDocker
```

## Estrutura do projeto

```
src/main/java/com/bill/
├── BillApplication.java        # Classe principal
├── config/DataSeeder.java      # Seed de categorias padrão
├── model/                      # Entidades MongoDB
├── repository/                 # Repositórios Spring Data
├── service/                    # Lógica de negócio
├── controller/                 # Controllers MVC + API REST
├── dto/                        # Objetos de transferência
└── util/                       # Utilitários (formatação)
```
