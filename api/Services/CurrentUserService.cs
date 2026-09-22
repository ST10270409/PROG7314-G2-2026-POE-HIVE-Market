using HiveMarketApi.Data;
using HiveMarketApi.Models;
using Microsoft.EntityFrameworkCore;

namespace HiveMarketApi.Services;

/// <summary>
/// Every protected endpoint needs an int userID (sellerID, buyerID,
/// senderID) but the JWT only proves a Firebase UID (a string) and an
/// email. This service is the one place that maps "authenticated Firebase
/// user" -> "our own int userID", creating the User row on first contact
/// so a student never has to explicitly "register" with this API — their
/// first authenticated request IS their registration.
/// </summary>
public class CurrentUserService
{
    private readonly AppDbContext _db;
    private readonly IHttpContextAccessor _httpContextAccessor;

    public CurrentUserService(AppDbContext db, IHttpContextAccessor httpContextAccessor)
    {
        _db = db;
        _httpContextAccessor = httpContextAccessor;
    }

    public async Task<UserEntity> GetOrCreateCurrentUserAsync()
    {
        var user = _httpContextAccessor.HttpContext?.User
            ?? throw new InvalidOperationException("No HttpContext available");

        // Firebase-issued tokens carry the UID in the standard "sub" claim.
        var firebaseUid = user.FindFirst("sub")?.Value ?? user.FindFirst("user_id")?.Value
            ?? throw new UnauthorizedAccessException("Token missing subject claim");
        var email = user.FindFirst("email")?.Value ?? "";

        var existing = await _db.Users.FirstOrDefaultAsync(u => u.FirebaseUid == firebaseUid);
        if (existing != null) return existing;

        // First request from this Firebase account — create the row.
        // Name/surname/studentNumber start blank; PATCH /api/users/me fills
        // them in once Edit Profile (not built this milestone) exists.
        var studentNumberGuess = email.Contains('@') ? email.Split('@')[0].ToUpperInvariant() : "";
        var created = new UserEntity
        {
            FirebaseUid = firebaseUid,
            Email = email,
            Name = "",
            Surname = "",
            Username = email.Contains('@') ? email.Split('@')[0] : firebaseUid,
            StudentNumber = studentNumberGuess,
            TrustScore = 0
        };
        _db.Users.Add(created);
        await _db.SaveChangesAsync();
        return created;
    }
}
