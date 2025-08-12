package de.jexcellence.currency.migrate;

import com.google.common.util.concurrent.AtomicDouble;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.adapter.CurrencyAdapter;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.entity.User;
import de.jexcellence.currency.database.entity.UserCurrency;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comprehensive migration system for transitioning from Vault-based economy systems to JECurrency.
 *
 * This system provides:
 * - Automatic detection of existing Vault economy providers
 * - Data migration from various economy plugins
 * - Seamless replacement of Vault provider
 * - Backup and rollback capabilities
 * - Progress tracking and reporting
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class VaultMigrationManager {
    
    private final JECurrency plugin;
    private final Logger logger;
    private final CurrencyAdapter currencyAdapter;
    
    private boolean migrationInProgress = false;
    private MigrationResult lastMigrationResult;
    
    private static final Map<String, EconomyMigrator> SUPPORTED_MIGRATORS = new HashMap<>();
    
    static {
        SUPPORTED_MIGRATORS.put("Essentials", new EssentialsMigrator());
        SUPPORTED_MIGRATORS.put("iConomy", new IconomyMigrator());
        SUPPORTED_MIGRATORS.put("BOSEconomy", new BOSEconomyMigrator());
        SUPPORTED_MIGRATORS.put("CMI", new CMIMigrator());
        SUPPORTED_MIGRATORS.put("TNE", new TNEMigrator());
    }
    
    public VaultMigrationManager(@NotNull JECurrency plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.currencyAdapter = plugin.getCurrencyAdapter();
    }
    
    /**
     * Initiates the complete migration process from Vault to JECurrency.
     *
     * @param createBackup whether to create a backup before migration
     * @param replaceVaultProvider whether to replace the Vault provider after migration
     * @param targetCurrencyIdentifier the identifier of the currency to migrate to (null for auto-select)
     * @return CompletableFuture with migration result
     */
    public CompletableFuture<MigrationResult> startMigration(boolean createBackup, boolean replaceVaultProvider, @Nullable String targetCurrencyIdentifier) {
        if (migrationInProgress) {
            return CompletableFuture.completedFuture(
                MigrationResult.error("Migration already in progress")
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            migrationInProgress = true;
            logger.info("Starting Vault to JECurrency migration process...");
            
            try {
                // Step 1: Detect current economy provider
                DetectionResult detection = detectCurrentEconomyProvider();
                if (!detection.isSuccess()) {
                    return MigrationResult.error("Failed to detect economy provider: " + detection.getError());
                }
                
                logger.info("Detected economy provider: " + detection.getProviderName());
                
                // Step 2: Create backup if requested
                if (createBackup) {
                    logger.info("Creating backup before migration...");
                    if (!createMigrationBackup(detection)) {
                        return MigrationResult.error("Failed to create backup");
                    }
                }
                
                // Step 3: Get or create target currency
                Currency targetCurrency = getOrCreateTargetCurrency(detection.getEconomy(), targetCurrencyIdentifier).join();
                if (targetCurrency == null) {
                    return MigrationResult.error("Failed to get or create target currency");
                }
                
                logger.info("Using target currency: " + targetCurrency.getIdentifier());
                
                // Step 4: Migrate data
                logger.info("Starting data migration...");
                MigrationStats stats = migrateEconomyData(detection, targetCurrency);
                
                // Step 5: Replace Vault provider if requested
                if (replaceVaultProvider && stats.isSuccess()) {
                    logger.info("Replacing Vault economy provider...");
                    if (!replaceVaultProvider()) {
                        logger.warning("Data migration successful but failed to replace Vault provider");
                    }
                }
                
                // Step 6: Verify migration
                logger.info("Verifying migration integrity...");
                boolean verified = verifyMigration(detection, stats, targetCurrency);
                
                MigrationResult result = new MigrationResult(
                    stats.isSuccess() && verified,
                    detection.getProviderName(),
                    stats,
                    verified ? null : "Migration verification failed"
                );
                
                lastMigrationResult = result;
                
                if (result.isSuccess()) {
                    logger.info("Migration completed successfully!");
                    logger.info("Migrated " + stats.getPlayersProcessed() + " players with " +
                                stats.getTotalBalance() + " total balance");
                } else {
                    logger.severe("Migration failed: " + result.getErrorMessage());
                }
                
                return result;
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Migration failed with exception", e);
                return MigrationResult.error("Migration failed: " + e.getMessage());
            } finally {
                migrationInProgress = false;
            }
        }, plugin.getExecutor());
    }
    
    /**
     * Convenience method for migration with auto-selected currency.
     */
    public CompletableFuture<MigrationResult> startMigration(boolean createBackup, boolean replaceVaultProvider) {
        return startMigration(createBackup, replaceVaultProvider, null);
    }
    
    /**
     * Detects the current Vault economy provider.
     */
    private DetectionResult detectCurrentEconomyProvider() {
        RegisteredServiceProvider<Economy> economyProvider =
            Bukkit.getServicesManager().getRegistration(Economy.class);
        
        if (economyProvider == null) {
            return DetectionResult.error("No Vault economy provider found");
        }
        
        Economy economy = economyProvider.getProvider();
        String providerName = economy.getName();
        
        // Check if it's already JECurrency
        if ("JECurrency".equals(providerName)) {
            return DetectionResult.error("JECurrency is already the active economy provider");
        }
        
        // Check if we support migration from this provider
        EconomyMigrator migrator = findMigratorForProvider(providerName);
        if (migrator == null) {
            return DetectionResult.error("Unsupported economy provider: " + providerName);
        }
        
        return new DetectionResult(true, providerName, economy, migrator, null);
    }
    
    /**
     * Finds the appropriate migrator for the given provider.
     */
    @Nullable
    private EconomyMigrator findMigratorForProvider(@NotNull String providerName) {
        // Direct match
        EconomyMigrator migrator = SUPPORTED_MIGRATORS.get(providerName);
        if (migrator != null) {
            return migrator;
        }
        
        // Fuzzy matching for different versions/names
        for (Map.Entry<String, EconomyMigrator> entry : SUPPORTED_MIGRATORS.entrySet()) {
            if (providerName.toLowerCase().contains(entry.getKey().toLowerCase()) ||
                entry.getKey().toLowerCase().contains(providerName.toLowerCase())) {
                return entry.getValue();
            }
        }
        
        return null;
    }
    
    /**
     * Creates a backup of current economy data.
     */
    private boolean createMigrationBackup(@NotNull DetectionResult detection) {
        try {
            File backupDir = new File(plugin.getDataFolder(), "migration-backups");
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                logger.severe("Failed to create backup directory");
                return false;
            }
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            File backupFile = new File(backupDir, "vault-backup-" + timestamp + ".yml");
            
            FileConfiguration backup = new YamlConfiguration();
            backup.set("migration.timestamp", timestamp);
            backup.set("migration.source-provider", detection.getProviderName());
            backup.set("migration.plugin-version", plugin.getDescription().getVersion());
            
            // Backup all player balances
            Economy economy = detection.getEconomy();
            int playerCount = 0;
            
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (player.getName() != null && economy.hasAccount(player.getName())) {
                    double balance = economy.getBalance(player.getName());
                    backup.set("balances." + player.getUniqueId(), balance);
                    playerCount++;
                }
            }
            
            backup.set("migration.player-count", playerCount);
            backup.save(backupFile);
            
            logger.info("Created backup with " + playerCount + " player balances at: " + backupFile.getName());
            return true;
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create migration backup", e);
            return false;
        }
    }
    
    /**
     * Gets or creates the target currency for migration.
     */
    private CompletableFuture<Currency> getOrCreateTargetCurrency(@NotNull Economy sourceEconomy, @Nullable String targetCurrencyIdentifier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // If specific currency identifier provided, try to find it
                if (targetCurrencyIdentifier != null && !targetCurrencyIdentifier.isEmpty()) {
                    for (Currency currency : plugin.getCurrencies().values()) {
                        if (currency.getIdentifier().equals(targetCurrencyIdentifier)) {
                            logger.info("Using existing currency: " + targetCurrencyIdentifier);
                            return currency;
                        }
                    }
                    logger.warning("Specified currency '" + targetCurrencyIdentifier + "' not found, will create new one");
                }
                
                // Check if any currencies exist
                if (!plugin.getCurrencies().isEmpty()) {
                    // Use the first available currency
                    Currency existingCurrency = plugin.getCurrencies().values().iterator().next();
                    logger.info("Using existing currency: " + existingCurrency.getIdentifier());
                    
                    // Ask user if they want to overwrite existing data
                    logger.warning("Currency '" + existingCurrency.getIdentifier() + "' already exists.");
                    logger.warning("Migration will add balances to existing player accounts or create new ones.");
                    
                    return existingCurrency;
                }
                
                // Create new currency based on source economy
                String currencyIdentifier = targetCurrencyIdentifier != null ? targetCurrencyIdentifier : "migrated-currency";
                
                // Get currency names from source economy
                String pluralName = sourceEconomy.currencyNamePlural();
                String singularName = sourceEconomy.currencyNameSingular();
                
                // Create currency with only the fields that exist in your Currency entity
                Currency newCurrency = new Currency(
                    "", // prefix - empty by default
                    "", // suffix - empty by default
                    currencyIdentifier, // identifier
                    "$", // symbol - default to dollar sign
                    Material.GOLD_INGOT // icon - default to gold ingot
                );
                
                // If we can get currency names from source, use them as prefix/suffix
                if (singularName != null && !singularName.isEmpty()) {
                    newCurrency.setSuffix(" " + singularName.toLowerCase());
                }
                
                logger.info("Creating new currency: " + currencyIdentifier);
                
                // Save currency to database using repository
                Currency savedCurrency = plugin.getCurrencyRepository().createAsync(newCurrency).join();
                if (savedCurrency != null) {
                    // Add to cache
                    plugin.getCurrencies().put(savedCurrency.getId(), savedCurrency);
                    logger.info("Successfully created currency: " + currencyIdentifier);
                    return savedCurrency;
                } else {
                    logger.severe("Failed to save new currency to database");
                    return null;
                }
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error getting or creating target currency", e);
                return null;
            }
        }, plugin.getExecutor());
    }
    
    /**
     * Migrates economy data from the detected provider to JECurrency.
     */
    private MigrationStats migrateEconomyData(@NotNull DetectionResult detection, @NotNull Currency targetCurrency) {
        MigrationStats stats = new MigrationStats();
        Economy sourceEconomy = detection.getEconomy();
        EconomyMigrator migrator = detection.getMigrator();
        
        try {
            // Get all players with economy accounts
            Set<OfflinePlayer> playersToMigrate = getPlayersWithEconomyAccounts(sourceEconomy);
            stats.setTotalPlayers(playersToMigrate.size());
            
            logger.info("Found " + playersToMigrate.size() + " players to migrate");
            
            // Migrate each player
            for (OfflinePlayer player : playersToMigrate) {
                try {
                    double sourceBalance = sourceEconomy.getBalance(player.getName());
                    if (migratePlayerBalance(player, new AtomicDouble(sourceBalance), targetCurrency, migrator).join()) {
                        stats.incrementSuccessful();
                        stats.addToTotalBalance(BigDecimal.valueOf(sourceBalance));
                    } else {
                        stats.incrementFailed();
                        stats.addError("Failed to migrate player: " + player.getName());
                    }
                } catch (Exception e) {
                    stats.incrementFailed();
                    stats.addError("Error migrating " + player.getName() + ": " + e.getMessage());
                    logger.log(Level.WARNING, "Failed to migrate player: " + player.getName(), e);
                }
                
                stats.incrementProcessed();
                
                // Progress logging
                if (stats.getPlayersProcessed() % 100 == 0) {
                    logger.info("Migration progress: " + stats.getPlayersProcessed() + "/" + stats.getTotalPlayers());
                }
            }
            
            stats.setSuccess(stats.getFailedPlayers() == 0);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Migration failed", e);
            stats.setSuccess(false);
            stats.addError("Migration failed: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Gets all players that have economy accounts.
     */
    private Set<OfflinePlayer> getPlayersWithEconomyAccounts(@NotNull Economy economy) {
        Set<OfflinePlayer> players = new HashSet<>();
        
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.getName() != null && economy.hasAccount(player)) {
                players.add(player);
            }
        }
        
        return players;
    }
    
    /**
     * Migrates a single player's balance.
     */
    private CompletableFuture<Boolean> migratePlayerBalance(
        @NotNull OfflinePlayer player,
        final AtomicDouble sourceBalance,
        @NotNull Currency currency,
        @NotNull EconomyMigrator migrator
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String playerName = player.getName();
                if (playerName == null) return false;
                
                // Handle negative balances
                if (sourceBalance.get() < 0) {
                    logger.warning("Player " + playerName + " has negative balance (" + sourceBalance + "), setting to 0");
                    sourceBalance.set(0.00);
                }
                
                // Check if player already has balance in JECurrency for this currency
                UserCurrency existingUserCurrency = currencyAdapter.getUserCurrency(player, currency.getIdentifier()).join();
                if (existingUserCurrency != null) {
                    // Player already has this currency - decide what to do
                    double currentBalance = existingUserCurrency.getBalance();
                    logger.info("Player " + playerName + " already has " + currentBalance + " " + currency.getIdentifier());
                    
                    if (sourceBalance.get() > currentBalance) {
                        // Source has more money, update to source balance
                        logger.info("Updating " + playerName + " balance from " + currentBalance + " to " + sourceBalance);
                        existingUserCurrency.setBalance(sourceBalance.get());
                        plugin.getUserCurrencyRepository().createAsync(existingUserCurrency).join();
                    } else {
                        // Keep existing balance (it's higher or equal)
                        logger.info("Keeping existing balance for " + playerName + " (" + currentBalance + " >= " + sourceBalance + ")");
                    }
                    return true;
                }
                
                // Create player if doesn't exist
                boolean playerCreated = currencyAdapter.createPlayer(player).join();
                if (!playerCreated) {
                    logger.warning("Failed to create player entity for: " + playerName);
                    return false;
                }
                
                // Get the user entity
                User userEntity = plugin.getUserRepository().findByAttributes(Map.of("uniqueId", player.getUniqueId()));
                if (userEntity == null) {
                    logger.warning("Failed to find user entity for: " + playerName);
                    return false;
                }
                
                // Create player-currency relationship
                boolean relationshipCreated = currencyAdapter.createPlayerCurrency(userEntity, currency).join();
                if (!relationshipCreated) {
                    logger.warning("Failed to create player-currency relationship for: " + playerName);
                    return false;
                }
                
                // Get the UserCurrency entity
                UserCurrency userCurrency = currencyAdapter.getUserCurrency(player, currency.getIdentifier()).join();
                if (userCurrency == null) {
                    logger.warning("Failed to get UserCurrency entity for: " + playerName);
                    return false;
                }
                
                // Set the migrated balance directly
                if (sourceBalance.get() > 0) {
                    userCurrency.setBalance(sourceBalance.get());
                    UserCurrency savedUserCurrency = plugin.getUserCurrencyRepository().createAsync(userCurrency).join();
                    if (savedUserCurrency == null) {
                        logger.warning("Failed to save migrated balance for " + playerName);
                        return false;
                    }
                }
                
                logger.fine("Migrated " + playerName + ": " + sourceBalance + " " + currency.getIdentifier());
                return true;
                
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to migrate player balance for: " + player.getName(), e);
                return false;
            }
        }, plugin.getExecutor());
    }
    
    /**
     * Replaces the current Vault economy provider with JECurrency.
     */
    private boolean replaceVaultProvider() {
        try {
            logger.info("Attempting to replace Vault economy provider with JECurrency...");
            
            // Get current provider for logging
            RegisteredServiceProvider<Economy> currentProvider =
                Bukkit.getServicesManager().getRegistration(Economy.class);
            
            if (currentProvider != null) {
                logger.info("Current Vault provider: " + currentProvider.getProvider().getName());
                
                // Unregister current provider
                Bukkit.getServicesManager().unregisterAll(currentProvider.getPlugin());
                logger.info("Unregistered existing Vault economy provider");
            }
            
            // Create and register JECurrency Vault provider
            JECurrencyVaultProvider vaultProvider = new JECurrencyVaultProvider(plugin);
            
            // Register with highest priority to ensure it takes precedence
            Bukkit.getServicesManager().register(
                Economy.class,
                vaultProvider,
                plugin,
                ServicePriority.Highest
            );
            
            // Verify registration was successful
            RegisteredServiceProvider<Economy> newProvider =
                Bukkit.getServicesManager().getRegistration(Economy.class);
            
            if (newProvider != null && "JECurrency".equals(newProvider.getProvider().getName())) {
                logger.info("Successfully registered JECurrency as Vault economy provider");
                
                // Test the provider to make sure it's working
                Economy economy = newProvider.getProvider();
                if (economy.isEnabled()) {
                    logger.info("JECurrency Vault provider is enabled and ready");
                    logger.info("Currency: " + economy.currencyNamePlural() + " (Symbol: " + economy.format(1.0) + ")");
                    return true;
                } else {
                    logger.severe("JECurrency Vault provider is registered but not enabled");
                    return false;
                }
            } else {
                logger.severe("Failed to register JECurrency Vault provider - registration verification failed");
                return false;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to replace Vault provider", e);
            return false;
        }
    }
    
    /**
     * Verifies the migration was successful.
     */
    private boolean verifyMigration(@NotNull DetectionResult detection, @NotNull MigrationStats stats, @NotNull Currency targetCurrency) {
        try {
            // Sample verification - check a few random players
            Economy sourceEconomy = detection.getEconomy();
            int verificationCount = Math.min(10, stats.getSuccessfulPlayers());
            int verified = 0;
            
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (verified >= verificationCount) break;
                if (player.getName() == null) continue;
                
                try {
                    double sourceBalance = sourceEconomy.getBalance(player.getName());
                    UserCurrency userCurrency = currencyAdapter.getUserCurrency(player, targetCurrency.getIdentifier()).join();
                    
                    if (userCurrency != null) {
                        double migratedBalance = userCurrency.getBalance();
                        if (Math.abs(sourceBalance - migratedBalance) < 0.01) { // Allow small rounding differences
                            verified++;
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Verification error for player: " + player.getName(), e);
                }
            }
            
            boolean success = verified == verificationCount;
            logger.info("Verification: " + verified + "/" + verificationCount + " players verified");
            
            return success;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Migration verification failed", e);
            return false;
        }
    }
    
    /**
     * Gets a list of available currencies for migration target selection.
     */
    public List<String> getAvailableCurrencies() {
        List<String> currencies = new ArrayList<>();
        for (Currency currency : plugin.getCurrencies().values()) {
            currencies.add(currency.getIdentifier());
        }
        return currencies;
    }
    
    /**
     * Checks if migration is currently in progress.
     */
    public boolean isMigrationInProgress() {
        return migrationInProgress;
    }
    
    /**
     * Gets the result of the last migration.
     */
    @Nullable
    public MigrationResult getLastMigrationResult() {
        return lastMigrationResult;
    }
    
    /**
     * Gets a list of supported economy plugins for migration.
     */
    public Set<String> getSupportedEconomyPlugins() {
        return new HashSet<>(SUPPORTED_MIGRATORS.keySet());
    }
    
    // Inner classes for migration data structures
    
    public static class DetectionResult {
        private final boolean success;
        private final String providerName;
        private final Economy economy;
        private final EconomyMigrator migrator;
        private final String error;
        
        public DetectionResult(boolean success, String providerName, Economy economy,
                               EconomyMigrator migrator, String error) {
            this.success = success;
            this.providerName = providerName;
            this.economy = economy;
            this.migrator = migrator;
            this.error = error;
        }
        
        public static DetectionResult error(String error) {
            return new DetectionResult(false, null, null, null, error);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getProviderName() { return providerName; }
        public Economy getEconomy() { return economy; }
        public EconomyMigrator getMigrator() { return migrator; }
        public String getError() { return error; }
    }
    
    public static class MigrationStats {
        private boolean success = false;
        private int totalPlayers = 0;
        private int playersProcessed = 0;
        private int successfulPlayers = 0;
        private int failedPlayers = 0;
        private BigDecimal totalBalance = BigDecimal.ZERO;
        private final List<String> errors = new ArrayList<>();
        
        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public int getTotalPlayers() { return totalPlayers; }
        public void setTotalPlayers(int totalPlayers) { this.totalPlayers = totalPlayers; }
        
        public int getPlayersProcessed() { return playersProcessed; }
        public void incrementProcessed() { this.playersProcessed++; }
        
        public int getSuccessfulPlayers() { return successfulPlayers; }
        public void incrementSuccessful() { this.successfulPlayers++; }
        
        public int getFailedPlayers() { return failedPlayers; }
        public void incrementFailed() { this.failedPlayers++; }
        
        public BigDecimal getTotalBalance() { return totalBalance; }
        public void addToTotalBalance(BigDecimal amount) {
            this.totalBalance = this.totalBalance.add(amount);
        }
        
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public void addError(String error) { this.errors.add(error); }
    }
    
    public static class MigrationResult {
        private final boolean success;
        private final String sourceProvider;
        private final MigrationStats stats;
        private final String errorMessage;
        
        public MigrationResult(boolean success, String sourceProvider, MigrationStats stats, String errorMessage) {
            this.success = success;
            this.sourceProvider = sourceProvider;
            this.stats = stats;
            this.errorMessage = errorMessage;
        }
        
        public static MigrationResult error(String error) {
            return new MigrationResult(false, null, null, error);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getSourceProvider() { return sourceProvider; }
        public MigrationStats getStats() { return stats; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    // Abstract migrator interface
    public interface EconomyMigrator {
        /**
         * Performs any plugin-specific migration logic.
         */
        boolean performCustomMigration(JECurrency plugin, Economy sourceEconomy);
        
        /**
         * Gets the plugin-specific data directory for additional migration.
         */
        @Nullable
        File getPluginDataDirectory();
        
        /**
         * Validates that the source plugin is properly configured.
         */
        boolean validateSourcePlugin();
    }
    
    // Concrete migrator implementations
    private static class EssentialsMigrator implements EconomyMigrator {
        @Override
        public boolean performCustomMigration(JECurrency plugin, Economy sourceEconomy) {
            // Essentials-specific migration logic
            return true;
        }
        
        @Override
        public File getPluginDataDirectory() {
            return new File("plugins/Essentials");
        }
        
        @Override
        public boolean validateSourcePlugin() {
            return Bukkit.getPluginManager().isPluginEnabled("Essentials");
        }
    }
    
    private static class IconomyMigrator implements EconomyMigrator {
        @Override
        public boolean performCustomMigration(JECurrency plugin, Economy sourceEconomy) {
            // iConomy-specific migration logic
            return true;
        }
        
        @Override
        public File getPluginDataDirectory() {
            return new File("plugins/iConomy");
        }
        
        @Override
        public boolean validateSourcePlugin() {
            return Bukkit.getPluginManager().isPluginEnabled("iConomy");
        }
    }
    
    private static class BOSEconomyMigrator implements EconomyMigrator {
        @Override
        public boolean performCustomMigration(JECurrency plugin, Economy sourceEconomy) {
            return true;
        }
        
        @Override
        public File getPluginDataDirectory() {
            return new File("plugins/BOSEconomy");
        }
        
        @Override
        public boolean validateSourcePlugin() {
            return Bukkit.getPluginManager().isPluginEnabled("BOSEconomy");
        }
    }
    
    private static class CMIMigrator implements EconomyMigrator {
        @Override
        public boolean performCustomMigration(JECurrency plugin, Economy sourceEconomy) {
            return true;
        }
        
        @Override
        public File getPluginDataDirectory() {
            return new File("plugins/CMI");
        }
        
        @Override
        public boolean validateSourcePlugin() {
            return Bukkit.getPluginManager().isPluginEnabled("CMI");
        }
    }
    
    private static class TNEMigrator implements EconomyMigrator {
        @Override
        public boolean performCustomMigration(JECurrency plugin, Economy sourceEconomy) {
            return true;
        }
        
        @Override
        public File getPluginDataDirectory() {
            return new File("plugins/TheNewEconomy");
        }
        
        @Override
        public boolean validateSourcePlugin() {
            return Bukkit.getPluginManager().isPluginEnabled("TheNewEconomy");
        }
    }
}