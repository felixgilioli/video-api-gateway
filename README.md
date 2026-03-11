# Video API Gateway

API Gateway reativa construída com **Spring Cloud Gateway** e **Kotlin**, responsável por ser o ponto central de entrada para o ecossistema de microserviços de processamento de vídeo. Gerencia autenticação JWT via Keycloak, roteamento de requisições, CORS, observabilidade e propagação de contexto do usuário.

---

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Kotlin + Java 21 | Linguagem e runtime |
| Spring Boot 4.0.1 | Framework base |
| Spring Cloud Gateway (WebFlux) | Roteamento reativo |
| Spring Security OAuth2 | Validação JWT e autenticação |
| Keycloak | Provedor de identidade (IdP) |
| Resilience4j | Circuit breaker e tolerância a falhas |
| OpenTelemetry | Rastreamento distribuído |
| Prometheus + Actuator | Métricas e monitoramento |
| Redis Reactive | Suporte a cache reativo |
| Docker | Containerização |
| Gradle | Build tool |

---

## Arquitetura

```
Cliente (Frontend :4200)
        │
        ▼
┌─────────────────────────────┐
│      Video API Gateway      │  :8080
│                             │
│  ┌─────────────────────┐    │
│  │   SecurityConfig    │    │◄── Keycloak :8180
│  │  (OAuth2 / JWT)     │    │
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │   UserInfoFilter    │    │  (Global Filter - adiciona headers X-User-*)
│  └─────────────────────┘    │
│  ┌─────────────────────┐    │
│  │   Roteamento        │    │
│  │  /test/**  → público│    │
│  │  /videos/** → auth  │    │──► Video Service :8081
│  └─────────────────────┘    │
└─────────────────────────────┘
```

---

## Estrutura do Projeto

```
video-api-gateway/
├── src/main/kotlin/br/com/felixgilioli/videoapigateway/
│   ├── VideoApiGatewayApplication.kt   # Entry point
│   ├── config/
│   │   ├── SecurityConfig.kt           # OAuth2, JWT, roles do Keycloak
│   │   └── CorsConfig.kt              # Configuração de CORS
│   ├── filter/
│   │   └── UserInfoFilter.kt          # Extrai claims do JWT e propaga como headers
│   └── controller/
│       ├── HealthController.kt        # Endpoint /health
│       └── TestController.kt          # Endpoints de teste /api/me, /api/hello
├── src/main/resources/
│   └── application.yaml               # Configurações da aplicação
├── Dockerfile                         # Build multi-stage
├── docker-compose.build.yml           # Compose para build local
└── .github/workflows/build.yml        # Pipeline CI/CD
```

---

## Rotas

### Públicas (sem autenticação)

| Método | Path | Destino |
|---|---|---|
| `GET` | `/health` | Gateway health check |
| `GET` | `/actuator/**` | Métricas e status (Actuator) |
| `ANY` | `/test/**` | httpbin.org (rota pública de teste) |

### Autenticadas (requer JWT válido)

| Método | Path | Destino |
|---|---|---|
| `GET` | `/api/me` | Info do usuário autenticado |
| `GET` | `/api/hello` | Saudação autenticada |
| `ANY` | `/protected/**` | httpbin.org (rota protegida de teste) |
| `ANY` | `/videos/**` | Video Service (`:8081`) |

### Headers propagados para serviços downstream

O filtro global `UserInfoFilter` extrai as claims do JWT e adiciona os seguintes headers em todas as requisições autenticadas:

| Header | Descrição |
|---|---|
| `X-User-Id` | Subject (ID) do usuário |
| `X-User-Email` | E-mail do usuário |
| `X-User-Name` | Nome completo |
| `X-User-Username` | Username (preferred_username) |
| `X-User-Roles` | Roles separadas por vírgula |

---

## Configuração

### Pré-requisitos

- JDK 21
- Keycloak rodando em `http://localhost:8180` com o realm `video-processing`
- (Opcional) Docker e Docker Compose

### Variáveis de ambiente principais

| Variável | Padrão | Descrição |
|---|---|---|
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | `http://localhost:8180/realms/video-processing` | Issuer do Keycloak |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | `http://localhost:8180/realms/video-processing/protocol/openid-connect/certs` | JWKs do Keycloak |
| `server.port` | `8080` | Porta da aplicação |

Todas as propriedades do `application.yaml` podem ser sobrescritas via variáveis de ambiente seguindo a convenção do Spring (ex: `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`).

---

## Como Executar

### Localmente

```bash
# Build
./gradlew build

# Executar
./gradlew bootRun
# ou
java -jar build/libs/video-api-gateway-*.jar
```

### Com Docker

```bash
# Build da imagem
docker build -t video-api-gateway .

# Executar o container
docker run -p 8080:8080 \
  -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://keycloak:8180/realms/video-processing \
  video-api-gateway
```

### Com Docker Compose

```bash
docker-compose -f docker-compose.build.yml up
```

---

## Observabilidade

| Endpoint | Descrição |
|---|---|
| `GET /actuator/health` | Status de saúde da aplicação |
| `GET /actuator/metrics` | Lista de métricas disponíveis |
| `GET /actuator/prometheus` | Métricas no formato Prometheus |
| `GET /actuator/info` | Informações da aplicação |

O suporte a rastreamento distribuído via **OpenTelemetry** está configurado e pode ser habilitado apontando para um coletor OTLP (padrão: `http://localhost:4318/v1/traces`).

---

## CI/CD

O pipeline do GitHub Actions (`.github/workflows/build.yml`) executa automaticamente ao fazer push na branch `main`:

1. Build com JDK 21 e Gradle
2. Análise de código com SonarQube
3. Build da imagem Docker
4. Push para o Docker Hub (`felixgilioli/video-api-gateway:latest` e `v{run_number}`)
