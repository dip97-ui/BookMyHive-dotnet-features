# Deploying BookMyHive (demo-grade, free tiers)

Status: NOT deployed. This is a plan. The parts marked TESTED were run locally; everything else is untested.

## Shape
- Frontend: Vercel Hobby (static React build).
- Java API (Spring Boot 3, Java 17): Render free Docker web service.
- .NET API (ASP.NET Core 10, hero banners / reviews / comments): Render free Docker web service.
- Database: one Aiven free MySQL service holding TWO databases (one per API).

## Free-tier facts to know (checked against the vendors' docs on 2026-10-29)
- Render free: 512 MB RAM, 0.1 CPU, spins down after 15 min idle (about 1 min cold start), ephemeral disk. SQLite files are lost on restart, so the .NET API must use MySQL here.
- Aiven free MySQL: 1 GB RAM, 1 GB disk, 76 connections, no static IP, may be powered off when idle (notified first), no SLA.
- This is a demo setup, not production.

## Tested locally (MySQL 9.7.4, JDK 17, .NET 10)
- Java and .NET both run against MySQL, JWT issued by Java is accepted by .NET, reviews survive a .NET restart, CORS works, Java ran with a 256 MB heap at about 280 MB RSS.
- Not tested: Docker images, MySQL 8.x (Aiven's default may differ from 9.7; the code uses only standard JPA/EF features, but this is untested), TLS to Aiven, Render CPU limits.

## Steps (when approved)
1. Aiven: create a free MySQL service, then create two databases (Console > service > Databases > Create database; docs: https://aiven.io/docs/products/mysql/howto/create-database), e.g. `bmh_java` and `bmh_extras`. Use separate databases, because EF `EnsureCreated` will not create tables in a database that already has tables.
2. Render: two web services from this repo (Docker): `backend/` with Dockerfile.java, `dotnet-backend/BookMyHive.Api/` with Dockerfile.dotnet. Health check path: Java `/api/events`, .NET `/api/health`.
3. Env vars: see ENV-MATRIX.md. Secrets go in Render's environment settings only, never in the repo.
4. Vercel: import the repo, root `frontend`, set VITE_API_URL and VITE_EXTRAS_API_URL, then redeploy. Then set CORS origins on both APIs to the Vercel URL.

## Admin and seed data
- First start seeds demo events and two users. The local-dev passwords are public in the README, so on any deployment set SEED_ADMIN_PASSWORD and SEED_USER_PASSWORD BEFORE the first start (the seed only runs when the users table is empty). TESTED: with the overrides, the default admin password returns 401 and the new one returns 200.
- Seed demo users are optional; set SEED_ENABLED=false to skip the seed, but then there is no admin until one is inserted directly in the DB.
- If the DB was already seeded with defaults, change the passwords in the DB or recreate the database.
