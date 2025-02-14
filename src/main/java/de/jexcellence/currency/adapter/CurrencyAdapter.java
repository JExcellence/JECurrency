package de.jexcellence.currency.adapter;

import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.entity.User;
import de.jexcellence.currency.database.entity.UserCurrency;
import de.jexcellence.currency.database.repository.UserCurrencyRepository;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter class for handling currency operations.
 * Implements the ICurrencyAdapter interface to provide asynchronous currency management.
 */
public class CurrencyAdapter implements ICurrencyAdapter {

	private final JECurrency currency;

	/**
	 * Constructs a CurrencyAdapter with the specified JECurrency instance.
	 *
	 * @param currency The JECurrency instance to use for currency operations.
	 */
	public CurrencyAdapter(final @NotNull JECurrency currency) {
		this.currency = currency;
	}

	/**
	 * Retrieves the balance of an offline player for a specific currency.
	 *
	 * @param offlinePlayer The offline player to get the balance for.
	 * @param currency      The currency to get the balance in.
	 * @return A CompletableFuture containing the balance of the player in the specified currency.
	 */
	@Override
	public CompletableFuture<Double> getBalance(@NotNull OfflinePlayer offlinePlayer, @NotNull Currency currency) {
		return this.currency.getUsercurrencyRepository()
			.findByUniqueIdAndCurrencyAsync(offlinePlayer.getUniqueId(), currency)
			.thenApplyAsync(usercurrency -> usercurrency == null ? 0.00 : usercurrency.getBalance(), this.currency.getExecutor());
	}

	/**
	 * Retrieves the balance of a player currency entity.
	 *
	 * @param usercurrency The player currency entity to get the balance for.
	 * @return A CompletableFuture containing the balance of the player currency entity.
	 */
	@Override
	public CompletableFuture<Double> getBalance(@NotNull UserCurrency usercurrency) {
		return CompletableFuture.supplyAsync(usercurrency::getBalance, this.currency.getExecutor());
	}

	/**
	 * Deposits a specified amount of currency to an offline player.
	 *
	 * @param offlinePlayer The offline player to deposit the currency to.
	 * @param currency      The currency to deposit.
	 * @param amount        The amount to deposit.
	 * @return A CompletableFuture containing a CurrencyResponse indicating the result of the deposit operation.
	 */
	@Override
	public CompletableFuture<CurrencyResponse> deposit(@NotNull OfflinePlayer offlinePlayer, @NotNull Currency currency, double amount) {
		return this.currency.getUsercurrencyRepository()
			.findByUniqueIdAndCurrencyAsync(offlinePlayer.getUniqueId(), currency)
			.thenApplyAsync(usercurrency -> {
				if (usercurrency == null) {
					CurrencyResponse currencyResponse = new CurrencyResponse(amount, 0.00, CurrencyResponse.ResponseType.FAILURE, "Missing Player Account for uuid (" + offlinePlayer.getUniqueId() + ")");
					this.currency.getPlatformLogger().logDebug(currencyResponse.errorMessage());
					return currencyResponse;
				}
				return this.deposit(amount, usercurrency);
			}, this.currency.getExecutor());
	}

	/**
	 * Deposits a specified amount of currency to a player currency entity.
	 *
	 * @param usercurrency The player currency entity to deposit the currency to.
	 * @param amount       The amount to deposit.
	 * @return A CompletableFuture containing a CurrencyResponse indicating the result of the deposit operation.
	 */
	@Override
	public CompletableFuture<CurrencyResponse> deposit(@NotNull UserCurrency usercurrency, double amount) {
		return CompletableFuture.supplyAsync(() -> this.deposit(amount, usercurrency), this.currency.getExecutor());
	}

