using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using BookMyHive.Api.Data;
using BookMyHive.Api.Models;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddOpenApi();

// Database: SQLite for local development; the same EF Core model runs on
// MySQL 8 in deployment by switching Database:Provider + ConnectionStrings:Extras.
var provider = builder.Configuration["Database:Provider"] ?? "Sqlite";
var extrasConn = builder.Configuration.GetConnectionString("Extras") ?? "Data Source=bookmyhive-extras.db";
builder.Services.AddDbContext<ExtrasDbContext>(opt =>
{
    if (provider.Equals("MySql", StringComparison.OrdinalIgnoreCase))
        opt.UseMySQL(extrasConn);
    else
        opt.UseSqlite(extrasConn);
});

// Authentication: validates the SAME HS256 access tokens the Spring Boot backend
// issues at /api/auth/login (subject = user email, "role" claim = USER/ADMIN).
// This is the shared-auth integration: a user logs in once on the existing app
// and this service accepts that token, so no second login is ever needed.
var jwtSecret = builder.Configuration["Jwt:Secret"]
    ?? throw new InvalidOperationException("Jwt:Secret is not configured. Set the Jwt__Secret environment variable to the same secret the Java backend uses (its JWT_SECRET).");
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(o =>
    {
        o.MapInboundClaims = false;
        o.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = false,
            ValidateAudience = false,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtSecret)),
            NameClaimType = JwtRegisteredClaimNames.Sub,
            RoleClaimType = "role",
            ClockSkew = TimeSpan.FromMinutes(1)
        };
    });
builder.Services.AddAuthorization();

var corsOrigins = builder.Configuration.GetSection("Cors:Origins").Get<string[]>() ?? ["http://localhost:5173"];
builder.Services.AddCors(o => o.AddDefaultPolicy(p => p.WithOrigins(corsOrigins).AllowAnyHeader().AllowAnyMethod()));

var app = builder.Build();

using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<ExtrasDbContext>();
    db.Database.EnsureCreated();
}

if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
}

app.UseCors();
app.UseAuthentication();
app.UseAuthorization();

var api = app.MapGroup("/api");

api.MapGet("/health", () => Results.Ok(new { status = "ok", service = "bookmyhive-extras", database = provider }));

// Hero banners: the data behind the PVR-style auto-sliding hero carousel.
api.MapGet("/hero-banners", ListActiveBanners);
api.MapPost("/hero-banners", CreateBanner).RequireAuthorization(p => p.RequireRole("ADMIN"));
api.MapDelete("/hero-banners/{id:long}", DeleteBanner).RequireAuthorization(p => p.RequireRole("ADMIN"));

// Reviews: one per signed-in user per event, 1-5 stars, plus a summary for star displays.
api.MapGet("/events/{eventId:long}/reviews", ListReviews);
api.MapGet("/events/{eventId:long}/reviews/summary", GetReviewSummary);
api.MapPost("/events/{eventId:long}/reviews", PostReview).RequireAuthorization();
api.MapDelete("/reviews/{id:long}", DeleteReview).RequireAuthorization(p => p.RequireRole("ADMIN"));

// Comments: open discussion thread per event for signed-in users.
api.MapGet("/events/{eventId:long}/comments", ListComments);
api.MapPost("/events/{eventId:long}/comments", PostComment).RequireAuthorization();
api.MapDelete("/comments/{id:long}", DeleteComment).RequireAuthorization(p => p.RequireRole("ADMIN"));

app.Run();

// ---------------- Hero banner handlers ----------------

static async Task<IResult> ListActiveBanners(ExtrasDbContext db) =>
    Results.Ok(await db.HeroBanners
        .Where(b => b.IsActive)
        .OrderBy(b => b.SortOrder)
        .ThenBy(b => b.Id)
        .AsNoTracking()
        .ToListAsync());

static async Task<IResult> CreateBanner(HeroBannerInput input, ExtrasDbContext db)
{
    var errors = ValidateBanner(input);
    if (errors.Count > 0) return Results.BadRequest(new ErrorBody("Validation failed", errors));

    var banner = new HeroBanner
    {
        EventId = input.EventId,
        Title = input.Title!.Trim(),
        Subtitle = input.Subtitle?.Trim(),
        ImageUrl = input.ImageUrl!.Trim(),
        CtaUrl = input.CtaUrl?.Trim(),
        SortOrder = input.SortOrder,
        IsActive = input.IsActive
    };
    db.HeroBanners.Add(banner);
    await db.SaveChangesAsync();
    return Results.Created($"/api/hero-banners/{banner.Id}", banner);
}

static async Task<IResult> DeleteBanner(long id, ExtrasDbContext db)
{
    var banner = await db.HeroBanners.FindAsync(id);
    if (banner is null) return Results.NotFound(new ErrorBody("Banner not found"));
    db.HeroBanners.Remove(banner);
    await db.SaveChangesAsync();
    return Results.NoContent();
}

// ---------------- Review handlers ----------------

static async Task<IResult> ListReviews(long eventId, ExtrasDbContext db) =>
    Results.Ok(await db.Reviews
        .Where(r => r.EventId == eventId)
        .OrderByDescending(r => r.CreatedAt)
        .AsNoTracking()
        .ToListAsync());

static async Task<IResult> GetReviewSummary(long eventId, ExtrasDbContext db)
{
    var query = db.Reviews.Where(r => r.EventId == eventId);
    var count = await query.CountAsync();
    var average = count == 0 ? 0.0 : await query.AverageAsync(r => (double)r.Rating);
    return Results.Ok(new ReviewSummary(eventId, Math.Round(average, 1), count));
}

