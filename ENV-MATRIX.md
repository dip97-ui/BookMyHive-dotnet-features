Java API (Render Docker web service, free)
  SPRING_DATASOURCE_URL=jdbc:mysql://<aiven-host>:<port>/<javadb>?sslMode=REQUIRED&serverTimezone=UTC
  SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD  (Aiven)
  JWT_SECRET  (>=32 bytes, same value as .NET Jwt__Secret)
  TMDB_API_KEY  (user's own)
  CORS_ORIGINS=https://<vercel-app>.vercel.app
.NET API (Render Docker web service, free)
  Database__Provider=MySql
  ConnectionStrings__Extras=Server=<aiven-host>;Port=<port>;Database=<extrasdb>;User=...;Password=...;SslMode=Required
  Jwt__Secret=<same as JWT_SECRET>
  Cors__Origins__0=https://<vercel-app>.vercel.app
Frontend (Vercel build env)
  VITE_API_URL=https://<java>.onrender.com/api
  VITE_EXTRAS_API_URL=https://<dotnet>.onrender.com/api
Health: Java has no actuator; use GET /api/events (public, 200) as Render health path. .NET: GET /api/health.
Java extras (deploy): SEED_ADMIN_PASSWORD, SEED_USER_PASSWORD (set before first start), optional SEED_ENABLED=false
Aiven: two databases in one service (docs: https://aiven.io/docs/products/mysql/howto/create-database)