	/**
	 * Withdraws a specified amount of currency from an offline player.
	 *
	 * @param offlinePlayer The offline player to withdraw the currency from.
	 * @param currency      The currency to withdraw.
	 * @param amount        The amount to withdraw.
	 * @return A CompletableFuture containing a CurrencyResponse indicating the result of the withdrawal operation.
	 */
	@Override
	public CompletableFuture<CurrencyResponse> withdraw(@NotNull OfflinePlayer offlinePlayer, @NotNull Currency currency, double amount) {
		return this.currency.getUsercurrencyRepository()
			.findByUniqueIdAndCurrencyAsync(offlinePlayer.getUniqueId(), currency)
			.thenApplyAsync(usercurrency -> {
				if (usercurrency == null) {
					CurrencyResponse currencyResponse = new CurrencyResponse(amount, 0.00, CurrencyResponse.ResponseType.FAILURE, "Missing Player Account for uuid (" + offlinePlayer.getUniqueId() + ")");
					this.currency.getPlatformLogger().logDebug(currencyResponse.errorMessage());
					return currencyResponse;
				}
				return this.withdraw(amount, usercurrency);
			}, this.currency.getExecutor());
	}

	/**
	 * Withdraws a specified amount of currency from a player currency entity.
	 *
	 * @param usercurrency The player currency entity to withdraw the currency from.
	 * @param amount       The amount to withdraw.
	 * @return A CompletableFuture containing a CurrencyResponse indicating the result of the withdrawal operation.
	 */
	@Override
	public CompletableFuture<CurrencyResponse> withdraw(@NotNull UserCurrency usercurrency, double amount) {
		return CompletableFuture.supplyAsync(() -> this.withdraw(amount, usercurrency), this.currency.getExecutor());
	}

	/**
	 * Creates a player entity for the given offline player.
	 *
	 * @param offlinePlayer The offline player to create a player entity for.
	 * @return A CompletableFuture containing true if the player entity was successfully created, false otherwise.
	 */
	@Override
	public CompletableFuture<Boolean> createPlayer(@NotNull OfflinePlayer offlinePlayer) {
		return CompletableFuture.supplyAsync(() -> {
			if (offlinePlayer.getName() == null) {
				this.currency.getPlatformLogger().logDebug("Offline player's name is not defined for uuid: " + offlinePlayer.getUniqueId());
				return false;
			}
			if (this.currency.getUserRepository().findByUniqueId(offlinePlayer.getUniqueId()) != null)
				return false;
			this.currency.getUserRepository().create(new User(offlinePlayer.getUniqueId(), offlinePlayer.getName()));
			return true;
		}, this.currency.getExecutor());
	}

	/**
	 * Creates a currency entity.
	 *
	 * @param currency The currency entity to create.
	 * @return A CompletableFuture containing true if the currency entity was successfully created, false otherwise.
	 */
	@Override
	public CompletableFuture<Boolean> createCurrency(@NotNull Currency currency) {
		return CompletableFuture.supplyAsync(() -> {
			if (this.currency.getCurrencies().values().stream().map(Currency::getIdentifier).anyMatch(currencyName -> currencyName.equals(currency.getIdentifier())))
				return false;
			this.currency.getCurrencyRepository().create(currency);
			return true;
		}, this.currency.getExecutor());
	}

	/**
	 * Checks if a currency with the given name exists.
	 *
	 * @param currencyName The name of the currency to check.
	 * @return A CompletableFuture containing true if the currency exists, false otherwise.
	 */
	@Override
	public CompletableFuture<Boolean> hasGivenCurrency(String currencyName) {
		return CompletableFuture.supplyAsync(() ->
				this.currency.getCurrencyRepository().findByAttributes(Map.of("identifier", currencyName)) != null,
			this.currency.getExecutor()
		);
	}

	/**
	 * Creates a player currency entity for the given player and currency.
	 *
	 * @param player   The player entity to associate with the player currency entity.
	 * @param currency The currency entity to associate with the player currency entity.
	 * @return A CompletableFuture containing true if the player currency entity was successfully created, false otherwise.
	 */
	@Override
	public CompletableFuture<Boolean> createPlayerCurrency(@NotNull User player, @NotNull Currency currency) {
		return CompletableFuture.supplyAsync(() -> {
			if (this.currency.getUsercurrencyRepository().findByUniqueIdAndCurrency(player.getUniqueId(), currency) != null)
				return false;
			this.currency.getUsercurrencyRepository().create(new UserCurrency(player, currency));
			return true;
		}, this.currency.getExecutor());
	}

