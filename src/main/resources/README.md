# User Service - Running in Different Environments

This project uses Spring profiles to control environment-specific behavior, including logging.

## Spring Profiles

The application has **two levels of profiles**:

1. **local/docker** – depends on running environment.
2. **Secondary profile** (`SPRING_SECONDARY_PROFILE`) – optional: `prod (default)` or `dev`.

**Behavior of LoggingAspect:**

| Environment / Profiles          | Logging Level & Details                                |
|--------------------------------|------------------------------------------------------|
| Local / docker + prod (default) | Minimal logs, no method arguments or results   |
| Local / docker + dev             | Detailed logs, includes method arguments and results |

---

## Running Locally

Default profile is `local` and `prod`.

To redefine profiles when running locally, you can use environment variables:

```bash
SPRING_SECONDARY_PROFILE=dev ./mvnw spring-boot:run
```

Or set it as an environment variable:

```bash
export SPRING_SECONDARY_PROFILE=dev
./mvnw spring-boot:run
```

---

## Running in Docker

Default profile is `docker` and `prod`.

To redefine profiles when running in Docker, add environment variables:

```bash
SPRING_SECONDARY_PROFILE=dev docker-compose up
```