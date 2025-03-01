package de.jexcellence.currency.adapter;

import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.entity.User;
import de.jexcellence.currency.database.entity.UserCurrency;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Adapter class for handling currency operations.
 * Implements the ICurrencyAdapter interface to provide asynchronous currency management.
 */
public class CurrencyAdapter implements ICurrencyAdapter {

	private final JECurrency plugin;

	/**
	 * Constructs a CurrencyAdapter with the specified JECurrency instance.
	 *
	 * @param plugin The JECurrency instance to use for currency operations.
	 */
	public CurrencyAdapter(final @NotNull JECurrency plugin) {
		this.plugin = plugin;
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
		return findUserCurrency(offlinePlayer, currency)
				.thenApplyAsync(userCurrency -> userCurrency != null ? userCurrency.getBalance() : 0.00,
						this.plugin.getExecutor());
	}

	/**
	 * Retrieves the balance of a player currency entity.
	 *
	 * @param userCurrency The player currency entity to get the balance for.
	 * @return A CompletableFuture containing the balance of the player currency entity.
	 */
	@Override
	public CompletableFuture<Double> getBalance(@NotNull UserCurrency userCurrency) {
		return CompletableFuture.supplyAsync(userCurrency::getBalance, this.plugin.getExecutor());
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
		return findUserCurrency(offlinePlayer, currency)
				.thenApplyAsync(userCurrency -> {
					if (userCurrency == null) {
						return createErrorResponse(amount, "Missing Player Account for uuid (" + offlinePlayer.getUniqueId() + ")");
					}
					return performCurrencyOperation(userCurrency, amount, UserCurrency::deposit, "deposited", "to");
				}, this.plugin.getExecutor());
	}

	/**
	 * Deposits a specified amount of currency to a player currency entity.
	 *
	 * @param userCurrency The player currency entity to deposit the currency to.
	 * @param amount       The amount to deposit.
	 * @return A CompletableFuture containing a CurrencyResponse indicating the result of the deposit operation.
	 */
	@Override
	public CompletableFuture<CurrencyResponse> deposit(@NotNull UserCurrency userCurrency, double amount) {
		return CompletableFuture.supplyAsync(() ->
						performCurrencyOperation(userCurrency, amount, UserCurrency::deposit, "deposited", "to"),
				this.plugin.getExecutor());
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
		return findUserCurrency(offlinePlayer, currency)
				.thenApplyAsync(userCurrency -> {
					if (userCurrency == null) {
						return createErrorResponse(amount, "Missing Player Account for uuid (" + offlinePlayer.getUniqueId() + ")");
					}
					return performCurrencyOperation(userCurrency, amount, UserCurrency::withdraw, "withdrawn", "from");
				}, this.plugin.getExecutor());
	}

