using System.Text.Json.Serialization;
using HiveMarketApi.Data;
using HiveMarketApi.Models;
using HiveMarketApi.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace HiveMarketApi.Controllers;

public class OfferDecisionRequestDto
{
    [JsonPropertyName("decision")] public string Decision { get; set; } = "";
}

public class OffersResponseDto
{
    [JsonPropertyName("items")] public List<OfferDto> Items { get; set; } = new();
}

[ApiController]
[Authorize]
[Route("api/offers")]
public class OffersController : ControllerBase
{
    private readonly CurrentUserService _currentUser;
    private readonly AppDbContext _db;
    private readonly INotificationService _notifications;

    public OffersController(CurrentUserService currentUser, AppDbContext db, INotificationService notifications)
    {
        _currentUser = currentUser;
        _db = db;
        _notifications = notifications;
    }

    // PATCH /api/offers/{offerID} — the SELLER accepts or declines an offer.
    // Accepting reserves the listing; either way the buyer is notified.
    [HttpPatch("{offerID:int}")]
    public async Task<ActionResult<OfferDto>> Respond(int offerID, [FromBody] OfferDecisionRequestDto body)
    {
        if (body.Decision != "Accepted" && body.Decision != "Rejected")
            return BadRequest(new { error = "decision must be 'Accepted' or 'Rejected'." });

        var offer = await _db.Offers.FirstOrDefaultAsync(o => o.OfferID == offerID);
        if (offer == null) return NotFound();
        var listing = await _db.Listings.FirstOrDefaultAsync(l => l.ListingID == offer.ListingID);
        if (listing == null) return NotFound();

        var me = await _currentUser.GetOrCreateCurrentUserAsync();
        if (listing.SellerID != me.UserID) return Forbid(); // only the seller decides
        if (offer.Status != "Pending") return Conflict(new { error = "This offer has already been answered." });
        if (body.Decision == "Accepted" && listing.Status != "Active")
            return Conflict(new { error = "This listing is no longer available." });

        offer.Status = body.Decision;
        if (body.Decision == "Accepted") listing.Status = "Reserved";
        await _db.SaveChangesAsync();

        var buyer = await _db.Users.FirstOrDefaultAsync(u => u.UserID == offer.BuyerID);
        if (buyer != null)
        {
            await _notifications.NotifyAsync(buyer,
                body.Decision == "Accepted" ? NotificationTypes.OfferAccepted : NotificationTypes.OfferRejected,
                UserNames.Display(me), offer.Amount.ToString("0"), listing.Title);
        }

        return Ok(new OfferDto
        {
            OfferID = offer.OfferID, ListingID = offer.ListingID, BuyerID = offer.BuyerID, Amount = offer.Amount,
            Message = offer.Message, Status = offer.Status, DateOffered = offer.DateOffered.ToString("O"),
            BuyerName = buyer != null ? UserNames.Display(buyer) : null
        });
    }
}

public static class UserNames
{
    public static string Display(UserEntity u)
    {
        var full = $"{u.Name} {u.Surname}".Trim();
        return full.Length > 0 ? full : u.Username;
    }
}