static async Task<IResult> PostReview(long eventId, ReviewInput input, ExtrasDbContext db, ClaimsPrincipal user)
{
    // Identity always comes from the validated token, never from the request body.
    var email = user.Identity?.Name;
    if (string.IsNullOrEmpty(email)) return Results.Unauthorized();

    var errors = ValidateReview(input);
    if (errors.Count > 0) return Results.BadRequest(new ErrorBody("Validation failed", errors));

    var alreadyReviewed = await db.Reviews.AnyAsync(r => r.EventId == eventId && r.AuthorEmail == email);
    if (alreadyReviewed) return Results.Conflict(new ErrorBody("You have already reviewed this event."));

    var review = new Review
    {
        EventId = eventId,
        AuthorEmail = email,
        AuthorName = DisplayName(input.AuthorName, email),
        Rating = input.Rating,
        Title = input.Title?.Trim(),
        Body = input.Body!.Trim(),
        CreatedAt = DateTime.UtcNow
    };
    db.Reviews.Add(review);
    await db.SaveChangesAsync();
    return Results.Created($"/api/events/{eventId}/reviews/{review.Id}", review);
}

static async Task<IResult> DeleteReview(long id, ExtrasDbContext db)
{
    var review = await db.Reviews.FindAsync(id);
    if (review is null) return Results.NotFound(new ErrorBody("Review not found"));
    db.Reviews.Remove(review);
    await db.SaveChangesAsync();
    return Results.NoContent();
}

// ---------------- Comment handlers ----------------

static async Task<IResult> ListComments(long eventId, ExtrasDbContext db) =>
    Results.Ok(await db.Comments
        .Where(c => c.EventId == eventId)
        .OrderByDescending(c => c.CreatedAt)
        .AsNoTracking()
        .ToListAsync());

static async Task<IResult> PostComment(long eventId, CommentInput input, ExtrasDbContext db, ClaimsPrincipal user)
{
    var email = user.Identity?.Name;
    if (string.IsNullOrEmpty(email)) return Results.Unauthorized();

    var errors = ValidateComment(input);
    if (errors.Count > 0) return Results.BadRequest(new ErrorBody("Validation failed", errors));

    var comment = new Comment
    {
        EventId = eventId,
        AuthorEmail = email,
        AuthorName = DisplayName(input.AuthorName, email),
        Body = input.Body!.Trim(),
        CreatedAt = DateTime.UtcNow
    };
    db.Comments.Add(comment);
    await db.SaveChangesAsync();
    return Results.Created($"/api/events/{eventId}/comments/{comment.Id}", comment);
}

static async Task<IResult> DeleteComment(long id, ExtrasDbContext db)
{
    var comment = await db.Comments.FindAsync(id);
    if (comment is null) return Results.NotFound(new ErrorBody("Comment not found"));
    db.Comments.Remove(comment);
    await db.SaveChangesAsync();
    return Results.NoContent();
}

// ---------------- validation + helpers ----------------

static Dictionary<string, string> ValidateReview(ReviewInput input)
{
    var errors = new Dictionary<string, string>();
    if (input.Rating < 1 || input.Rating > 5) errors["rating"] = "Rating must be between 1 and 5.";
    if (string.IsNullOrWhiteSpace(input.Body)) errors["body"] = "Review text is required.";
    else if (input.Body.Trim().Length > 2000) errors["body"] = "Review text must be 2000 characters or fewer.";
    if (input.Title is { Length: > 120 }) errors["title"] = "Title must be 120 characters or fewer.";
    if (input.AuthorName is { Length: > 80 }) errors["authorName"] = "Name must be 80 characters or fewer.";
    return errors;
}

static Dictionary<string, string> ValidateComment(CommentInput input)
{
    var errors = new Dictionary<string, string>();
    if (string.IsNullOrWhiteSpace(input.Body)) errors["body"] = "Comment text is required.";
    else if (input.Body.Trim().Length > 1000) errors["body"] = "Comment text must be 1000 characters or fewer.";
    if (input.AuthorName is { Length: > 80 }) errors["authorName"] = "Name must be 80 characters or fewer.";
    return errors;
}

static Dictionary<string, string> ValidateBanner(HeroBannerInput input)
{
    var errors = new Dictionary<string, string>();
    if (string.IsNullOrWhiteSpace(input.Title)) errors["title"] = "Title is required.";
    else if (input.Title.Trim().Length > 120) errors["title"] = "Title must be 120 characters or fewer.";
    if (string.IsNullOrWhiteSpace(input.ImageUrl)) errors["imageUrl"] = "Image URL is required.";
    else if (input.ImageUrl.Trim().Length > 500) errors["imageUrl"] = "Image URL must be 500 characters or fewer.";
    if (input.Subtitle is { Length: > 240 }) errors["subtitle"] = "Subtitle must be 240 characters or fewer.";
    return errors;
}

/// <summary>Display name next to a post: the signed-in profile name, else the email's local part.</summary>
static string DisplayName(string? suppliedName, string email)
{
    if (!string.IsNullOrWhiteSpace(suppliedName)) return suppliedName.Trim();
    var at = email.IndexOf('@');
    return at > 0 ? email[..at] : email;
}

// Lets the integration-test host reference this top-level Program class.
public partial class Program;
