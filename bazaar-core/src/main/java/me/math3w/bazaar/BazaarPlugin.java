package me.math3w.bazaar;

import me.math3w.bazaar.api.BazaarAPI;
import me.math3w.bazaar.api.bazaar.Bazaar;
import me.math3w.bazaar.api.bazaar.orders.OrderManager;
import me.math3w.bazaar.api.config.MenuConfig;
import me.math3w.bazaar.api.edit.EditManager;
import me.math3w.bazaar.api.menu.ClickActionManager;
import me.math3w.bazaar.api.menu.ItemPlaceholders;
import me.math3w.bazaar.api.menu.MenuHistory;
import me.math3w.bazaar.bazaar.BazaarImpl;
import me.math3w.bazaar.bazaar.category.CategoryConfiguration;
import me.math3w.bazaar.bazaar.market.LiquidityService;
import me.math3w.bazaar.bazaar.market.MarketTicker;
import me.math3w.bazaar.bazaar.portfolio.PortfolioManager;
import me.math3w.bazaar.bazaar.product.ProductConfiguration;
import me.math3w.bazaar.bazaar.productcategory.ProductCategoryConfiguration;
import me.math3w.bazaar.commands.BazaarCommand;
import me.math3w.bazaar.commands.CategoryCommand;
import me.math3w.bazaar.commands.EditCommand;
import me.math3w.bazaar.commands.StockCommand;
import me.math3w.bazaar.config.BazaarConfig;
import me.math3w.bazaar.config.DatabaseConfig;
import me.math3w.bazaar.config.DefaultMenuConfig;
import me.math3w.bazaar.config.MessagesConfig;
import me.math3w.bazaar.edit.DefaultEditManager;
import me.math3w.bazaar.menu.*;
import me.math3w.bazaar.menu.configurations.*;
import me.math3w.bazaar.messageinput.MessageInputManager;
import me.zort.containr.Containr;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import me.math3w.bazaar.menu.configurations.PendingOrdersMenuConfiguration;

public class BazaarPlugin extends JavaPlugin implements BazaarAPI {
    private Economy economy = null;
    private MessagesConfig messagesConfig;
    private BazaarConfig bazaarConfig;
    private MenuConfig menuConfig;
    private DatabaseConfig databaseConfig;
    private Bazaar bazaar;
    private ClickActionManager clickActionManager;
    private ItemPlaceholders itemPlaceholders;
    private MenuHistory menuHistory;

    // Managers mới
    private PortfolioManager portfolioManager;
    private EditManager editManager;
    private MessageInputManager messageInputManager;

    private MarketTicker marketTicker;
    private LiquidityService liquidityService;

    @Override
    public void onLoad() {
        ConfigurationSerialization.registerClass(ProductConfiguration.class);
        ConfigurationSerialization.registerClass(ProductCategoryConfiguration.class);
        ConfigurationSerialization.registerClass(CategoryConfiguration.class);
        ConfigurationSerialization.registerClass(DefaultConfigurableMenuItem.class);
        ConfigurationSerialization.registerClass(CategoryMenuConfiguration.class);
        ConfigurationSerialization.registerClass(ProductCategoryMenuConfiguration.class);
        ConfigurationSerialization.registerClass(SearchMenuConfiguration.class);
        ConfigurationSerialization.registerClass(ProductMenuConfiguration.class);
        ConfigurationSerialization.registerClass(ConfirmationMenuConfiguration.class);
        ConfigurationSerialization.registerClass(OrdersMenuConfiguration.class);
        ConfigurationSerialization.registerClass(PortfolioMenuConfiguration.class);
        ConfigurationSerialization.registerClass(PendingOrdersMenuConfiguration.class);

    }

    @Override
    public void onEnable() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            Bukkit.getLogger().severe("No registered Vault provider found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Containr.init(this);

        economy = rsp.getProvider();

        messagesConfig = new MessagesConfig(this);
        bazaarConfig = new BazaarConfig(this);
        menuConfig = new DefaultMenuConfig(this);
        databaseConfig = new DatabaseConfig(this);

        // Đăng ký lệnh cũ
        getCommand("bazaar").setExecutor(new BazaarCommand(this));
        getCommand("bazaaredit").setExecutor(new EditCommand(this));

        // [NEW] Đăng ký lệnh Stock & Category
        getCommand("category").setExecutor(new CategoryCommand(this));
        getCommand("stock").setExecutor(new StockCommand(this));

        bazaar = new BazaarImpl(this);

        // Khởi tạo hệ thống tài chính
        liquidityService = new LiquidityService(this);
        portfolioManager = new PortfolioManager(this);

        marketTicker = new MarketTicker(this);
        marketTicker.start();

        clickActionManager = new DefaultClickActionManager(this);
        itemPlaceholders = new DefaultItemPlaceholders(this);
        menuHistory = new DefaultMenuHistory(this);

        editManager = new DefaultEditManager(this);
        messageInputManager = new MessageInputManager(this);

        Bukkit.getPluginManager().registerEvents(new MenuListeners(this), this);
    }

    @Override
    public void onDisable() {
        if (marketTicker != null) marketTicker.stop();
        if (portfolioManager != null) {
            portfolioManager.stop(); // Đóng kết nối Database an toàn
        }

    }

    // --- Getters ---

    public MarketTicker getMarketTicker() {
        return marketTicker;
    }

    // [FIX] Sửa tên method từ getLiquidityService trong log lỗi thành đúng tên biến
    public LiquidityService getLiquidityService() {
        return liquidityService;
    }

    // [FIX] Thêm getter cho PortfolioManager
    public PortfolioManager getPortfolioManager() {
        return portfolioManager;
    }

    @Override
    public OrderManager getOrderManager() {
        throw new UnsupportedOperationException("OrderManager has been replaced by PortfolioManager");
    }

    @Override
    public Economy getEconomy() { return economy; }
    public MessagesConfig getMessagesConfig() { return messagesConfig; }
    public BazaarConfig getBazaarConfig() { return bazaarConfig; }
    @Override
    public MenuConfig getMenuConfig() { return menuConfig; }
    public DatabaseConfig getDatabaseConfig() { return databaseConfig; }
    @Override
    public Bazaar getBazaar() { return bazaar; }
    @Override
    public ClickActionManager getClickActionManager() { return clickActionManager; }
    @Override
    public ItemPlaceholders getItemPlaceholders() { return itemPlaceholders; }
    @Override
    public MenuHistory getMenuHistory() { return menuHistory; }
    @Override
    public EditManager getEditManager() { return editManager; }
    public MessageInputManager getMessageInputManager() { return messageInputManager; }
    @Override
    public JavaPlugin getPlugin() { return this; }

}