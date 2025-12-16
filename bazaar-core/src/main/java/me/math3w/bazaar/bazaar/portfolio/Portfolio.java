package me.math3w.bazaar.bazaar.portfolio;

import java.sql.Timestamp;
import java.util.UUID;

public class Portfolio {
    private final int id;
    private final UUID playerUuid;
    private final String productId;
    private final int amount;
    private final double pricePerUnit;
    private PortfolioStatus status;
    private final Timestamp createdAt;
    private final Timestamp unlockAt;
    private Timestamp payoutAt;

    public Portfolio(int id, UUID playerUuid, String productId, int amount, double pricePerUnit,
                     PortfolioStatus status, Timestamp createdAt, Timestamp unlockAt, Timestamp payoutAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.productId = productId;
        this.amount = amount;
        this.pricePerUnit = pricePerUnit;
        this.status = status;
        this.createdAt = createdAt;
        this.unlockAt = unlockAt;
        this.payoutAt = payoutAt;
    }

    public int getId() { return id; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getProductId() { return productId; }
    public int getAmount() { return amount; }
    public double getPricePerUnit() { return pricePerUnit; }
    public PortfolioStatus getStatus() { return status; }
    public void setStatus(PortfolioStatus status) { this.status = status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUnlockAt() { return unlockAt; }
    public Timestamp getPayoutAt() { return payoutAt; }
    public void setPayoutAt(Timestamp payoutAt) { this.payoutAt = payoutAt; }

    public enum PortfolioStatus {
        LOCKED,
        AVAILABLE,
        PENDING_PAYOUT,
        SOLD
    }
}