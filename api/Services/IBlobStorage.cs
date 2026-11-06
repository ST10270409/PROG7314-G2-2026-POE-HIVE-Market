namespace HiveMarketApi.Services;

/// <summary>Blob (file) storage for listing photos. Returns a public HTTPS URL.</summary>
public interface IBlobStorage
{
    Task<string> UploadAsync(Stream content, string contentType, string extension, CancellationToken ct = default);
}

/// <summary>Supabase Storage (S3-compatible object storage) via its REST API.</summary>
public class SupabaseBlobStorage : IBlobStorage
{
    private readonly HttpClient _http;
    private readonly string _baseUrl;
    private readonly string _bucket;
    private readonly string _key;

    public SupabaseBlobStorage(HttpClient http, string baseUrl, string serviceKey, string bucket)
    {
        _http = http;
        _baseUrl = baseUrl.TrimEnd('/');
        _key = serviceKey;
        _bucket = bucket;
    }

    public async Task<string> UploadAsync(Stream content, string contentType, string extension, CancellationToken ct = default)
    {
        var objectPath = $"{DateTime.UtcNow:yyyy/MM}/{Guid.NewGuid():N}.{extension}";
        using var request = new HttpRequestMessage(HttpMethod.Post, $"{_baseUrl}/storage/v1/object/{_bucket}/{objectPath}")
        {
            Content = new StreamContent(content)
        };
        request.Content.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue(contentType);
        request.Headers.Add("apikey", _key);
        if (_key.StartsWith("eyJ"))
            request.Headers.Authorization = new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", _key);

        using var response = await _http.SendAsync(request, ct);
        if (!response.IsSuccessStatusCode)
        {
            var body = await response.Content.ReadAsStringAsync(ct);
            throw new InvalidOperationException($"Blob upload failed ({(int)response.StatusCode}): {body}");
        }
        return $"{_baseUrl}/storage/v1/object/public/{_bucket}/{objectPath}";
    }
}

/// <summary>Registered when SUPABASE_* settings are missing: uploads answer 503 instead of crashing.</summary>
public class NotConfiguredBlobStorage : IBlobStorage
{
    public Task<string> UploadAsync(Stream content, string contentType, string extension, CancellationToken ct = default)
        => throw new InvalidOperationException("Blob storage is not configured (set SUPABASE_URL, SUPABASE_SERVICE_KEY, SUPABASE_BUCKET).");
}
