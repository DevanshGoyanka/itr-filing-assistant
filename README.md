# ITR-1 Tax Filing Assistant

## Phase 1: Login, Client Management, Prefill Upload

### Tech Stack
- **Backend:** Spring Boot 3 + Spring Security + JWT
- **Database:** PostgreSQL 15
- **Frontend:** Next.js 15 + TypeScript + TailwindCSS + Zustand
- **Infrastructure:** Docker Compose

### Quick Start

```bash
# Start all services with Docker
docker-compose up --build

# Or run individually:

# 1. Start PostgreSQL
docker-compose up postgres

# 2. Start Backend (requires Java 17+)
cd backend && ./mvnw spring-boot:run

# 3. Start Frontend (requires Node.js 18+)
cd frontend && npm install && npm run dev
```

### Services
| Service  | URL                          |
|----------|------------------------------|
| Frontend | http://localhost:3000         |
| Backend  | http://localhost:8080         |
| Swagger  | http://localhost:8080/swagger-ui.html |
| Postgres | localhost:5432               |

### Test Credentials
| User  | Email              | Password    |
|-------|--------------------|-------------|
| user1 | user1@itr.com      | password123 |
| user2 | user2@itr.com      | password123 |

### Environment Variables
| Variable           | Default Value                    |
|--------------------|----------------------------------|
| JWT_SECRET         | (set in .env)                    |
| JWT_EXPIRATION     | 86400000 (24 hours)              |
| NEXT_PUBLIC_API_URL| http://localhost:8080/api         |
| POSTGRES_PASSWORD  | itr_pass                         |
