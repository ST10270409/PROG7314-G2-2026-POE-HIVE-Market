using System.Text.Json.Serialization;

namespace HiveMarketApi.Models;

// NOTE ON FIELD NAMES: every property here has an explicit [JsonPropertyName]
// matching the Kotlin domain models (app/src/main/java/com/hivemarket/app/domain/Models.kt)
// character-for-character — e.g. "listingID", not "listingId". C#'s default
// JSON naming policies would produce "listingId" (lowercase d), which the
// Android app's kotlinx.serialization would silently fail to populate rather
// than error on. Do not rely on a global camelCase policy for this reason —
// every field is explicit on purpose.

public class UserEntity
{
    public int UserID { get; set; }
    public string FirebaseUid { get; set; } = "";
    public string Name { get; set; } = "";
    public string Surname { get; set; } = "";
    public string Username { get; set; } = "";
    public string Email { get; set; } = "";
    public string StudentNumber { get; set; } = "";
    public string? Campus { get; set; }
    public int TrustScore { get; set; } = 0;
    public string? Language { get; set; } = "en";
    public bool NotificationsEnabled { get; set; } = true;
    public bool BiometricEnabled { get; set; } = false;
}

public class CategoryEntity
{
    public int CategoryID { get; set; }
    public string Name { get; set; } = "";
    public string? Description { get; set; }
}

public class ListingEntity
{
    public int ListingID { get; set; }
    public string Title { get; set; } = "";
    public string Description { get; set; } = "";
    public int CategoryID { get; set; }
    public double Price { get; set; }
    public string? Condition { get; set; }
    public string? Image { get; set; }
    public DateTime DatePosted { get; set; } = DateTime.UtcNow;
    public int SellerID { get; set; }
    public string Status { get; set; } = "Active";
    public string? ClientId { get; set; }
}

public class OfferEntity
{
    public int OfferID { get; set; }
    public int ListingID { get; set; }
    public int BuyerID { get; set; }
    public double Amount { get; set; }
    public string? Message { get; set; }
    public DateTime DateOffered { get; set; } = DateTime.UtcNow;
    public string Status { get; set; } = "Pending";
}

public class ConversationEntity
{
    public int ConversationID { get; set; }
    public int ListingID { get; set; }
    public int BuyerID { get; set; }
    public int SellerID { get; set; }
    public string? LastMessage { get; set; }
    public DateTime? LastUpdated { get; set; }
}

public class MessageEntity
{
    public int MessageID { get; set; }
    public int ConversationID { get; set; }
    public int SenderID { get; set; }
    public string Content { get; set; } = "";
    public string Status { get; set; } = "Sent";
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
}

// ---- DTOs (response/request shapes — mirrors HiveMarketApi.kt exactly) ----

public class UserDto
{
    [JsonPropertyName("userID")] public int UserID { get; set; }
    [JsonPropertyName("name")] public string Name { get; set; } = "";
    [JsonPropertyName("surname")] public string Surname { get; set; } = "";
    [JsonPropertyName("username")] public string Username { get; set; } = "";
    [JsonPropertyName("email")] public string Email { get; set; } = "";
    [JsonPropertyName("studentNumber")] public string StudentNumber { get; set; } = "";
    [JsonPropertyName("campus")] public string? Campus { get; set; }
    [JsonPropertyName("trustScore")] public int TrustScore { get; set; }
}

public class AuthResponseDto
{
    [JsonPropertyName("userID")] public int UserID { get; set; }
    [JsonPropertyName("name")] public string Name { get; set; } = "";
    [JsonPropertyName("email")] public string Email { get; set; } = "";
    [JsonPropertyName("trustScore")] public int TrustScore { get; set; }
    [JsonPropertyName("isNewUser")] public bool IsNewUser { get; set; }
}

public class UpdateProfileRequestDto
{
    [JsonPropertyName("name")] public string? Name { get; set; }
    [JsonPropertyName("surname")] public string? Surname { get; set; }
    [JsonPropertyName("username")] public string? Username { get; set; }
}

