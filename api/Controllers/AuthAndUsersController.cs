using HiveMarketApi.Data;
using HiveMarketApi.Models;
using HiveMarketApi.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace HiveMarketApi.Controllers;

[ApiController]
[Authorize]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly CurrentUserService _currentUser;
    private readonly AppDbContext _db;

    public AuthController(CurrentUserService currentUser, AppDbContext db)
    {
        _currentUser = currentUser;
        _db = db;
    }

    // POST /api/auth/firebase — matches HiveMarketApi.kt's exchangeFirebaseToken().
    // The token itself is validated by JwtBearer middleware before this
    // action ever runs (see Program.cs); by the time we're here, User is
    // already known-authentic. This endpoint's only job is to return the
    // int userID the rest of the API expects, creating the row if this is
    // the account's first request.
    [HttpPost("firebase")]
    public async Task<ActionResult<AuthResponseDto>> ExchangeFirebaseToken()
    {
        var countBefore = await _db.Users.CountAsync();
        var user = await _currentUser.GetOrCreateCurrentUserAsync();
        var countAfter = await _db.Users.CountAsync();

        return Ok(new AuthResponseDto
        {
            UserID = user.UserID,
            Name = user.Name,
            Email = user.Email,
            TrustScore = user.TrustScore,
            IsNewUser = countAfter > countBefore
        });
    }
}

[ApiController]
[Authorize]
[Route("api/users")]
public class UsersController : ControllerBase
{
    private readonly CurrentUserService _currentUser;
    private readonly AppDbContext _db;

    public UsersController(CurrentUserService currentUser, AppDbContext db)
    {
        _currentUser = currentUser;
        _db = db;
    }

    [HttpGet("me")]
    public async Task<ActionResult<UserDto>> GetMyProfile()
    {
        var user = await _currentUser.GetOrCreateCurrentUserAsync();
        return Ok(new UserDto
        {
            UserID = user.UserID, Name = user.Name, Surname = user.Surname, Username = user.Username,
            Email = user.Email, StudentNumber = user.StudentNumber, Campus = user.Campus, TrustScore = user.TrustScore
        });
    }

    [HttpPatch("me")]
    public async Task<ActionResult<UpdatedResponseDto>> UpdateMyProfile([FromBody] UpdateProfileRequestDto body)
    {
        var user = await _currentUser.GetOrCreateCurrentUserAsync();
        if (body.Name != null) user.Name = body.Name;
        if (body.Surname != null) user.Surname = body.Surname;
        if (body.Username != null) user.Username = body.Username;
        await _db.SaveChangesAsync();
        return Ok(new UpdatedResponseDto { Updated = true });
    }
}

[ApiController]
[Authorize]
[Route("api/settings")]
public class SettingsController : ControllerBase
{
    private readonly CurrentUserService _currentUser;
    private readonly AppDbContext _db;

    public SettingsController(CurrentUserService currentUser, AppDbContext db)
    {
        _currentUser = currentUser;
        _db = db;
    }

    // PATCH /api/settings — backs the Settings screen (FR2). Same
    // offline-first client behaviour applies regardless of what this
    // returns: SettingsViewModel already writes locally first and treats
    // this as best-effort background sync.
    [HttpPatch]
    public async Task<ActionResult<UpdatedResponseDto>> UpdateSettings([FromBody] SettingsRequestDto body)
    {
        var user = await _currentUser.GetOrCreateCurrentUserAsync();
        if (body.Language != null) user.Language = body.Language;
        if (body.NotificationsEnabled.HasValue) user.NotificationsEnabled = body.NotificationsEnabled.Value;
        if (body.BiometricEnabled.HasValue) user.BiometricEnabled = body.BiometricEnabled.Value;
        await _db.SaveChangesAsync();
        return Ok(new UpdatedResponseDto { Updated = true });
    }
}
