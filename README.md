# booking-service

A generic booking service that can manage lessons, rooms, equipment, and other bookable resources.

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


