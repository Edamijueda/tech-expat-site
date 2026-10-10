# TechExpat

Agency portfolio and client portal.

## Tech Stack

- Java 25, Spring Boot 4.0.2
- Thymeleaf + Layout Dialect
- Bootstrap 5.3.8
- Gradle 9.3
- Supabase (Postgres)
- NOWPayments for crypto checkout
- Deployed on Railway

## Running

```bash
./gradlew bootRun     # starts on :8080
./gradlew test
```

## Local `.env`

Copy `.env.example` and fill in:

```
DATABASE_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?user=postgres.<project-ref>&password=<pwd>
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<pwd>
NOWPAYMENTS_API_KEY=
NOWPAYMENTS_IPN_SECRET=
NOWPAYMENTS_BASE_URL=https://api-sandbox.nowpayments.io/v1
NOWPAYMENTS_SUCCESS_URL=http://localhost:8080/orders/{orderId}?result=success
NOWPAYMENTS_CANCEL_URL=http://localhost:8080/orders/{orderId}?result=cancel
NOWPAYMENTS_IPN_CALLBACK_URL=https://<ngrok>.ngrok.io/webhooks/nowpayments
```

Notes:
- Supabase: use **Session pooler** URI from Connect panel (port 5432, not 6543), prepend `jdbc:` yourself.
- Schema: paste `supabase/migrations/0001__orders.sql` into Supabase SQL editor once.
- Admin: dev defaults to `admin/admin` if unset; prod refuses to boot without these.
- NOWPayments: sandbox and prod are separate accounts with separate keys; base URL must match.
- Webhook: NOWPayments can't reach localhost — `ngrok http 8080`, paste the HTTPS URL + `/webhooks/nowpayments`.

## URL map

| URL | Access |
|---|---|
| `/course-outline` | Public — free PDFs |
| `/admin/course-outline` | Admin — same, with Pay buttons |
| `/checkout/{slug}` | Public, unlinked |
| `/orders/{id}` | Public — buyer receipt |
| `/admin/orders` | Admin — all orders |
| `/admin/testing/{id}` | Admin — stand-in delivery page |
| `/webhooks/nowpayments` | NOWPayments only (HMAC-SHA512) |

## Deploy (Railway)

Set the same env vars; swap sandbox URL for `https://api.nowpayments.io/v1` and localhost URLs for the prod domain. Railway sets `PORT` and `-Dspring.profiles.active=prod` automatically.

## Receiving in Nigeria

NOWPayments is non-custodial — funds go to the wallet configured in its dashboard (USDT/TRC20 recommended). Off-ramp to NGN via Binance P2P, Yellow Card, or Busha. CBN restrictions on local gateways do not apply.
