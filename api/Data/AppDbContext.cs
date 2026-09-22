using HiveMarketApi.Models;
using Microsoft.EntityFrameworkCore;

namespace HiveMarketApi.Data;

public class AppDbContext : DbContext
{
    public AppDbContext(DbContextOptions<AppDbContext> options) : base(options) { }

    public DbSet<UserEntity> Users => Set<UserEntity>();
    public DbSet<CategoryEntity> Categories => Set<CategoryEntity>();
    public DbSet<ListingEntity> Listings => Set<ListingEntity>();
    public DbSet<OfferEntity> Offers => Set<OfferEntity>();
    public DbSet<ConversationEntity> Conversations => Set<ConversationEntity>();
    public DbSet<MessageEntity> Messages => Set<MessageEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<UserEntity>().HasIndex(u => u.FirebaseUid).IsUnique();

        // Matches the seed values already assumed by the Android client's
        // CreateListingScreen category dropdown (Textbook=1, Electronics=2,
        // Furniture=3) — see the Planning and Design document, Section 7.
        modelBuilder.Entity<CategoryEntity>().HasData(
            new CategoryEntity { CategoryID = 1, Name = "Textbooks", Description = "Course textbooks and study guides" },
            new CategoryEntity { CategoryID = 2, Name = "Electronics", Description = "Laptops, calculators, accessories" },
            new CategoryEntity { CategoryID = 3, Name = "Furniture", Description = "Desks, chairs, room furniture" }
        );
    }
}
