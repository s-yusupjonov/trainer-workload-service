# trainer-workload-service

Tracks trainer training-workload minutes per year/month. Exposes:

- `POST /api/trainer-workloads`
- `GET /api/trainer-workloads/{username}`
- `GET /api/trainer-workloads/{username}/{year}/{month}`

All `/api/**` endpoints require a bearer JWT issued for an internal caller (see
"Calling the API" below). Other endpoints (`/actuator/**`, `/swagger-ui/**`,
`/v3/api-docs/**`) are open.

## Running locally

```
mvn spring-boot:run
```

The service starts on port `8082` and attempts to register with Eureka using
`eureka.client.service-url.defaultZone`.

## Running with Docker

```
docker build -t trainer-workload-service .
docker run -p 8082:8082 \
  -e EUREKA_URI=http://eureka-host:8761/eureka/ \
  -e INTERNAL_JWT_SECRET=change-me-in-prod \
  -e INTERNAL_AUTH_ALLOWED_CALLERS=gym-crm,discovery-service \
  trainer-workload-service
```

## Environment variables

| Variable | Purpose | Default |
| --- | --- | --- |
| `EUREKA_URI` | Eureka `defaultZone` URL used for service registration/discovery | `http://localhost:8761/eureka/` |
| `INTERNAL_JWT_SECRET` | HMAC-SHA secret used to sign/verify service-to-service JWTs. **Override this in every non-dev environment.** | `th1s-is-a-shared-dev-only-internal-secret-override-in-prod` |
| `INTERNAL_AUTH_ALLOWED_CALLERS` | Comma-separated list of JWT `sub` values permitted to call this service | `gym-crm` |

## API documentation

Once running, Swagger UI is available at:

```
http://localhost:8082/swagger-ui.html
```

The raw OpenAPI document is at `http://localhost:8082/v3/api-docs`.

## Calling the API (generating a throwaway token for manual testing)

Every `/api/**` request must include `Authorization: Bearer <token>`, where
`<token>` is an HS256 JWT whose `sub` claim matches one of the values in
`INTERNAL_AUTH_ALLOWED_CALLERS` and whose signature matches
`INTERNAL_JWT_SECRET`.

A quick way to mint one for manual testing, using Python and `PyJWT`
(`pip install pyjwt`):

```python
import jwt
import datetime

secret = "th1s-is-a-shared-dev-only-internal-secret-override-in-prod"

token = jwt.encode(
    {
        "sub": "gym-crm",
        "iat": datetime.datetime.utcnow(),
        "exp": datetime.datetime.utcnow() + datetime.timedelta(minutes=5),
    },
    secret,
    algorithm="HS256",
)

print(token)
```

Then call the API with it:

```
curl -H "Authorization: Bearer <token>" http://localhost:8082/api/trainer-workloads/trainer.one
```

A request without a valid token returns `401 Unauthorized`; a token whose
`sub` is not in the allow-list returns `403 Forbidden`.

## Request correlation

Every request may include an `X-Transaction-Id` header. If omitted, the
service generates one, logs it for the duration of the request, and echoes it
back on the response (including on error responses, in the `transactionId`
field of the error body).

## Tests and coverage

```
mvn verify
```

Runs the full test suite and enforces a minimum of 80% instruction/line
coverage via the `jacoco-maven-plugin`; the build fails if coverage drops
below that threshold.
# trainer-workload-service
