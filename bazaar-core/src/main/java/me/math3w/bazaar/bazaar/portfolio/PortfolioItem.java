package me.math3w.bazaar.bazaar.portfolio;

public class PortfolioItem {
    private final String productId;
    private final int amount;
    private final long purchaseTimestamp;

    public PortfolioItem(String productId, int amount, long purchaseTimestamp) {
        this.productId = productId;
        this.amount = amount;
        this.purchaseTimestamp = purchaseTimestamp;
    }

    public String getProductId() {
        return productId;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isTradable() {
        return System.currentTimeMillis() - purchaseTimestamp >= 86400000;
    }
}