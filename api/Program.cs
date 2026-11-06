using System.Text.Json;
using HiveMarketApi.Data;
using FirebaseAdmin;
using Google.Apis.Auth.OAuth2;
using Google.Cloud.Firestore;
using HiveMarketApi.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

var renderPort = Environment.GetEnvironmentVariable("PORT") ?? "8080";
builder.WebHost.UseUrls($"http://0.0.0.0:{renderPort}");

var firebaseProjectId = builder.Configuration["FIREBASE_PROJECT_ID"] ?? "hivemarket-prototype";

// ---- Database ----
// Render's "Internal Database URL" is a URI-style string
// (postgresql://user:pass@host/dbname) — Npgsql does NOT accept that
// format directly; it expects classic keyword=value pairs
// (Host=...;Username=...;Password=...). ConvertRenderUrlToNpgsql below
// does that translation. Skipping this step is exactly what caused:
//   "System.ArgumentException: Format of the initialization string does
//    not conform to specification starting at index 0."
var rawConnectionString = builder.Configuration["DATABASE_URL"];
builder.Services.AddDbContext<AppDbContext>(options =>
{
    if (!string.IsNullOrEmpty(rawConnectionString))
    {
        options.UseNpgsql(ConvertRenderUrlToNpgsql(rawConnectionString));
    }
    else
    {
        options.UseSqlite($"Data Source={builder.Configuration["SQLITE_PATH"] ?? "hivemarket.dev.db"}");
    }
});

// ---- Blob storage (Supabase Storage) ----
var supabaseUrl = builder.Configuration["SUPABASE_URL"];
var supabaseKey = builder.Configuration["SUPABASE_SERVICE_KEY"];
var supabaseBucket = builder.Configuration["SUPABASE_BUCKET"] ?? "listing-images";
if (!string.IsNullOrWhiteSpace(supabaseUrl) && !string.IsNullOrWhiteSpace(supabaseKey))
{
    builder.Services.AddHttpClient<IBlobStorage, SupabaseBlobStorage>((http, _) =>
        new SupabaseBlobStorage(http, supabaseUrl, supabaseKey, supabaseBucket));
}
else
{
    builder.Services.AddSingleton<IBlobStorage, NotConfiguredBlobStorage>();
}

// ---- Firebase Admin: Cloud Messaging (push) + Firestore (NoSQL inbox) ----
// FIREBASE_SERVICE_ACCOUNT_JSON = the full service-account key JSON (Render env var).
var serviceAccountJson = builder.Configuration["FIREBASE_SERVICE_ACCOUNT_JSON"];
if (!string.IsNullOrWhiteSpace(serviceAccountJson))
{
    FirebaseApp.Create(new AppOptions { Credential = GoogleCredential.FromJson(serviceAccountJson) });
    builder.Services.AddSingleton(_ => new FirestoreDbBuilder
    {
        ProjectId = firebaseProjectId,
        JsonCredentials = serviceAccountJson
    }.Build());
    builder.Services.AddSingleton<INotificationService, FirebaseNotificationService>();
}
else
{
    builder.Services.AddSingleton<INotificationService, NoOpNotificationService>();
}

builder.Services.AddHttpContextAccessor();
builder.Services.AddScoped<CurrentUserService>();
builder.Services.AddControllers().AddJsonOptions(o =>
{
    o.JsonSerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
});

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

builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy => policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader());
});

var app = builder.Build();

using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    db.Database.EnsureCreated();
}

app.UseCors();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.MapGet("/", () => Results.Ok(new { status = "HiveMarket API is running" }));

app.Run();

// Converts postgresql://user:password@host:port/dbname (Render's format)
// into Host=...;Port=...;Database=...;Username=...;Password=...;
// (Npgsql's format). SSL Mode=Prefer works whether or not Render's
// internal network requires SSL for this connection.
static string ConvertRenderUrlToNpgsql(string url)
{
    var uri = new Uri(url);
    var userInfo = uri.UserInfo.Split(':');
    var username = Uri.UnescapeDataString(userInfo[0]);
    var password = userInfo.Length > 1 ? Uri.UnescapeDataString(userInfo[1]) : "";
    var database = uri.AbsolutePath.TrimStart('/');
    var port = uri.Port == -1 ? 5432 : uri.Port;

    return $"Host={uri.Host};Port={port};Database={database};Username={username};Password={password};SSL Mode=Prefer;Trust Server Certificate=true";
}

// Lets the integration tests (api.Tests) boot the real app in memory.
public partial class Program { }
