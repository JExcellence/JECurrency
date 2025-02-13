package de.jexcellence.currency.adapter;

import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.entity.User;
import de.jexcellence.currency.database.entity.UserCurrency;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for handling currency operations.
 */
public interface ICurrencyAdapter {
	
	/**
	 * Get the balance of an offline player for a specific currency.
	 *
	 * @param offlinePlayer The offline player to get the balance for.
	 * @param currency      The currency to get the balance in.
	 * @return The balance of the player in the specified currency.
	 */
	CompletableFuture<Double> getBalance(@NotNull OfflinePlayer offlinePlayer, @NotNull Currency currency);
	
	/**
	 * Get the balance of a player currency entity.
	 *
	 * @param usercurrency The player currency entity to get the balance for.
	 * @return The balance of the player currency entity.
	 */
	CompletableFuture<Double> getBalance(@NotNull UserCurrency usercurrency);
	
	/**
	 * Deposit a specified amount of currency to an offline player.
	 *
	 * @param offlinePlayer The offline player to deposit the currency to.
	 * @param pCurrency     The currency to deposit.
	 * @param amount        The amount to deposit.
	 * @return A `CurrencyResponse` indicating the result of the deposit operation.
	 */
	CompletableFuture<CurrencyResponse> deposit(@NotNull OfflinePlayer offlinePlayer, @NotNull Currency pCurrency, double amount);
	
	/**
	 * Deposit a specified amount of currency to a player currency entity.
	 *
	 * @param usercurrency The player currency entity to deposit the currency to.
	 * @param amount          The amount to deposit.
	 * @return A `CurrencyResponse` indicating the result of the deposit operation.
	 */
	CompletableFuture<CurrencyResponse> deposit(@NotNull UserCurrency usercurrency, double amount);
	
	/**
	 * Withdraw a specified amount of currency from an offline player.
	 *
	 * @param offlinePlayer The offline player to withdraw the currency from.
	 * @param currency     The currency to withdraw.
	 * @param amount        The amount to withdraw.
	 * @return A `CurrencyResponse` indicating the result of the withdrawal operation.
	 */
	CompletableFuture<CurrencyResponse> withdraw(@NotNull OfflinePlayer offlinePlayer, @NotNull Currency currency, double amount);
	
	/**
	 * Withdraw a specified amount of currency from a player currency entity.
	 *
	 * @param usercurrency The player currency entity to withdraw the currency from.
	 * @param amount          The amount to withdraw.
	 * @return A `CurrencyResponse` indicating the result of the withdrawal operation.
	 */
	CompletableFuture<CurrencyResponse> withdraw(@NotNull UserCurrency usercurrency, double amount);
	
	/**
	 * Create a player entity for the given offline player.
	 *
	 * @param offlinePlayer The offline player to create a player entity for.
	 * @return `true` if the player entity was successfully created, `false` otherwise.
	 */
	CompletableFuture<Boolean> createPlayer(@NotNull OfflinePlayer offlinePlayer);
	
	/**
	 * Create a currency entity.
	 *
	 * @param pCurrency The currency entity to create.
	 * @return `true` if the currency entity was successfully created, `false` otherwise.
	 */
	CompletableFuture<Boolean> createCurrency(@NotNull Currency pCurrency);
	
	
	CompletableFuture<Boolean> hasGivenCurrency(String currencyName);
	
	/**
	 * Create a player currency entity for the given player and currency.
	 *
	 * @param pPlayer  The player entity to associate with the player currency entity.
	 * @param currency The currency entity to associate with the player currency entity.
	 * @return `true` if the player currency entity was successfully created, `false` otherwise.
	 */
	CompletableFuture<Boolean> createPlayerCurrency(@NotNull User pPlayer, @NotNull Currency currency);
}