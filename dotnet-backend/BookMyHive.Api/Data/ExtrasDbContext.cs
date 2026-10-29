using BookMyHive.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace BookMyHive.Api.Data;

public class ExtrasDbContext(DbContextOptions<ExtrasDbContext> options) : DbContext(options)
{
    public DbSet<Review> Reviews => Set<Review>();
    public DbSet<Comment> Comments => Set<Comment>();
    public DbSet<HeroBanner> HeroBanners => Set<HeroBanner>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<Review>(e =>
        {
            e.HasIndex(r => new { r.EventId, r.AuthorEmail }).IsUnique();
            e.Property(r => r.AuthorEmail).HasMaxLength(190);
            e.Property(r => r.AuthorName).HasMaxLength(80);
            e.Property(r => r.Title).HasMaxLength(120);
            e.Property(r => r.Body).HasMaxLength(2000);
        });
        modelBuilder.Entity<Comment>(e =>
        {
            e.HasIndex(c => new { c.EventId, c.CreatedAt });
            e.Property(c => c.AuthorEmail).HasMaxLength(190);
            e.Property(c => c.AuthorName).HasMaxLength(80);
            e.Property(c => c.Body).HasMaxLength(1000);
        });
        modelBuilder.Entity<HeroBanner>(e =>
        {
            e.Property(b => b.Title).HasMaxLength(120);
            e.Property(b => b.Subtitle).HasMaxLength(240);
            e.Property(b => b.ImageUrl).HasMaxLength(500);
            e.Property(b => b.CtaUrl).HasMaxLength(500);
        });
    }
}
