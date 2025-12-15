package me.math3w.bazaar.bazaar.portfolio;

import java.util.UUID;

public class PortfolioTransaction {
    private final UUID id; // [NEW] ID định danh cho Database
    private final String productId;
    private int amount;
    private final double value;
    private final long timestamp;
    private final TransactionType type;

    // 24h Cooldown (86400000 ms)
    public static final long COOLDOWN_MS = 86400000L;

    public enum TransactionType {
        BUY_HOLDING,
        SELL_PENDING
    }

    // Constructor cho code Java tạo mới
    public PortfolioTransaction(String productId, int amount, double value, TransactionType type) {
        this(UUID.randomUUID(), productId, amount, value, type, System.currentTimeMillis());
    }

    // Constructor cho Database load lên (có sẵn ID và Time)
    public PortfolioTransaction(UUID id, String productId, int amount, double value, TransactionType type, long timestamp) {
        this.id = id;
        this.productId = productId;
        this.amount = amount;
        this.value = value;
        this.type = type;
        this.timestamp = timestamp;
    }

    public UUID getId() { return id; }
    public String getProductId() { return productId; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public double getValue() { return value; }
    public long getTimestamp() { return timestamp; }
    public TransactionType getType() { return type; }

    public boolean isSettled() {
        return System.currentTimeMillis() - timestamp >= COOLDOWN_MS;
    }

    public long getTimeRemaining() {
        long passed = System.currentTimeMillis() - timestamp;
        return Math.max(0, COOLDOWN_MS - passed);
    }
}