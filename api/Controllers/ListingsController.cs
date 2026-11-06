using HiveMarketApi.Data;
using HiveMarketApi.Models;
using HiveMarketApi.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace HiveMarketApi.Controllers;

[ApiController]
[Authorize]
[Route("api/listings")]
public class ListingsController : ControllerBase
{
    private readonly CurrentUserService _currentUser;
    private readonly AppDbContext _db;
    private readonly INotificationService _notifications;

    public ListingsController(CurrentUserService currentUser, AppDbContext db, INotificationService notifications)
    {
        _currentUser = currentUser;
        _db = db;
        _notifications = notifications;
    }

    // GET /api/listings — backs Browse. Supports the same query params
    // BrowseViewModel already sends (categoryID, q); minPrice/maxPrice are
    // accepted for contract completeness even though the current UI
    // doesn't expose a price filter yet.
    [HttpGet]
    public async Task<ActionResult<ListingsResponseDto>> GetListings(
        [FromQuery] string? q, [FromQuery] int? categoryID,
        [FromQuery] double? minPrice, [FromQuery] double? maxPrice)
    {
        var query = _db.Listings.AsQueryable();
        if (categoryID.HasValue) query = query.Where(l => l.CategoryID == categoryID.Value);
        if (minPrice.HasValue) query = query.Where(l => l.Price >= minPrice.Value);
        if (maxPrice.HasValue) query = query.Where(l => l.Price <= maxPrice.Value);
        if (!string.IsNullOrWhiteSpace(q)) query = query.Where(l => l.Title.Contains(q));

        var listings = await query.OrderByDescending(l => l.DatePosted).ToListAsync();
        var sellerIds = listings.Select(l => l.SellerID).Distinct().ToList();
        var sellers = await _db.Users.Where(u => sellerIds.Contains(u.UserID)).ToDictionaryAsync(u => u.UserID);

        return Ok(new ListingsResponseDto
        {
            Items = listings.Select(l => ToDto(l, sellers.GetValueOrDefault(l.SellerID))).ToList()
        });
    }

    // POST /api/listings — backs Create Listing (FR4/FR9) and, by
    // extension, the Offline Drafts retry flow, since retrySync calls this
    // same endpoint. sellerID is deliberately NOT read from the request
    // body (CreateListingRequestDto has no such field) — it's derived from
    // the authenticated token via CurrentUserService, so a client can never
    // claim to be selling as someone else.
    [HttpPost]
    public async Task<ActionResult<CreateListingResponseDto>> CreateListing([FromBody] CreateListingRequestDto body)
    {
        var user = await _currentUser.GetOrCreateCurrentUserAsync();

        // clientId doubles as an idempotency key: a retried POST (e.g. from
        // Offline Drafts' manual retry, or a dropped-then-resent request)
        // with the same clientId returns the existing row instead of
        // creating a duplicate listing.
        var existing = await _db.Listings.FirstOrDefaultAsync(l => l.ClientId == body.ClientId);
        if (existing != null)
        {
            return Ok(new CreateListingResponseDto
            {
                ListingID = existing.ListingID, Status = existing.Status,
                DatePosted = existing.DatePosted.ToString("O")
            });
        }

        var listing = new ListingEntity
        {
            Title = body.Title, Description = body.Description, CategoryID = body.CategoryID,
            Price = body.Price, Condition = body.Condition, Image = body.Image,
            SellerID = user.UserID, Status = "Active", ClientId = body.ClientId,
            DatePosted = DateTime.UtcNow
        };
        _db.Listings.Add(listing);
        await _db.SaveChangesAsync();

        return Ok(new CreateListingResponseDto
        {
            ListingID = listing.ListingID, Status = listing.Status, DatePosted = listing.DatePosted.ToString("O")
        });
    }

