using HiveMarketApi.Data;
using HiveMarketApi.Models;
using HiveMarketApi.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace HiveMarketApi.Controllers;

[ApiController]
[Authorize]
[Route("api/conversations")]
public class ConversationsController : ControllerBase
{
    private readonly CurrentUserService _currentUser;
    private readonly AppDbContext _db;

    public ConversationsController(CurrentUserService currentUser, AppDbContext db)
    {
        _currentUser = currentUser;
        _db = db;
    }

    // GET /api/conversations — the inbox list. Only returns conversations
    // the caller is actually a participant in (buyer or seller) — this is
    // the ownership check the Planning and Design document's "Structural
    // plan" describes ("only a conversation's two participants may read or
    // post within it").
    [HttpGet]
    public async Task<ActionResult<ConversationsResponseDto>> GetConversations()
    {
        var user = await _currentUser.GetOrCreateCurrentUserAsync();
        var conversations = await _db.Conversations
            .Where(c => c.BuyerID == user.UserID || c.SellerID == user.UserID)
            .OrderByDescending(c => c.LastUpdated)
            .ToListAsync();

        var otherIds = conversations.Select(c => c.BuyerID == user.UserID ? c.SellerID : c.BuyerID).Distinct().ToList();
        var others = await _db.Users.Where(u => otherIds.Contains(u.UserID)).ToDictionaryAsync(u => u.UserID);

        return Ok(new ConversationsResponseDto
        {
            Items = conversations.Select(c =>
            {
                var otherId = c.BuyerID == user.UserID ? c.SellerID : c.BuyerID;
                var other = others.GetValueOrDefault(otherId);
                return new ConversationDto
                {
                    ConversationID = c.ConversationID, ListingID = c.ListingID, BuyerID = c.BuyerID,
                    SellerID = c.SellerID, LastMessage = c.LastMessage,
                    LastUpdated = c.LastUpdated?.ToString("O"),
                    OtherParticipant = other != null ? $"{other.Name} {other.Surname}".Trim() : null
                };
            }).ToList()
        });
    }

    // POST /api/conversations — finds-or-creates a conversation for a
    // listing, matching ListingRepository.startOrGetConversation's
    // find-or-start expectation. A listing can have at most one
    // conversation per buyer, so repeated taps of "Message" on the same
    // listing reopen the same thread rather than spawning duplicates.
    [HttpPost]
    public async Task<ActionResult<ConversationDto>> StartConversation([FromBody] StartConversationRequestDto body)
    {
        var user = await _currentUser.GetOrCreateCurrentUserAsync();
        var listing = await _db.Listings.FirstOrDefaultAsync(l => l.ListingID == body.ListingID);
        if (listing == null) return NotFound();

        var existing = await _db.Conversations.FirstOrDefaultAsync(
            c => c.ListingID == body.ListingID && c.BuyerID == user.UserID && c.SellerID == listing.SellerID);
        if (existing != null) return Ok(ToDto(existing, null));

        var conversation = new ConversationEntity
        {
            ListingID = body.ListingID, BuyerID = user.UserID, SellerID = listing.SellerID,
            LastUpdated = DateTime.UtcNow
        };
        _db.Conversations.Add(conversation);
        await _db.SaveChangesAsync();
        return Ok(ToDto(conversation, null));
    }

    [HttpGet("{conversationID:int}/messages")]
    public async Task<ActionResult<MessagesResponseDto>> GetMessages(int conversationID)
    {
        var user = await _currentUser.GetOrCreateCurrentUserAsync();
        var conversation = await _db.Conversations.FirstOrDefaultAsync(c => c.ConversationID == conversationID);
        if (conversation == null) return NotFound();
        if (conversation.BuyerID != user.UserID && conversation.SellerID != user.UserID) return Forbid();

        var messages = await _db.Messages
            .Where(m => m.ConversationID == conversationID)
            .OrderBy(m => m.Timestamp)
            .ToListAsync();

        return Ok(new MessagesResponseDto
        {
            Items = messages.Select(m => new MessageDto
            {
                MessageID = m.MessageID, ConversationID = m.ConversationID, SenderID = m.SenderID,
                Content = m.Content, Status = m.Status, Timestamp = m.Timestamp.ToString("O")
            }).ToList()
        });
    }

    [HttpPost("{conversationID:int}/messages")]
    public async Task<ActionResult<MessageDto>> SendMessage(int conversationID, [FromBody] SendMessageRequestDto body)
    {
        var user = await _currentUser.GetOrCreateCurrentUserAsync();
        var conversation = await _db.Conversations.FirstOrDefaultAsync(c => c.ConversationID == conversationID);
        if (conversation == null) return NotFound();
        if (conversation.BuyerID != user.UserID && conversation.SellerID != user.UserID) return Forbid();

        var message = new MessageEntity
        {
            ConversationID = conversationID, SenderID = user.UserID, Content = body.Content,
            Status = "Sent", Timestamp = DateTime.UtcNow
        };
        _db.Messages.Add(message);

        conversation.LastMessage = body.Content;
        conversation.LastUpdated = message.Timestamp;

        await _db.SaveChangesAsync();

        return Ok(new MessageDto
        {
            MessageID = message.MessageID, ConversationID = message.ConversationID, SenderID = message.SenderID,
            Content = message.Content, Status = message.Status, Timestamp = message.Timestamp.ToString("O")
        });
    }

    private static ConversationDto ToDto(ConversationEntity c, string? otherParticipant) => new()
    {
        ConversationID = c.ConversationID, ListingID = c.ListingID, BuyerID = c.BuyerID, SellerID = c.SellerID,
        LastMessage = c.LastMessage, LastUpdated = c.LastUpdated?.ToString("O"), OtherParticipant = otherParticipant
    };
}
