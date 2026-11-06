using HiveMarketApi.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace HiveMarketApi.Controllers;

public class UploadResponseDto
{
    [System.Text.Json.Serialization.JsonPropertyName("url")] public string Url { get; set; } = "";
}

[ApiController]
[Authorize]
[Route("api/uploads")]
public class UploadsController : ControllerBase
{
    public const long MaxBytes = 5 * 1024 * 1024;
    private readonly IBlobStorage _blobs;
    private readonly ILogger<UploadsController> _log;

    public UploadsController(IBlobStorage blobs, ILogger<UploadsController> log)
    {
        _blobs = blobs;
        _log = log;
    }

    // POST /api/uploads/images — multipart form, field "file". Stores the photo in
    // blob storage and returns its public URL, which the client puts on the listing.
    [HttpPost("images")]
    [RequestSizeLimit(MaxBytes + 1024 * 64)]
    public async Task<ActionResult<UploadResponseDto>> UploadImage(IFormFile? file, CancellationToken ct)
    {
        if (file == null || file.Length == 0) return BadRequest(new { error = "No file uploaded." });
        if (file.Length > MaxBytes) return BadRequest(new { error = "Image must be 5 MB or smaller." });

        // Never trust the client's Content-Type: sniff the real file signature.
        await using var stream = file.OpenReadStream();
        var header = new byte[12];
        var read = await stream.ReadAsync(header.AsMemory(0, header.Length), ct);
        var kind = DetectImageKind(header, read);
        if (kind == null) return BadRequest(new { error = "Only JPEG, PNG or WebP images are allowed." });
        stream.Position = 0;

        try
        {
            var url = await _blobs.UploadAsync(stream, kind.Value.contentType, kind.Value.extension, ct);
            return Ok(new UploadResponseDto { Url = url });
        }
        catch (Exception ex)
        {
            _log.LogError(ex, "Image upload failed");
            return StatusCode(503, new { error = "Image storage is unavailable right now." });
        }
    }

    public static (string contentType, string extension)? DetectImageKind(byte[] h, int length)
    {
        if (length >= 3 && h[0] == 0xFF && h[1] == 0xD8 && h[2] == 0xFF) return ("image/jpeg", "jpg");
        if (length >= 8 && h[0] == 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47) return ("image/png", "png");
        if (length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return ("image/webp", "webp");
        return null;
    }
}
