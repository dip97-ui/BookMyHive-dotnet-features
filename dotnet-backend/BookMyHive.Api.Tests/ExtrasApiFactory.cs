using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using Microsoft.IdentityModel.JsonWebTokens;
using Microsoft.IdentityModel.Tokens;

namespace BookMyHive.Api.Tests;

/// <summary>
/// Hosts the API in-memory with a throwaway SQLite file and a test JWT secret.
/// Tokens are minted exactly like the Spring Boot backend does: HS256, sub = email, role claim.
/// </summary>
public class ExtrasApiFactory : WebApplicationFactory<Program>
{
    public const string TestSecret = "test-secret-test-secret-test-secret-test-secret-0123456789abcd";
    private readonly string _dbPath = Path.Combine(Path.GetTempPath(), $"bmh-extras-test-{Guid.NewGuid():N}.db");

    // Set before the host is built so Program.cs startup reads these values:
    // environment variables are part of the app's default configuration.
    public ExtrasApiFactory()
    {
        Environment.SetEnvironmentVariable("ConnectionStrings__Extras", $"Data Source={_dbPath}");
        Environment.SetEnvironmentVariable("Database__Provider", "Sqlite");
        Environment.SetEnvironmentVariable("Jwt__Secret", TestSecret);
    }

    // JsonWebTokenHandler writes claims verbatim (no outbound XML-name mapping),
    // which is exactly how the Java JJWT library emits sub/role.
    public string TokenFor(string email, string role = "USER")
    {
        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(TestSecret));
        var descriptor = new SecurityTokenDescriptor
        {
            Claims = new Dictionary<string, object>
            {
                ["sub"] = email,
                ["role"] = role,
                ["iat"] = DateTimeOffset.UtcNow.ToUnixTimeSeconds()
            },
            Expires = DateTime.UtcNow.AddHours(1),
            SigningCredentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256)
        };
        return new JsonWebTokenHandler().CreateToken(descriptor);
    }
}
