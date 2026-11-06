using System.Text.Json.Serialization;
using HiveMarketApi.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace HiveMarketApi.Controllers;

public class DeviceRequestDto
{
    [JsonPropertyName("token")] public string Token { get; set; } = "";
}

[ApiController]
[Authorize]
[Route("api/devices")]
public class DevicesController : ControllerBase
{
    private readonly CurrentUserService _currentUser;
    private readonly INotificationService _notifications;

    public DevicesController(CurrentUserService currentUser, INotificationService notifications)
    {
        _currentUser = currentUser;
        _notifications = notifications;
    }

    // POST /api/devices — links this phone's FCM token to the signed-in user.
    [HttpPost]
    public async Task<ActionResult<Models.UpdatedResponseDto>> Register([FromBody] DeviceRequestDto body)
    {
        if (string.IsNullOrWhiteSpace(body.Token) || body.Token.Length > 4096 || body.Token.Contains('/'))
            return BadRequest(new { error = "Invalid token." });
        var user = await _currentUser.GetOrCreateCurrentUserAsync();
        await _notifications.RegisterDeviceAsync(user.FirebaseUid, body.Token);
        return Ok(new Models.UpdatedResponseDto { Updated = true });
    }

    // DELETE /api/devices — called on sign-out so pushes stop reaching this phone.
    [HttpDelete]
    public async Task<ActionResult<Models.UpdatedResponseDto>> Unregister([FromBody] DeviceRequestDto body)
    {
        var user = await _currentUser.GetOrCreateCurrentUserAsync();
        await _notifications.UnregisterDeviceAsync(user.FirebaseUid, body.Token);
        return Ok(new Models.UpdatedResponseDto { Updated = true });
    }
}