    // GET /api/listings/{listingID} — the detail endpoint. Unlike the list
    // endpoint, this populates sellerName/sellerTrustScore (see the
    // comment on Listing.kt about lean lists vs. rich detail).
    [HttpGet("{listingID:int}")]
    public async Task<ActionResult<ListingDto>> GetListing(int listingID)
    {
        var listing = await _db.Listings.FirstOrDefaultAsync(l => l.ListingID == listingID);
        if (listing == null) return NotFound();
        var seller = await _db.Users.FirstOrDefaultAsync(u => u.UserID == listing.SellerID);
        return Ok(ToDto(listing, seller));
    }

    // POST /api/listings/{listingID}/offers — backs the Make Offer dialog
    // on Listing Detail. buyerID comes from the token, same reasoning as
    // sellerID above.
    [HttpPost("{listingID:int}/offers")]
    public async Task<ActionResult<OfferDto>> MakeOffer(int listingID, [FromBody] MakeOfferRequestDto body)
    {
        var listing = await _db.Listings.FirstOrDefaultAsync(l => l.ListingID == listingID);
        if (listing == null) return NotFound();
        var user = await _currentUser.GetOrCreateCurrentUserAsync();

        if (body.Amount <= 0) return BadRequest(new { error = "Offer must be greater than zero." });
        if (listing.SellerID == user.UserID) return BadRequest(new { error = "You cannot make an offer on your own listing." });
        if (listing.Status != "Active") return Conflict(new { error = "This listing is no longer available." });

        var offer = new OfferEntity
        {
            ListingID = listingID, BuyerID = user.UserID, Amount = body.Amount,
            Message = body.Message, Status = "Pending", DateOffered = DateTime.UtcNow
        };
        _db.Offers.Add(offer);
        await _db.SaveChangesAsync();

        // Tell the seller (Firestore inbox + push).
        var seller = await _db.Users.FirstOrDefaultAsync(u => u.UserID == listing.SellerID);
        if (seller != null)
        {
            await _notifications.NotifyAsync(seller, NotificationTypes.NewOffer,
                UserNames.Display(user), offer.Amount.ToString("0"), listing.Title);
        }

        return Ok(new OfferDto
        {
            OfferID = offer.OfferID, ListingID = offer.ListingID, BuyerID = offer.BuyerID,
            Amount = offer.Amount, Message = offer.Message, Status = offer.Status,
            DateOffered = offer.DateOffered.ToString("O")
        });
    }

    // GET /api/listings/{listingID}/offers — the SELLER's view of offers received.
    [HttpGet("{listingID:int}/offers")]
    public async Task<ActionResult<OffersResponseDto>> GetOffers(int listingID)
    {
        var listing = await _db.Listings.FirstOrDefaultAsync(l => l.ListingID == listingID);
        if (listing == null) return NotFound();
        var user = await _currentUser.GetOrCreateCurrentUserAsync();
        if (listing.SellerID != user.UserID) return Forbid();

        var offers = await _db.Offers.Where(o => o.ListingID == listingID).OrderByDescending(o => o.DateOffered).ToListAsync();
        var buyerIds = offers.Select(o => o.BuyerID).Distinct().ToList();
        var buyers = await _db.Users.Where(u => buyerIds.Contains(u.UserID)).ToDictionaryAsync(u => u.UserID);

        return Ok(new OffersResponseDto
        {
            Items = offers.Select(o => new OfferDto
            {
                OfferID = o.OfferID, ListingID = o.ListingID, BuyerID = o.BuyerID, Amount = o.Amount,
                Message = o.Message, Status = o.Status, DateOffered = o.DateOffered.ToString("O"),
                BuyerName = buyers.TryGetValue(o.BuyerID, out var b) ? UserNames.Display(b) : null
            }).ToList()
        });
    }

    private static ListingDto ToDto(ListingEntity l, UserEntity? seller) => new()
    {
        ListingID = l.ListingID, Title = l.Title, Description = l.Description, CategoryID = l.CategoryID,
        Price = l.Price, Condition = l.Condition, Image = l.Image, DatePosted = l.DatePosted.ToString("O"),
        SellerID = l.SellerID, Status = l.Status,
        SellerName = seller != null ? $"{seller.Name} {seller.Surname}".Trim() : null,
        SellerTrustScore = seller?.TrustScore
    };
}
