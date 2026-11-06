# Pede Fruki — Motor de Recomendação Comercial

## Sobre o projeto

O Pede Fruki é um sistema de recomendação comercial desenvolvido para auxiliar vendedores da Fruki na identificação de oportunidades de venda.

A aplicação analisa dados dos clientes, histórico de compras, perfil comercial e fatores externos para gerar recomendações personalizadas de produtos, contribuindo para uma abordagem de vendas mais estratégica e eficiente.

## Funcionalidades

- **Visão do vendedor:** painel com ranking de produtos recomendados e suas respectivas pontuações.
- **Motor de recomendação:** análise do histórico de compras, segmento, subsegmento, clima, feriados e sazonalidades.
- **Simulação de atendimento:** interface para identificação de clientes, consulta de produtos e realização de pedidos.
- **Gestão de pedidos:** registro dos pedidos realizados no banco de dados.
- **Notificações por e-mail:** envio de confirmações de pedidos por meio da integração com Resend.

## Tecnologias utilizadas

**Backend**
- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring JDBC
- Maven

**Frontend**
- HTML
- CSS
- JavaScript

**Banco de dados**
- PostgreSQL
- Supabase

**Integrações externas**
- Open-Meteo — informações climáticas.
- Nager.Date — feriados nacionais e estaduais.
- Resend — envio de e-mails.

## Como executar o projeto

### Pré-requisitos

- Java JDK 21
- Git
- IntelliJ IDEA ou IDE compatível
- Acesso ao Supabase

### 1. Clonar o repositório

```bash
git clone URL_DO_REPOSITORIO
```

### 2. Configurar as variáveis de ambiente

Configure as credenciais do Supabase e do Resend nas variáveis de ambiente da aplicação. No arquivo `.env`.

### 3. Executar o backend

No Visual Studio Code, execute o arquivo PediFrukiApiApplication.java .

A API estará disponível em:

```text
http://localhost:8080
```

### 4. Executar o frontend

Abra a pasta `fruki-frontend` no Visual Studio Code e execute o arquivo `index.html` utilizando a extensão Live Server.


## Objetivo

Transformar dados comerciais em recomendações inteligentes, auxiliando vendedores na diversificação do portfólio, identificação de oportunidades e melhoria dos resultados de vendas.

