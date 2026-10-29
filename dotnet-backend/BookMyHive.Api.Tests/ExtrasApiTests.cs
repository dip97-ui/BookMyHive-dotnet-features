using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;

namespace BookMyHive.Api.Tests;

public class ExtrasApiTests : IClassFixture<ExtrasApiFactory>
{
    private readonly ExtrasApiFactory _factory;

    public ExtrasApiTests(ExtrasApiFactory factory) => _factory = factory;

    private HttpClient ClientAs(string? email = null, string role = "USER")
    {
        var client = _factory.CreateClient();
        if (email is not null)
            client.DefaultRequestHeaders.Authorization =
                new AuthenticationHeaderValue("Bearer", _factory.TokenFor(email, role));
        return client;
    }

    [Fact]
    public async Task Health_is_anonymous_and_ok()
    {
        var res = await _factory.CreateClient().GetAsync("/api/health");
        Assert.Equal(HttpStatusCode.OK, res.StatusCode);
        var json = await res.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal("ok", json.GetProperty("status").GetString());
    }

    [Fact]
    public async Task Banners_list_is_anonymous()
    {
        var res = await _factory.CreateClient().GetAsync("/api/hero-banners");
        Assert.Equal(HttpStatusCode.OK, res.StatusCode);
    }

    [Fact]
    public async Task Posting_a_review_requires_a_token()
    {
        var res = await _factory.CreateClient().PostAsJsonAsync("/api/events/1001/reviews",
            new { authorName = "Dipesh", rating = 5, body = "Loved it" });
        Assert.Equal(HttpStatusCode.Unauthorized, res.StatusCode);
    }

    [Fact]
    public async Task Review_roundtrip_identity_comes_from_token_and_duplicate_is_conflict()
    {
        var client = ClientAs("dipesh@example.com");

        var created = await client.PostAsJsonAsync("/api/events/1002/reviews",
            new { authorName = "Dipesh Gupta", rating = 4, title = "Good", body = "Solid show, easy booking." });
        Assert.Equal(HttpStatusCode.Created, created.StatusCode);
        var review = await created.Content.ReadFromJsonAsync<JsonElement>();
        Assert.False(review.TryGetProperty("authorEmail", out _), "authorEmail must not be exposed");
        Assert.Equal("Dipesh Gupta", review.GetProperty("authorName").GetString());
        Assert.Equal(4, review.GetProperty("rating").GetInt32());

        var duplicate = await client.PostAsJsonAsync("/api/events/1002/reviews",
            new { authorName = "Dipesh Gupta", rating = 2, body = "Changed my mind" });
        Assert.Equal(HttpStatusCode.Conflict, duplicate.StatusCode);

        var summary = await _factory.CreateClient().GetFromJsonAsync<JsonElement>("/api/events/1002/reviews/summary");
        Assert.Equal(1, summary.GetProperty("count").GetInt32());
        Assert.Equal(4.0, summary.GetProperty("averageRating").GetDouble());

        // A different user (different token subject) can review the same event.
        var other = ClientAs("meera@example.com");
        var second = await other.PostAsJsonAsync("/api/events/1002/reviews",
            new { rating = 2, body = "Sound was too loud." });
        Assert.Equal(HttpStatusCode.Created, second.StatusCode);
        var secondJson = await second.Content.ReadFromJsonAsync<JsonElement>();
        // No display name supplied -> falls back to the email's local part.
        Assert.Equal("meera", secondJson.GetProperty("authorName").GetString());

        var summary2 = await _factory.CreateClient().GetFromJsonAsync<JsonElement>("/api/events/1002/reviews/summary");
        Assert.Equal(2, summary2.GetProperty("count").GetInt32());
        Assert.Equal(3.0, summary2.GetProperty("averageRating").GetDouble());
    }

    [Fact]
    public async Task Review_rating_out_of_range_is_a_field_error()
    {
        var client = ClientAs("dipesh@example.com");
        var res = await client.PostAsJsonAsync("/api/events/1003/reviews",
            new { rating = 7, body = "Too many stars" });
        Assert.Equal(HttpStatusCode.BadRequest, res.StatusCode);
        var err = await res.Content.ReadFromJsonAsync<JsonElement>();
        Assert.True(err.GetProperty("fieldErrors").TryGetProperty("rating", out _));
    }

    [Fact]
    public async Task Forged_token_with_wrong_secret_is_rejected()
    {
        var client = _factory.CreateClient();
        var forged = _factory.TokenFor("dipesh@example.com")[..^3] + "xxx";
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", forged);
        var res = await client.PostAsJsonAsync("/api/events/1004/reviews", new { rating = 5, body = "hi" });
        Assert.Equal(HttpStatusCode.Unauthorized, res.StatusCode);
    }

    [Fact]
    public async Task Comments_require_auth_and_roundtrip()
    {
        var anon = await _factory.CreateClient().PostAsJsonAsync("/api/events/1005/comments",
            new { body = "no token" });
        Assert.Equal(HttpStatusCode.Unauthorized, anon.StatusCode);

        var client = ClientAs("dipesh@example.com");
        var created = await client.PostAsJsonAsync("/api/events/1005/comments",
            new { authorName = "Dipesh Gupta", body = "Is parking available at this venue?" });
        Assert.Equal(HttpStatusCode.Created, created.StatusCode);

        var list = await _factory.CreateClient().GetFromJsonAsync<JsonElement[]>("/api/events/1005/comments");
        Assert.Single(list!);
        Assert.Equal("Is parking available at this venue?", list![0].GetProperty("body").GetString());
        Assert.False(list[0].TryGetProperty("authorEmail", out _), "authorEmail must not be exposed");
    }

    [Fact]
    public async Task Banner_management_is_admin_only()
    {
        var asUser = await ClientAs("dipesh@example.com", "USER").PostAsJsonAsync("/api/hero-banners",
            new { title = "Big release", imageUrl = "https://img.example.com/banner.jpg", sortOrder = 1 });
        Assert.Equal(HttpStatusCode.Forbidden, asUser.StatusCode);

        var asAdmin = await ClientAs("admin@bookmyhive.com", "ADMIN").PostAsJsonAsync("/api/hero-banners",
            new { title = "Big release", imageUrl = "https://img.example.com/banner.jpg", sortOrder = 1, eventId = 42, ctaUrl = "/events/42" });
        Assert.Equal(HttpStatusCode.Created, asAdmin.StatusCode);

        var banners = await _factory.CreateClient().GetFromJsonAsync<JsonElement[]>("/api/hero-banners");
        Assert.Single(banners!);
        Assert.Equal("Big release", banners![0].GetProperty("title").GetString());
    }
}
