# lesson-booking-service

A straightforward app for scheduling lessons.

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
- CI/CD: GitHub Actions or Jenkins
- Deployment: AWS (EC2)
- Monitoring: Prometheus + Grafana

### Miscellaneous

- Payment Integration: Stripe/PayPal (if needed for paid lessons)

## Configuration

Once your project environment has been established in IntelliJ IDEA, build the `lesson-booking-service` run:

```shell
  mvn clean package
```

Now, build and run all the services using Docker Compose:

```shell
  docker-compose up --build
```

The service is now accessible on port 8080.

