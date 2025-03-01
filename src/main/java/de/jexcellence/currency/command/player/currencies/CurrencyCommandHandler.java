package de.jexcellence.currency.command.player.currencies;

import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.je18n.i18n.I18n;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Handles currency-related commands for creating, deleting, and managing currencies
 * without relying on UI components.
 */
public class CurrencyCommandHandler {

    private final JECurrency plugin;

    public CurrencyCommandHandler(@NotNull JECurrency plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a new currency with the specified parameters.
     *
     * @param player     The player executing the command
     * @param args       Command arguments: [identifier] [symbol] [prefix] [suffix]
     */
    public void createCurrency(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 3) {
            sendUsage(player, "create <identifier> <symbol> [prefix] [suffix]");
            return;
        }

        String identifier = args[1];
        String symbol = args[2];
        String prefix = args.length > 3 ? args[3] : "";
        String suffix = args.length > 4 ? args[4] : "";

        this.plugin.getCurrencyAdapter().hasGivenCurrency(identifier)
                .thenAcceptAsync(exists -> {
                    if (exists) {
                        new I18n.Builder("currency.create.already_exists", player)
                                .includingPrefix()
                                .withPlaceholder("identifier", identifier)
                                .build()
                                .send();
                        return;
                    }

                    Currency currency = new Currency(prefix, suffix, identifier, symbol);
                    this.plugin.getCurrencyAdapter().createCurrency(currency)
                            .thenAcceptAsync(success -> {
                                if (success) {
                                    new I18n.Builder("currency.create.success", player)
                                            .includingPrefix()
                                            .withPlaceholder("identifier", identifier)
                                            .build()
                                            .send();
                                } else {
                                    new I18n.Builder("currency.create.failed", player)
                                            .includingPrefix()
                                            .build()
                                            .send();
                                }
                            }, this.plugin.getExecutor());
                }, this.plugin.getExecutor());
    }

    /**
     * Deletes an existing currency.
     *
     * @param player     The player executing the command
     * @param args       Command arguments: [identifier]
     */
    public void deleteCurrency(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            sendUsage(player, "delete <identifier>");
            return;
        }

        String identifier = args[1];

