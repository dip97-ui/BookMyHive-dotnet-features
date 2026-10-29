using System.Text.Json.Serialization;

namespace BookMyHive.Api.Models;

/// <summary>A slide in the PVR-style auto-rotating hero carousel on the home page.</summary>
public class HeroBanner
{
    public long Id { get; set; }
    public long? EventId { get; set; }
    public required string Title { get; set; }
    public string? Subtitle { get; set; }
    public required string ImageUrl { get; set; }
    public string? CtaUrl { get; set; }
    public int SortOrder { get; set; }
    public bool IsActive { get; set; } = true;
}

/// <summary>A star-rated review of an event. One per user per event.</summary>
public class Review
{
    public long Id { get; set; }
    public long EventId { get; set; }

    /// <summary>Identity from the JWT subject (the user's login email). Never client-chosen, never serialized.</summary>
    [JsonIgnore]
    public string AuthorEmail { get; set; } = string.Empty;

    /// <summary>Display name shown next to the review (from the signed-in profile).</summary>
    public required string AuthorName { get; set; }

    /// <summary>1-5 stars.</summary>
    public int Rating { get; set; }
    public string? Title { get; set; }
    public required string Body { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}

/// <summary>One message in the open discussion thread under an event.</summary>
public class Comment
{
    public long Id { get; set; }
    public long EventId { get; set; }

    /// <summary>Identity from the JWT subject (the user's login email). Never client-chosen, never serialized.</summary>
    [JsonIgnore]
    public string AuthorEmail { get; set; } = string.Empty;
    public required string AuthorName { get; set; }
    public required string Body { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}

public class HeroBannerInput
{
    public long? EventId { get; set; }
    public string? Title { get; set; }
    public string? Subtitle { get; set; }
    public string? ImageUrl { get; set; }
    public string? CtaUrl { get; set; }
    public int SortOrder { get; set; }
    public bool IsActive { get; set; } = true;
}

public class ReviewInput
{
    public string? AuthorName { get; set; }
    public int Rating { get; set; }
    public string? Title { get; set; }
    public string? Body { get; set; }
}

public class CommentInput
{
    public string? AuthorName { get; set; }
    public string? Body { get; set; }
}

public record ReviewSummary(long EventId, double AverageRating, int Count);

/// <summary>Same error shape the Spring Boot backend returns ({message, fieldErrors}).</summary>
public record ErrorBody(string Message, Dictionary<string, string>? FieldErrors = null);
