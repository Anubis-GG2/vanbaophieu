package me.math3w.bazaar.bazaar.portfolio;

import java.util.UUID;

public class PortfolioTransaction {
    private final UUID id;
    private final String productId;
    private int amount;
    private final double value;
    private long timestamp;
    private final TransactionType type;
    private double settledValue;
    private double claimedValue;

    public static final long COOLDOWN_MS = 86400000L;

    public enum TransactionType {
        BUY_HOLDING,
        SELL_PENDING
    }

    public PortfolioTransaction(String productId, int amount, double value, TransactionType type) {
        this(UUID.randomUUID(), productId, amount, value, type, System.currentTimeMillis(), 0.0, 0.0);
    }

    public PortfolioTransaction(UUID id, String productId, int amount, double value, TransactionType type, long timestamp, double settledValue, double claimedValue) {
        this.id = id;
        this.productId = productId;
        this.amount = amount;
        this.value = value;
        this.type = type;
        this.timestamp = timestamp;
        this.settledValue = settledValue;
        this.claimedValue = claimedValue;
    }

    public UUID getId() { return id; }
    public String getProductId() { return productId; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public double getValue() { return value; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public TransactionType getType() { return type; }

    public double getSettledValue() { return settledValue; }
    public void setSettledValue(double settledValue) { this.settledValue = settledValue; }

    public double getClaimedValue() { return claimedValue; }
    public void setClaimedValue(double claimedValue) { this.claimedValue = claimedValue; }

    public boolean isSettled() {
        if (type == TransactionType.BUY_HOLDING) {
            return getTimeRemaining() <= 0;
        }
        return settledValue >= value;
    }

    public double getRemainingValueToSettle() {
        return value - settledValue;
    }

    public long getTimeRemaining() {
        long passed = System.currentTimeMillis() - timestamp;
        return Math.max(0, COOLDOWN_MS - passed);
    }
}