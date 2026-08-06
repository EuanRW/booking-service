# booking-service

Generic Java booking system with a modular architecture.

## Architecture

The long-term goal is to keep modules independent while allowing them to collaborate through well-defined service interfaces.

```
booking-system/ 
├── authentication 
├── users 
├── bookings 
└── resources
```

## Tech stack

### Backend:

- Framework: Spring Boot (Spring MVC, Spring Data JPA)
- Database: PostgreSQL
- ORM: Hibernate (via JPA)
- Security: Spring Security (JWT for authentication)
- API Documentation: Swagger/OpenAPI
- Database Migrations: Liquibase
- Build Tool: Maven
- Testing: JUnit (backend)

### DevOps & Deployment:

- Containerization: Docker
- Deployment: AWS (EC2)
- Monitoring: Prometheus + Grafana

## Testing Strategy

The project follows an integration testing methodology, using realistic HTTP requests to validate complete application behaviour. Tests run against the Spring application context with an isolated in-memory database, verifying interactions between controllers, security, services, and persistence layers.


## Configuration

Add a .env with a `JWT_SECRET` env var generated with:

```bash
  openssl rand -base64 64
```

Run the supporting services (Postgres, PGAdmin etc):

```shell
  docker compose up --build
```

Once your project environment has been established in IntelliJ IDEA, build the `booking-service`. Run:

```shell
  mvn clean package
```

Now, run the service:

```shell
  mvn spring-boot:run
```
The service is now accessible on port 8080.

## Editing liquibase

Sometimes when changing entities we're getting liquibase errors. In this case, try deleting the changelog DB table and
running the below command.

```shell
  mvn liquibase:clearCheckSums
```

## Dev links

- PgAdmin: http://localhost:8081/browser/
- Swagger: http://localhost:8080/swagger-ui/index.html#

## Useful dev commands

Stop and remove running containers and related volumes.

```shell
    docker stop $(docker ps -q) && \
    docker rm $(docker ps -aq) && \
    docker volume rm $(docker volume ls -q)
```

