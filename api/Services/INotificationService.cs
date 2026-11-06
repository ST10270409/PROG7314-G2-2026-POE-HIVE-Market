using HiveMarketApi.Models;

namespace HiveMarketApi.Services;

public static class NotificationTypes
{
    public const string NewOffer = "new_offer";
    public const string OfferAccepted = "offer_accepted";
    public const string OfferRejected = "offer_rejected";
    public const string NewMessage = "new_message";
}

/// <summary>
/// Real-time notifications. Every event does two things:
///   1. writes a document to Cloud Firestore (NoSQL) -> the in-app inbox, which
///      the Android app reads live with a snapshot listener;
///   2. sends a data-only Firebase Cloud Messaging push to the recipient's devices
///      (unless they switched notifications off in Settings).
/// Implementations must NEVER throw: a notification failure must not fail the
/// API request that caused it.
/// </summary>
public interface INotificationService
{
    Task NotifyAsync(UserEntity recipient, string type, string actor, string amount, string listing);
    Task RegisterDeviceAsync(string firebaseUid, string token);
    Task UnregisterDeviceAsync(string firebaseUid, string token);
}

/// <summary>Used when FIREBASE_SERVICE_ACCOUNT_JSON is not configured (local dev, CI tests).</summary>
public class NoOpNotificationService : INotificationService
{
    private readonly ILogger<NoOpNotificationService> _log;
    public NoOpNotificationService(ILogger<NoOpNotificationService> log) => _log = log;

    public Task NotifyAsync(UserEntity recipient, string type, string actor, string amount, string listing)
    {
        _log.LogInformation("[notifications disabled] {Type} for user {UserId}", type, recipient.UserID);
        return Task.CompletedTask;
    }
    public Task RegisterDeviceAsync(string firebaseUid, string token) => Task.CompletedTask;
    public Task UnregisterDeviceAsync(string firebaseUid, string token) => Task.CompletedTask;
}