	/**
	 * Retrieves all player currency entities for the given offline player.
	 *
	 * @param offlinePlayer The offline player for which to retrieve all associated currencies.
	 * @return A CompletableFuture containing a list of UserCurrency associated with the offline player.
	 */
	@Override
	public CompletableFuture<List<UserCurrency>> getUserCurrencies(@NotNull OfflinePlayer offlinePlayer) {
		return CompletableFuture.supplyAsync(() ->
				this.currency.getUsercurrencyRepository().findAllByUniqueId(offlinePlayer.getUniqueId()),
			this.currency.getExecutor()
		);
	}

	/**
	 * Retrieves a specific player currency entity for the given offline player based on the currency name.
	 *
	 * @param offlinePlayer The offline player for which to retrieve the currency.
	 * @param currencyName  The name of the currency to retrieve.
	 * @return A CompletableFuture containing the UserCurrency matching the currency name associated with the offline player,
	 *         or null if not found.
	 */
	@Override
	public CompletableFuture<UserCurrency> getUserCurrency(@NotNull OfflinePlayer offlinePlayer, String currencyName) {
		return CompletableFuture.supplyAsync(() -> {
			List<UserCurrency> userCurrencies = this.currency.getUsercurrencyRepository().findAllByUniqueId(offlinePlayer.getUniqueId());
			return userCurrencies.stream()
				.filter(uc -> uc.getCurrency() != null && currencyName.equals(uc.getCurrency().getIdentifier()))
				.findFirst()
				.orElse(null);
		}, this.currency.getExecutor());
	}

	/**
	 * Helper method to deposit an amount to a player currency entity.
	 *
	 * @param amount       The amount to deposit.
	 * @param usercurrency The player currency entity to deposit the amount to.
	 * @return A CurrencyResponse indicating the result of the deposit operation.
	 */
	private CurrencyResponse deposit(double amount, UserCurrency usercurrency) {
		if (usercurrency.deposit(amount)) {
			this.currency.getUsercurrencyRepository().update(usercurrency);
			CurrencyResponse currencyResponse = new CurrencyResponse(amount, usercurrency.getBalance(), CurrencyResponse.ResponseType.SUCCESS, "Successfully deposited " + amount + " to " + usercurrency.getCurrency());
			this.currency.getPlatformLogger().logDebug(currencyResponse.errorMessage());
			return currencyResponse;
		}
		CurrencyResponse currencyResponse = new CurrencyResponse(amount, usercurrency.getBalance(), CurrencyResponse.ResponseType.FAILURE, "Failed to deposit " + amount + " to " + usercurrency.getCurrency());
		this.currency.getPlatformLogger().logDebug(currencyResponse.errorMessage());
		return currencyResponse;
	}

	/**
	 * Helper method to withdraw an amount from a player currency entity.
	 *
	 * @param amount       The amount to withdraw.
	 * @param usercurrency The player currency entity to withdraw the amount from.
	 * @return A CurrencyResponse indicating the result of the withdrawal operation.
	 */
	private CurrencyResponse withdraw(double amount, UserCurrency usercurrency) {
		if (usercurrency.withdraw(amount)) {
			this.currency.getUsercurrencyRepository().update(usercurrency);
			CurrencyResponse currencyResponse = new CurrencyResponse(amount, usercurrency.getBalance(), CurrencyResponse.ResponseType.SUCCESS, "Successfully withdrawn " + amount + " from " + usercurrency.getCurrency());
			this.currency.getPlatformLogger().logDebug(currencyResponse.errorMessage());
			return currencyResponse;
		}
		CurrencyResponse currencyResponse = new CurrencyResponse(amount, usercurrency.getBalance(), CurrencyResponse.ResponseType.FAILURE, "Failed to withdrawn " + amount + " from " + usercurrency.getCurrency());
		this.currency.getPlatformLogger().logDebug(currencyResponse.errorMessage());
		return currencyResponse;
	}
}