        this.plugin.getCurrencyAdapter().hasGivenCurrency(identifier)
                .thenAcceptAsync(exists -> {
                    if (!exists) {
                        new I18n.Builder("currency.delete.not_found", player)
                                .includingPrefix()
                                .withPlaceholder("identifier", identifier)
                                .build()
                                .send();
                        return;
                    }

                    CompletableFuture.supplyAsync(() -> {
                        Currency currency = this.plugin.getCurrencyRepository().findByAttributes(java.util.Map.of("identifier", identifier));
                        if (currency != null) {
                            this.plugin.getCurrencyRepository().delete(currency.getId());
                            return true;
                        }
                        return false;
                    }, this.plugin.getExecutor()).thenAcceptAsync(success -> {
                        if (success) {
                            new I18n.Builder("currency.delete.success", player)
                                    .includingPrefix()
                                    .withPlaceholder("identifier", identifier)
                                    .build()
                                    .send();
                        } else {
                            new I18n.Builder("currency.delete.failed", player)
                                    .includingPrefix()
                                    .build()
                                    .send();
                        }
                    }, this.plugin.getExecutor());
                }, this.plugin.getExecutor());
    }

    /**
     * Lists all available currencies.
     *
     * @param player The player executing the command
     */
    public void listCurrencies(@NotNull Player player) {
        CompletableFuture.supplyAsync(() -> 
            this.plugin.getCurrencyRepository().findAll(0, 128),
            this.plugin.getExecutor()
        ).thenAcceptAsync(currencies -> {
            if (currencies.isEmpty()) {
                new I18n.Builder("currency.list.empty", player)
                        .includingPrefix()
                        .build()
                        .send();
                return;
            }

            new I18n.Builder("currency.list.header", player)
                    .includingPrefix()
                    .withPlaceholder("count", String.valueOf(currencies.size()))
                    .build()
                    .send();

            for (Currency currency : currencies) {
                new I18n.Builder("currency.list.entry", player)
                        .includingPrefix()
                        .withPlaceholder("identifier", currency.getIdentifier())
                        .withPlaceholder("symbol", currency.getSymbol())
                        .withPlaceholder("prefix", currency.getPrefix())
                        .withPlaceholder("suffix", currency.getSuffix())
                        .build()
                        .send();
            }
        }, this.plugin.getExecutor());
    }

    /**
     * Edits an existing currency.
     *
     * @param player The player executing the command
     * @param args   Command arguments: [identifier] [field] [value]
     */
    public void editCurrency(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 4) {
            sendUsage(player, "edit <identifier> <field> <value>");
            sendEditFields(player);
            return;
        }

        String identifier = args[1];
        String field = args[2].toLowerCase();
        String value = args[3];

        this.plugin.getCurrencyAdapter().hasGivenCurrency(identifier)
                .thenAcceptAsync(exists -> {
                    if (!exists) {
                        new I18n.Builder("currency.edit.not_found", player)
                                .includingPrefix()
                                .withPlaceholder("identifier", identifier)
                                .build()
                                .send();
                        return;
                    }

                    CompletableFuture.supplyAsync(() -> {
                        Currency currency = this.plugin.getCurrencyRepository().findByAttributes(
                                java.util.Map.of("identifier", identifier));
                        if (currency != null) {
                            switch (field) {
                                case "symbol" -> currency.setSymbol(value);
                                case "prefix" -> currency.setPrefix(value);
                                case "suffix" -> currency.setSuffix(value);
                                case "identifier" -> {
                                    if (this.plugin.getCurrencyRepository().findByAttributes(
                                            java.util.Map.of("identifier", value)) != null) {
                                        return false;
                                    }
                                    currency.setIdentifier(value);
                                }
                                default -> {
                                    return false;
                                }
                            }
                            this.plugin.getCurrencyRepository().update(currency);
                            return true;
                        }
                        return false;
                    }, this.plugin.getExecutor()).thenAcceptAsync(success -> {
                        if (success) {
                            new I18n.Builder("currency.edit.success", player)
                                    .includingPrefix()
                                    .withPlaceholder("identifier", identifier)
                                    .withPlaceholder("field", field)
                                    .withPlaceholder("value", value)
                                    .build()
                                    .send();
                        } else {
                            new I18n.Builder("currency.edit.failed", player)
                                    .includingPrefix()
                                    .build()
                                    .send();
                        }
                    }, this.plugin.getExecutor());
                }, this.plugin.getExecutor());
    }

    /**
     * Shows detailed information about a specific currency.
     *
     * @param player The player executing the command
     * @param args   Command arguments: [identifier]
     */
    public void showCurrencyInfo(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            sendUsage(player, "info <identifier>");
            return;
        }

        String identifier = args[1];

        CompletableFuture.supplyAsync(() -> 
            this.plugin.getCurrencyRepository().findByAttributes(java.util.Map.of("identifier", identifier)),
            this.plugin.getExecutor()
        ).thenAcceptAsync(currency -> {
            if (currency == null) {
                new I18n.Builder("currency.info.not_found", player)
                        .includingPrefix()
                        .withPlaceholder("identifier", identifier)
                        .build()
                        .send();
                return;
            }

            new I18n.Builder("currency.info.header", player)
                    .includingPrefix()
                    .withPlaceholder("identifier", currency.getIdentifier())
                    .build()
                    .send();

            new I18n.Builder("currency.info.details", player)
                    .includingPrefix()
                    .withPlaceholder("symbol", currency.getSymbol())
                    .withPlaceholder("prefix", currency.getPrefix())
                    .withPlaceholder("suffix", currency.getSuffix())
                    .build()
                    .send();
        }, this.plugin.getExecutor());
    }

    private void sendUsage(@NotNull Player player, String usage) {
        new I18n.Builder("currency.command.usage", player)
                .includingPrefix()
                .withPlaceholder("usage", usage)
                .build()
                .send();
    }

    private void sendEditFields(@NotNull Player player) {
        new I18n.Builder("currency.edit.fields", player)
                .includingPrefix()
                .build()
                .send();
    }

    /**
     * Gets a list of available currency identifiers for tab completion.
     *
     * @return A list of currency identifiers
     */
    public CompletableFuture<List<String>> getCurrencyIdentifiers() {
        return CompletableFuture.supplyAsync(() -> 
            this.plugin.getCurrencyRepository().findAll(0, 128).stream()
                .map(Currency::getIdentifier)
                .toList(),
            this.plugin.getExecutor()
        );
    }

    /**
     * Gets a list of editable fields for tab completion.
     *
     * @return A list of editable fields
     */
    public List<String> getEditableFields() {
        return Arrays.asList("symbol", "prefix", "suffix", "identifier");
    }
}