public class ListingDto
{
    [JsonPropertyName("listingID")] public int ListingID { get; set; }
    [JsonPropertyName("title")] public string Title { get; set; } = "";
    [JsonPropertyName("description")] public string Description { get; set; } = "";
    [JsonPropertyName("categoryID")] public int CategoryID { get; set; }
    [JsonPropertyName("price")] public double Price { get; set; }
    [JsonPropertyName("condition")] public string? Condition { get; set; }
    [JsonPropertyName("image")] public string? Image { get; set; }
    [JsonPropertyName("datePosted")] public string DatePosted { get; set; } = "";
    [JsonPropertyName("sellerID")] public int SellerID { get; set; }
    [JsonPropertyName("status")] public string Status { get; set; } = "";
    [JsonPropertyName("sellerName")] public string? SellerName { get; set; }
    [JsonPropertyName("sellerTrustScore")] public int? SellerTrustScore { get; set; }
}

public class ListingsResponseDto
{
    [JsonPropertyName("items")] public List<ListingDto> Items { get; set; } = new();
    [JsonPropertyName("nextPage")] public string? NextPage { get; set; }
}

public class CreateListingRequestDto
{
    [JsonPropertyName("clientId")] public string ClientId { get; set; } = "";
    [JsonPropertyName("title")] public string Title { get; set; } = "";
    [JsonPropertyName("description")] public string Description { get; set; } = "";
    [JsonPropertyName("price")] public double Price { get; set; }
    [JsonPropertyName("categoryID")] public int CategoryID { get; set; }
    [JsonPropertyName("condition")] public string? Condition { get; set; }
    [JsonPropertyName("image")] public string? Image { get; set; }
}

public class CreateListingResponseDto
{
    [JsonPropertyName("listingID")] public int ListingID { get; set; }
    [JsonPropertyName("status")] public string Status { get; set; } = "";
    [JsonPropertyName("datePosted")] public string DatePosted { get; set; } = "";
}

public class OfferDto
{
    [JsonPropertyName("offerID")] public int OfferID { get; set; }
    [JsonPropertyName("listingID")] public int ListingID { get; set; }
    [JsonPropertyName("buyerID")] public int BuyerID { get; set; }
    [JsonPropertyName("amount")] public double Amount { get; set; }
    [JsonPropertyName("message")] public string? Message { get; set; }
    [JsonPropertyName("dateOffered")] public string DateOffered { get; set; } = "";
    [JsonPropertyName("status")] public string Status { get; set; } = "";
}

public class MakeOfferRequestDto
{
    [JsonPropertyName("amount")] public double Amount { get; set; }
    [JsonPropertyName("message")] public string? Message { get; set; }
}

public class ConversationDto
{
    [JsonPropertyName("conversationID")] public int ConversationID { get; set; }
    [JsonPropertyName("listingID")] public int ListingID { get; set; }
    [JsonPropertyName("buyerID")] public int BuyerID { get; set; }
    [JsonPropertyName("sellerID")] public int SellerID { get; set; }
    [JsonPropertyName("lastMessage")] public string? LastMessage { get; set; }
    [JsonPropertyName("lastUpdated")] public string? LastUpdated { get; set; }
    [JsonPropertyName("otherParticipant")] public string? OtherParticipant { get; set; }
}

public class ConversationsResponseDto
{
    [JsonPropertyName("items")] public List<ConversationDto> Items { get; set; } = new();
}

public class StartConversationRequestDto
{
    [JsonPropertyName("listingID")] public int ListingID { get; set; }
}

public class MessageDto
{
    [JsonPropertyName("messageID")] public int MessageID { get; set; }
    [JsonPropertyName("conversationID")] public int ConversationID { get; set; }
    [JsonPropertyName("senderID")] public int SenderID { get; set; }
    [JsonPropertyName("content")] public string Content { get; set; } = "";
    [JsonPropertyName("status")] public string Status { get; set; } = "";
    [JsonPropertyName("timestamp")] public string Timestamp { get; set; } = "";
}

public class MessagesResponseDto
{
    [JsonPropertyName("items")] public List<MessageDto> Items { get; set; } = new();
}

public class SendMessageRequestDto
{
    [JsonPropertyName("content")] public string Content { get; set; } = "";
}

public class SettingsRequestDto
{
    [JsonPropertyName("language")] public string? Language { get; set; }
    [JsonPropertyName("notificationsEnabled")] public bool? NotificationsEnabled { get; set; }
    [JsonPropertyName("biometricEnabled")] public bool? BiometricEnabled { get; set; }
}

public class UpdatedResponseDto
{
    [JsonPropertyName("updated")] public bool Updated { get; set; }
}
