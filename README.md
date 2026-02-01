# MakerSpaceTools

> Fast, reliable MakerSpace bookings with a clean UI, a focused API, and an auto-booking scheduler.

MakerSpaceTools is a Spring Boot web app that wraps the MakerSpace (Nexudus) API. It standardizes how availability is checked, how bookings are validated, and how recurring slots are claimed, so you do not have to race the calendar every week.

```text
Browser UI -> MakerSpaceTools API -> MakerSpace/Nexudus API
```

## At a glance

| Layer | Purpose | Location |
| --- | --- | --- |
| UI | Single-page booking experience | `src/main/resources/static` |
| API | REST endpoints for booking and automation | `com.makerspacetools.controller` |
| Automation | Scheduler, job storage, attempt feed | `com.makerspacetools.automation` |
| Auth | Token lifecycle + fallback credentials | `com.makerspacetools.auth` |

## Why it exists

Booking an embroidery machine should not feel like a race. This service turns the MakerSpace booking workflow into a predictable flow: check availability, validate time windows, preview invoice constraints, and submit the booking in one consistent path. For recurring needs, it schedules auto-booking jobs that keep trying within the allowed booking window and reports every attempt.

## Highlights

- Calendar-based availability for the configured machine.
- Time-range selection with live validation for booking rules.
- Booking preview + submission flow backed by MakerSpace invoices.
- Pending bookings list with quick cancellation.
- Auto booking mode for weekly recurring slots with job management.
- Attempt feed that shows auto-booking outcomes in near real time.
- Session-based login with optional fallback credentials for automation.

## How it works

### Manual booking flow

1. UI loads availability for a chosen date.
2. Backend validates time range (workday, duration, horizon, slot alignment).
3. Backend previews the invoice through MakerSpace to catch API-side issues.
4. If preview succeeds, the booking is submitted.

### Auto booking flow

1. You select a date and time range in Auto mode.
2. A job is persisted to `automation.jobs-file` (default `data/auto-booking-jobs.json`).
3. A scheduler polls every `automation.scheduler-delay` and tries to book the next eligible occurrence.
4. Each attempt is recorded in an in-memory feed and shown in the UI.

Scheduled attempts run inside `MakerSpaceAuthService.runWithFallback(...)`, which allows automation to use configured credentials even when no one is logged in.

## Booking rules enforced

| Rule | Value |
| --- | --- |
| Working hours | 09:00 to 17:00 |
| Maximum duration | 4 hours |
| Maximum horizon | 360 hours ahead |
| Slot alignment | 30-minute increments |

These constraints are enforced in `BookingValidator` and `AutoBookingJobService`.

## Quick start

Prerequisites:

- Java 25 (see `build.gradle`)

Run:

```bash
./gradlew bootRun
```

Open `http://localhost:8080` and sign in from the UI.

## Configuration

Settings live in `application.yml` and environment variables.

### Required setup data

- `data.coworker.id`
- `data.embroidery-machine.guid`
- `data.embroidery-machine.id`

### MakerSpace endpoint

- `makerspace.base-url` (default `https://makerspace.spaces.nexudus.com`)

### Authentication (fallback for automation)

- `MAKERSPACE_USERNAME`
- `MAKERSPACE_PASSWORD`
- `MAKERSPACE_CLIENT_ID`
- `MAKERSPACE_TOTP` (optional)

See `.env-example` for a template.

### Automation tuning

- `automation.jobs-file` (defaults to `data/auto-booking-jobs.json`)
- `automation.scheduler-delay` (defaults to `PT1M`)
- `automation.attempt-interval` (defaults to `PT5M`)
- `automation.feed-size` (defaults to `200`)

## API overview

| Area | Method | Endpoint |
| --- | --- | --- |
| Auth | POST | `/api/auth/login` |
| Auth | GET | `/api/auth/status` |
| Auth | POST | `/api/auth/logout` |
| Machines | GET | `/api/machines/availability?date=YYYY-MM-DD` |
| Machines | POST | `/api/machines/bookings` |
| Bookings | GET | `/api/bookings/pending` |
| Bookings | POST | `/api/bookings/cancel/{bookingId}` |
| Automation | GET | `/api/automation/jobs` |
| Automation | POST | `/api/automation/jobs` |
| Automation | PATCH | `/api/automation/jobs/{jobId}` |
| Automation | DELETE | `/api/automation/jobs/{jobId}` |
| Automation | GET | `/api/automation/attempts?limit=100` |

Sample request and response payloads live under `httpclient/`.

## Project map

Backend:

- `com.makerspacetools.controller` REST API.
- `com.makerspacetools.service` booking validation, preview, submission.
- `com.makerspacetools.automation` auto-booking jobs, scheduler, attempt feed.
- `com.makerspacetools.auth` token management and login workflows.
- `com.makerspacetools.client` MakerSpace API client adapters.

Frontend:

- `src/main/resources/static/index.html` single-page UI.
- `static/app.js` app wiring.
- `static/api.js` API wrapper.
- `static/ui/*` calendar, slots, auth, auto-booking.

## Data persistence

- Auto-booking jobs are stored in a JSON file (`automation.jobs-file`).
- Attempt logs are in memory only and reset on restart.
