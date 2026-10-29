# BookMyHive - .NET 10 extras (hero marquee + reviews/comments)

Two features, built as a small .NET 10 sidecar service next to the existing
Spring Boot + React app. The Java backend is untouched; the React frontend
talks to both APIs. Login is shared: the .NET service validates the SAME JWT
the Java backend issues, so users log in once.

## What's in here

- `dotnet-backend/BookMyHive.Api` - ASP.NET Core (.NET 10) Web API:
  - Hero banners: `GET /api/hero-banners` (public), `POST/DELETE` (ADMIN role)
  - Reviews: `GET /api/events/{id}/reviews`, `GET .../reviews/summary` (public),
    `POST` (signed-in; one review per user per event; 1-5 stars), `DELETE /api/reviews/{id}` (ADMIN)
  - Comments: `GET /api/events/{id}/comments` (public), `POST` (signed-in), `DELETE` (ADMIN)
- `dotnet-backend/BookMyHive.Api.Tests` - xunit integration tests (8, all passing)
- `frontend/` changes - `HeroMarquee` (PVR-style auto-sliding hero carousel) and
  `EventExtras` (reviews + comments) components, wired into `Home.jsx` /
  `EventDetail.jsx`, plus `api.js`, `styles.css`, `vite.config.js`.

## Hero behavior

The carousel shows 3 featured titles at a time and auto-advances between them
every 5 seconds. The featured 3 are re-picked automatically every 6 hours from
the pool - all active banners, or the top 10 rated events with backdrop images
from the live catalogue when no banners exist - and the rotation keeps moving
even while the page stays open. Every "Book now" button links to that title's
real `/events/<id>` page. Admins can pin banners via the API; the pool is
uncapped, the display window is 3.

## Prerequisites

- .NET SDK 10 (`dotnet --version` -> 10.x)
- Node 18+ for the frontend
- Java 17 for the existing backend

## Setup

1. Shared JWT secret (required for both backends): pick one strong secret
   (32+ characters) and give it to BOTH services - the Java app needs it to
   sign tokens, the .NET service needs the same value to validate them:

       export JWT_SECRET="<your strong secret>"      # Java backend (required)
       export Jwt__Secret="<same value>"             # .NET sidecar (required)

   Neither side ships a default. The .NET API refuses to start without it.

2. TMDB key (required by the Java backend for catalogue sync):

       export TMDB_API_KEY="<your own TMDB key>"

3. Run the Java backend (default port 8080), then the extras API (port 5080):

       cd dotnet-backend/BookMyHive.Api
       dotnet run --urls http://localhost:5080

4. Run the tests:

       cd dotnet-backend/BookMyHive.Api.Tests
       dotnet test

5. Frontend: `cd frontend && npm install && npm run dev`. The extras client
   defaults to `http://localhost:5080/api`; override with `VITE_EXTRAS_API_URL`.
   `vite.config.js` lets you point the Java proxy with `JAVA_ORIGIN`
   (defaults to http://localhost:8080).

## Managing hero banners

With an ADMIN token from the Java login (`POST /api/auth/login`):

    curl -X POST http://localhost:5080/api/hero-banners \
      -H "Authorization: Bearer <admin token>" -H "Content-Type: application/json" \
      -d '{"eventId": 6, "title": "Wild Horse Nine", "subtitle": "Now showing",
           "imageUrl": "https://image.tmdb.org/t/p/original/<backdrop>.jpg",
           "ctaUrl": "/events/6", "sortOrder": 1}'

## Database: proven vs configured

- SQLite (default, file `bookmyhive-extras.db`) is what was run and tested locally.
- MySQL 8 is CONFIGURED but NOT runtime-tested: set `Database:Provider=MySql` and
  `ConnectionStrings:Extras`. Note: Pomelo (the usual MySQL EF provider) has no
  EF Core 10 build yet, so the MySQL path uses Oracle's official
  `MySql.EntityFrameworkCore` 10.0.12. Schema is created automatically on startup.

## Notes / limits

- The hero is structurally PVR-style (full-width auto-sliding banner carousel);
  pvrcinemas.com blocks datacenter IPs, so this is not a pixel copy.
- Banner management and moderation are API-only (no admin screen yet).
- Identity for reviews/comments comes from the JWT subject (email); the email is
  stored for the one-review-per-user rule but never returned by the API.
