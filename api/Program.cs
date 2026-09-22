using System.Text.Json;
using HiveMarketApi.Data;
using HiveMarketApi.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

// Render assigns a dynamic port via the PORT environment variable and
// expects the container to listen on it — without this, ASP.NET Core
// defaults to 8080/5000 regardless of what Render actually tells it to
// use, and Render's health check would fail to ever see the service come up.
var renderPort = Environment.GetEnvironmentVariable("PORT") ?? "8080";
builder.WebHost.UseUrls($"http://0.0.0.0:{renderPort}");

// ---- Firebase project ID ----
// Read from the FIREBASE_PROJECT_ID environment variable if set (configure
// this in Render's dashboard under your service's Environment tab),
// otherwise falls back to the ID seen in this project's own Firebase JWTs
// during testing. If your group creates a different Firebase project
// later, set the environment variable rather than editing this file.
var firebaseProjectId = builder.Configuration["FIREBASE_PROJECT_ID"] ?? "hivemarket-prototype";

// ---- Database ----
// Render: create a free PostgreSQL instance (New -> PostgreSQL), then copy
// its "Internal Database URL" into this Web Service's environment as
// DATABASE_URL. Falls back to a local SQLite file for running this
// locally without any Postgres setup.
var connectionString = builder.Configuration["DATABASE_URL"];
builder.Services.AddDbContext<AppDbContext>(options =>
{
    if (!string.IsNullOrEmpty(connectionString))
    {
        options.UseNpgsql(connectionString);
    }
    else
    {
        options.UseSqlite("Data Source=hivemarket.dev.db");
    }
});

builder.Services.AddHttpContextAccessor();
builder.Services.AddScoped<CurrentUserService>();
builder.Services.AddControllers().AddJsonOptions(o =>
{
    // Every DTO already declares its own [JsonPropertyName] explicitly
    // (see Models/Entities.cs) — this just prevents any *unannotated*
    // property from accidentally being serialized in PascalCase (the C#
    // default) instead of failing loudly, which would silently break the
    // Android client's deserialization.
    o.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
});

// ---- Authentication ----
// Validates Firebase-issued ID tokens using ASP.NET Core's built-in JWT
// Bearer middleware pointed at Firebase's own public key endpoint — this
// deliberately avoids needing the Firebase Admin SDK or a downloaded
// service-account JSON file, which would be one more secret to manage
// under tonight's time pressure. The tradeoff: this validates the token's
// signature and claims, but does not check token revocation the way the
// full Admin SDK would — acceptable for a Part 2 prototype, worth
// upgrading before any real production use.
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.Authority = $"https://securetoken.google.com/{firebaseProjectId}";
        options.TokenValidationParameters = new Microsoft.IdentityModel.Tokens.TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = $"https://securetoken.google.com/{firebaseProjectId}",
            ValidateAudience = true,
            ValidAudience = firebaseProjectId,
            ValidateLifetime = true
        };
    });
builder.Services.AddAuthorization();

// ---- CORS ----
// Wide open on purpose for a student prototype reachable only via the
// Android app (no browser origin to restrict to). Tighten before any real
// deployment.
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy => policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader());
});

var app = builder.Build();

// Creates the database schema directly from the current model, with no
// EF Core migration files needed — deliberately simpler than
// Database.Migrate() for tonight, since generating real migration files
// requires the `dotnet ef` CLI tool, which needs a full local .NET SDK
// install to run. Tradeoff: EnsureCreated doesn't support incrementally
// evolving the schema later the way real migrations would — if the group
// changes an entity after tonight, the cleanest fix is dropping and
// letting EnsureCreated rebuild it (acceptable for a prototype with no
// production data to lose), or switching to real migrations once someone
// has a local .NET SDK to generate them.
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    db.Database.EnsureCreated();
}

app.UseCors();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

// Basic liveness check — hit this URL directly in a browser to confirm
// the service deployed successfully, before testing anything that needs
// authentication.
app.MapGet("/", () => Results.Ok(new { status = "HiveMarket API is running" }));

app.Run();
