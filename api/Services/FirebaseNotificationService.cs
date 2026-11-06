using FirebaseAdmin.Messaging;
using Google.Cloud.Firestore;
using HiveMarketApi.Models;

namespace HiveMarketApi.Services;

/// <summary>
/// Firestore collections used (NoSQL):
///   notifications/{autoId}  { recipientUid, type, actor, amount, listing, createdAt(ms), read }
///   deviceTokens/{token}    { uid, updatedAt(ms) }   (doc id = FCM token, so a token can only ever belong to ONE user)
/// </summary>
public class FirebaseNotificationService : INotificationService
{
    private readonly FirestoreDb _db;
    private readonly ILogger<FirebaseNotificationService> _log;

    public FirebaseNotificationService(FirestoreDb db, ILogger<FirebaseNotificationService> log)
    {
        _db = db;
        _log = log;
    }

    public async Task NotifyAsync(UserEntity recipient, string type, string actor, string amount, string listing)
    {
        try
        {
            var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

            // 1) NoSQL write: the in-app inbox entry.
            await _db.Collection("notifications").AddAsync(new Dictionary<string, object>
            {
                ["recipientUid"] = recipient.FirebaseUid,
                ["type"] = type,
                ["actor"] = actor,
                ["amount"] = amount,
                ["listing"] = listing,
                ["createdAt"] = now,
                ["read"] = false
            });

            // 2) Push, only if the user has notifications switched on.
            if (!recipient.NotificationsEnabled) return;

            var tokens = await GetTokensAsync(recipient.FirebaseUid);
            if (tokens.Count == 0) return;

            var response = await FirebaseMessaging.DefaultInstance.SendEachForMulticastAsync(new MulticastMessage
            {
                Tokens = tokens,
                Android = new AndroidConfig { Priority = Priority.High },
                // Data-only: the app builds the localised notification text itself.
                Data = new Dictionary<string, string>
                {
                    ["type"] = type, ["actor"] = actor, ["amount"] = amount, ["listing"] = listing
                }
            });

            // Remove tokens FCM says are dead so we stop sending to them.
            for (var i = 0; i < response.Responses.Count; i++)
            {
                var code = response.Responses[i].Exception?.MessagingErrorCode;
                if (code == MessagingErrorCode.Unregistered || code == MessagingErrorCode.InvalidArgument)
                    await _db.Collection("deviceTokens").Document(tokens[i]).DeleteAsync();
            }
            _log.LogInformation("Push {Type}: {Ok}/{Total} delivered", type, response.SuccessCount, tokens.Count);
        }
        catch (Exception ex)
        {
            _log.LogWarning(ex, "Notification {Type} failed (request still succeeds)", type);
        }
    }

    public async Task RegisterDeviceAsync(string firebaseUid, string token)
    {
        try
        {
            await _db.Collection("deviceTokens").Document(token).SetAsync(new Dictionary<string, object>
            {
                ["uid"] = firebaseUid,
                ["updatedAt"] = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()
            });
        }
        catch (Exception ex) { _log.LogWarning(ex, "Device registration failed"); }
    }

    public async Task UnregisterDeviceAsync(string firebaseUid, string token)
    {
        try
        {
            var doc = _db.Collection("deviceTokens").Document(token);
            var snap = await doc.GetSnapshotAsync();
            // Only the owner may remove it.
            if (snap.Exists && snap.GetValue<string>("uid") == firebaseUid) await doc.DeleteAsync();
        }
        catch (Exception ex) { _log.LogWarning(ex, "Device unregister failed"); }
    }

    private async Task<List<string>> GetTokensAsync(string uid)
    {
        var snap = await _db.Collection("deviceTokens").WhereEqualTo("uid", uid).GetSnapshotAsync();
        return snap.Documents.Select(d => d.Id).ToList();
    }
}