	/**
	 * Withdraws a specified amount of currency from a player currency entity.
	 *
	 * @param userCurrency The player currency entity to withdraw the currency from.
	 * @param amount       The amount to withdraw.
	 * @return A CompletableFuture containing a CurrencyResponse indicating the result of the withdrawal operation.
	 */
	@Override
	public CompletableFuture<CurrencyResponse> withdraw(@NotNull UserCurrency userCurrency, double amount) {
		return CompletableFuture.supplyAsync(() ->
						performCurrencyOperation(userCurrency, amount, UserCurrency::withdraw, "withdrawn", "from"),
				this.plugin.getExecutor());
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
				this.plugin.getPlatformLogger().logDebug("Offline player's name is not defined for uuid: " + offlinePlayer.getUniqueId());
				return false;
			}
			if (this.plugin.getUserRepository().findByAttributes(Map.of("uniqueId", offlinePlayer.getUniqueId())) != null) {
				return false;
			}
			User user = new User(offlinePlayer.getUniqueId(), offlinePlayer.getName());
			return this.plugin.getUserRepository().create(user) != null;
		}, this.plugin.getExecutor());
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
			if (this.plugin.getCurrencies().values().stream()
					.map(Currency::getIdentifier)
					.anyMatch(currencyName -> currencyName.equals(currency.getIdentifier()))) {
				return false;
			}
			Currency created = this.plugin.getCurrencyRepository().create(currency);
			if (created != null) {
				this.plugin.getCurrencies().put(created.getId(), created);
				return true;
			}
			return false;
		}, this.plugin.getExecutor());
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
						this.plugin.getCurrencyRepository().findByAttributes(Map.of("identifier", currencyName)) != null,
				this.plugin.getExecutor()
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
			UserCurrency existing = this.plugin.getUsercurrencyRepository()
					.findByAttributes(Map.of("player.uniqueId", player.getUniqueId(), "currency", currency));

			if (existing != null) {
				return false;
			}

			UserCurrency created = this.plugin.getUsercurrencyRepository().create(new UserCurrency(player, currency));
			return created != null;
		}, this.plugin.getExecutor());
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
						this.plugin.getUsercurrencyRepository().findListByAttributes(Map.of("player.uniqueId", offlinePlayer.getUniqueId())),
				this.plugin.getExecutor()
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
			try {
				List<UserCurrency> userCurrencies = this.plugin.getUsercurrencyRepository().findListByAttributes(Map.of("player.uniqueId", offlinePlayer.getUniqueId()));

				return userCurrencies.stream()
						.filter(uc -> uc.getCurrency() != null && currencyName.equals(uc.getCurrency().getIdentifier()))
						.findFirst()
						.orElse(null);
			} catch (Exception e) {
				this.plugin.getPlatformLogger().logDebug("Failed to get currency for " + offlinePlayer.getUniqueId() + ", name: " + currencyName, e);
				return null;
			}
		}, this.plugin.getExecutor()).exceptionallyAsync(
				throwable -> {
					this.plugin.getPlatformLogger().logDebug("Failed to get currency for " + offlinePlayer.getUniqueId() + ", name: " + currencyName);
					return null;
				}
		, this.plugin.getExecutor());
	}

	/**
	 * Finds a UserCurrency for a player and currency.
	 *
	 * @param player The player to find the currency for
	 * @param currency The currency to find
	 * @return A CompletableFuture containing the UserCurrency if found
	 */
	private CompletableFuture<UserCurrency> findUserCurrency(
			@NotNull OfflinePlayer player,
			@NotNull Currency currency) {

		return CompletableFuture.supplyAsync(() ->
						this.plugin.getUsercurrencyRepository()
								.findByAttributes(Map.of("player.uniqueId", player.getUniqueId(), "currency", currency)),
				this.plugin.getExecutor());
	}

	/**
	 * Creates an error response with the given message.
	 *
	 * @param amount The amount involved in the operation
	 * @param errorMessage The error message to include
	 * @return A CurrencyResponse with failure status
	 */
	private CurrencyResponse createErrorResponse(double amount, String errorMessage) {
		CurrencyResponse response = new CurrencyResponse(
				amount,
				0.0,
				CurrencyResponse.ResponseType.FAILURE,
				errorMessage
		);
		this.plugin.getPlatformLogger().logDebug(errorMessage);
		return response;
	}

	/**
	 * Generic method to perform currency operations (deposit/withdraw) with consistent response handling.
	 *
	 * @param userCurrency The user currency to operate on
	 * @param amount The amount to process
	 * @param operation The operation function (deposit or withdraw)
	 * @param actionVerb The verb describing the action (e.g., "deposited", "withdrawn")
	 * @param preposition The preposition for the message (e.g., "to", "from")
	 * @return A CurrencyResponse indicating the result
	 */
	private CurrencyResponse performCurrencyOperation(
			UserCurrency userCurrency,
			double amount,
			BiFunction<UserCurrency, Double, Boolean> operation,
			String actionVerb,
			String preposition) {

		if (operation.apply(userCurrency, amount)) {
			this.plugin.getUsercurrencyRepository().update(userCurrency);
			String successMessage = "Successfully " + actionVerb + " " + amount + " " +
					preposition + " " + userCurrency.getCurrency().getIdentifier();

			CurrencyResponse response = new CurrencyResponse(
					amount,
					userCurrency.getBalance(),
					CurrencyResponse.ResponseType.SUCCESS,
					successMessage
			);
			this.plugin.getPlatformLogger().logDebug(successMessage);
			return response;
		}

		String failMessage = "Failed to " + actionVerb.replace("ed", "") + " " + amount + " " +
				preposition + " " + userCurrency.getCurrency().getIdentifier();

		CurrencyResponse response = new CurrencyResponse(
				amount,
				userCurrency.getBalance(),
				CurrencyResponse.ResponseType.FAILURE,
				failMessage
		);
		this.plugin.getPlatformLogger().logDebug(failMessage);
		return response;
	}
}