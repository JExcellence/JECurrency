package de.jexcellence.currency.placeholder;

import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.entity.UserCurrency;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for handling currency-related placeholders.
 * Works with the existing Placeholder system to provide formatted currency information.
 */
public class CurrencyPlaceholderUtil {

    private final JECurrency plugin;
    private final NumberFormat germanNumberFormat;
    private final NumberFormat decimalFormat;

    public CurrencyPlaceholderUtil(@NotNull JECurrency plugin) {
        this.plugin = plugin;

        this.germanNumberFormat = NumberFormat.getInstance(Locale.GERMANY);
        this.germanNumberFormat.setMaximumFractionDigits(0);
        
        this.decimalFormat = NumberFormat.getInstance();
        this.decimalFormat.setMinimumFractionDigits(2);
        this.decimalFormat.setMaximumFractionDigits(2);
    }

    /**
     * Gets currency information based on the provided identifier.
     *
     * @param identifier The currency identifier
     * @param infoType The type of information to retrieve (name, symbol, currencies)
     * @return The requested currency information or empty string if not found
     */
    public String getCurrencyInfo(@NotNull String identifier, @NotNull String infoType) {
        Map<Long, Currency> currencies = this.plugin.getCurrencies();
        
        Optional<Currency> currency = currencies.values().stream()
            .filter(c -> c.getIdentifier().equalsIgnoreCase(identifier))
            .findFirst();
            
        return currency.map(c -> switch (infoType) {
            case "name" -> c.getIdentifier();
            case "symbol" -> c.getSymbol();
            case "currencies" -> c.getPrefix() + c.getIdentifier() + c.getSuffix();
            case "prefix" -> c.getPrefix();
            case "suffix" -> c.getSuffix();
            default -> "";
        }).orElse("");
    }

    /**
     * Gets a player's currency balance with various formatting options.
     *
     * @param playerId The UUID of the player
     * @param currencyIdentifier The currency identifier
     * @param format The format type (amount, amount-rounded, amount-rounded-dots)
     * @return The formatted balance or "N/A" if not found
     */
    public String getPlayerBalance(@NotNull UUID playerId, @NotNull String currencyIdentifier, @NotNull String format) {
        Optional<Currency> currency = this.plugin.getCurrencies().values().stream()
            .filter(c -> c.getIdentifier().equalsIgnoreCase(currencyIdentifier))
            .findFirst();
            
        if (currency.isEmpty()) {
            return "N/A";
        }
        
        UserCurrency userCurrency = this.plugin.getUsercurrencyRepository().findByAttributes(Map.of("player.uniqueId", playerId, "currency", currency.get().getIdentifier()));
            
        if (userCurrency == null) {
            return "N/A";
        }
        
        double balance = userCurrency.getBalance();
        
        return switch (format.toLowerCase()) {
            case "amount" -> this.decimalFormat.format(balance);
            case "amount-rounded" -> String.format("%.0f", balance);
            case "amount-rounded-dots" -> this.germanNumberFormat.format(balance);
            default -> String.format("%.2f", balance);
        };
    }

    /**
     * Gets a fully formatted player balance with currency symbols and formatting.
     *
     * @param playerId The UUID of the player
     * @param currencyIdentifier The currency identifier
     * @param format The format type (amount, amount-rounded, amount-rounded-dots)
     * @return The fully formatted balance string or "N/A" if not found
     */
    public String getFormattedPlayerBalance(@NotNull UUID playerId, @NotNull String currencyIdentifier, @NotNull String format) {
        Optional<Currency> currency = this.plugin.getCurrencies().values().stream()
            .filter(c -> c.getIdentifier().equalsIgnoreCase(currencyIdentifier))
            .findFirst();
            
        if (currency.isEmpty()) {
            return "N/A";
        }
        
        String balanceStr = getPlayerBalance(playerId, currencyIdentifier, format);
        if (balanceStr.equals("N/A")) {
            return "N/A";
        }
        
        Currency curr = currency.get();
        return curr.getPrefix() + balanceStr + curr.getSymbol() + curr.getSuffix();
    }

    /**
     * Processes a placeholder for a specific player.
     * This method can be used directly from the Placeholder class.
     *
     * @param player The player
     * @param params The placeholder parameters
     * @return The processed placeholder value or null if not applicable
     */
    public @Nullable String processPlaceholder(@Nullable Player player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        
        if (params.startsWith("currency_")) {
            String[] parts = params.split("_");
            if (parts.length < 3) {
                return null;
            }
            
            String currencyIdentifier = parts[1];
            String infoType = parts[2];
            
            return getCurrencyInfo(currencyIdentifier, infoType);
        }
        
        if (params.startsWith("player_currency_")) {
            String[] parts = params.split("_");
            if (parts.length < 4) {
                return null;
            }
            
            String currencyIdentifier = parts[2];
            String format = parts[3];
            
            return getPlayerBalance(player.getUniqueId(), currencyIdentifier, format);
        }
        
        if (params.startsWith("player_formatted_currency_")) {
            String[] parts = params.split("_");
            if (parts.length < 4) {
                return null;
            }
            
            String currencyIdentifier = parts[3];
            String format = parts.length > 4 ? parts[4] : "amount";
            
            return getFormattedPlayerBalance(player.getUniqueId(), currencyIdentifier, format);
        }
        
        return null;
    }

    /**
     * Gets the top players for a specific currency.
     *
     * @param currencyIdentifier The currency identifier
     * @param limit The maximum number of players to return
     * @return A future that will complete with a map of player names to their balances
     */
    public CompletableFuture<Map<String, Double>> findTopByCurrency(@NotNull String currencyIdentifier, int limit) {
        return CompletableFuture.completedFuture(new HashMap<>());
    }

    /**
     * Formats a raw balance amount according to the specified format.
     *
     * @param amount The raw balance amount
     * @param format The format type (amount, amount-rounded, amount-rounded-dots)
     * @return The formatted balance string
     */
    public String formatAmount(double amount, @NotNull String format) {
        return switch (format.toLowerCase()) {
            case "amount" -> this.decimalFormat.format(amount);
            case "amount-rounded" -> String.format("%.0f", amount);
            case "amount-rounded-dots" -> this.germanNumberFormat.format(amount);
            default -> String.format("%.2f", amount);
        };
    }

    /**
     * Gets the total amount of a specific currency in circulation.
     *
     * @param currencyIdentifier The currency identifier
     * @return A future that will complete with the total amount
     */
    public CompletableFuture<Double> getTotalCurrencyInCirculation(@NotNull String currencyIdentifier) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<Currency> currency = this.plugin.getCurrencies().values().stream()
                .filter(c -> c.getIdentifier().equalsIgnoreCase(currencyIdentifier))
                .findFirst();
                
            if (currency.isEmpty()) {
                return 0.0;
            }

            return 0.0;
            //return this.plugin.getUsercurrencyRepository().getTotalAmountByCurrency(currency.get());
        }, this.plugin.getExecutor());
    }
}