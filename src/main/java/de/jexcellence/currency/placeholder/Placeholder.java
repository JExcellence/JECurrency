package de.jexcellence.currency.placeholder;

import com.raindropcentral.rplatform.placeholder.APlaceholder;
import de.jexcellence.currency.JECurrency;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Placeholder expansion for currency-related placeholders in the JECurrency system.
 * <p>
 * This class integrates with the platform's placeholder system to provide dynamic
 * currency and player balance information for use in chat, scoreboards, and other
 * placeholder-aware components. It delegates placeholder resolution to
 * {@link CurrencyPlaceholderUtil} and defines the supported placeholder formats.
 * </p>
 *
 * <h3>Supported Placeholder Categories:</h3>
 * <ul>
 *   <li><strong>Currency Information:</strong> Basic currency metadata and formatting</li>
 *   <li><strong>Player Balances:</strong> Raw balance amounts with various formatting options</li>
 *   <li><strong>Formatted Balances:</strong> Complete balance strings with currency symbols and formatting</li>
 * </ul>
 *
 * <h3>Supported Placeholder Formats:</h3>
 * <ul>
 *   <li><code>currency_&lt;currency&gt;_name</code> - The name/identifier of a currency</li>
 *   <li><code>currency_&lt;currency&gt;_symbol</code> - The symbol of a currency</li>
 *   <li><code>currency_&lt;currency&gt;_prefix</code> - The prefix for a currency</li>
 *   <li><code>currency_&lt;currency&gt;_suffix</code> - The suffix for a currency</li>
 *   <li><code>currency_&lt;currency&gt;_currencies</code> - The formatted currency string (prefix + name + suffix)</li>
 *   <li><code>player_currency_&lt;currency&gt;_amount</code> - The player's balance in a currency (2 decimals)</li>
 *   <li><code>player_currency_&lt;currency&gt;_amount-rounded</code> - The player's balance rounded to integer</li>
 *   <li><code>player_currency_&lt;currency&gt;_amount-rounded-dots</code> - The player's balance rounded with locale dots</li>
 *   <li><code>player_formatted_currency_&lt;currency&gt;_amount</code> - The player's formatted balance (prefix + amount + symbol + suffix)</li>
 *   <li><code>player_formatted_currency_&lt;currency&gt;_amount-rounded</code> - The player's formatted rounded balance</li>
 *   <li><code>player_formatted_currency_&lt;currency&gt;_amount-rounded-dots</code> - The player's formatted rounded balance with locale dots</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // In chat or scoreboard configuration:
 * "Your gold balance: %jecurrency_player_formatted_currency_gold_amount%"
 * "Server economy: %jecurrency_currency_gold_symbol%"
 * }</pre>
 *
 * @author JExcellence
 * @see CurrencyPlaceholderUtil
 * @see APlaceholder
 */
public class Placeholder extends APlaceholder {
	
	/**
	 * Utility for resolving and formatting currency placeholders.
	 */
	private final CurrencyPlaceholderUtil currencyPlaceholderUtil;
	
	/**
	 * Constructs a new {@code Placeholder} expansion for the given currency plugin.
	 * <p>
	 * Initializes the placeholder expansion with the platform abstraction layer and
	 * creates a {@link CurrencyPlaceholderUtil} instance for handling placeholder resolution.
	 * The expansion will be automatically registered with the platform's placeholder system.
	 * </p>
	 *
	 * @param jeCurrency the main JECurrency plugin instance, must not be null
	 * @throws IllegalArgumentException if the plugin instance is null
	 */
	public Placeholder(
		final @NotNull JECurrency jeCurrency
	) {
		
		super(jeCurrency.getPlatform());
		this.currencyPlaceholderUtil = new CurrencyPlaceholderUtil(jeCurrency);
	}
	
	/**
	 * Defines the list of supported placeholder formats for this expansion.
	 * <p>
	 * This method returns a comprehensive list of all placeholder patterns supported
	 * by the currency system. The placeholders are organized into three main categories:
	 * currency information, player balances, and formatted balances.
	 * </p>
	 *
	 * <h3>Placeholder Categories:</h3>
	 * <ul>
	 *   <li><strong>Currency Info:</strong> Basic currency metadata (name, symbol, prefix, suffix)</li>
	 *   <li><strong>Player Currency:</strong> Raw balance amounts with formatting options</li>
	 *   <li><strong>Formatted Currency:</strong> Complete balance strings with all currency elements</li>
	 * </ul>
	 *
	 * @return a list of placeholder format strings, never null or empty
	 */
	@Override
	public @NotNull List<String> setPlaceholder() {
		return List.of(
			"currency_<currency>_name",
			"currency_<currency>_symbol",
			"currency_<currency>_prefix",
			"currency_<currency>_suffix",
			"currency_<currency>_currencies",
			"player_currency_<currency>_amount",
			"player_currency_<currency>_amount-rounded",
			"player_currency_<currency>_amount-rounded-dots",
			"player_formatted_currency_<currency>_amount",
			"player_formatted_currency_<currency>_amount-rounded",
			"player_formatted_currency_<currency>_amount-rounded-dots"
		);
	}
	
	/**
	 * Resolves a placeholder for a given player and parameter string.
	 * <p>
	 * This method serves as the main entry point for placeholder resolution within the
	 * platform's placeholder system. It validates the input parameters and delegates
	 * the actual resolution to {@link CurrencyPlaceholderUtil} for supported placeholder formats.
	 * </p>
	 *
	 * <h3>Supported Placeholder Prefixes:</h3>
	 * <ul>
	 *   <li><code>currency_</code> - Currency information placeholders</li>
	 *   <li><code>player_currency_</code> - Raw player balance placeholders</li>
	 *   <li><code>player_formatted_currency_</code> - Formatted player balance placeholders</li>
	 * </ul>
	 *
	 * <h3>Error Handling:</h3>
	 * <p>
	 * If the player is null or the placeholder parameters don't match any supported
	 * format, an empty string is returned to indicate that the placeholder is not
	 * handled by this expansion.
	 * </p>
	 *
	 * @param targetPlayer the player for whom the placeholder is being resolved, can be null
	 * @param placeholderParams the placeholder parameter string, must not be null
	 * @return the resolved placeholder value, or an empty string if not applicable
	 * @throws IllegalArgumentException if placeholderParams is null
	 */
	@Override
	public @Nullable String onPlaceholder(
		final @Nullable Player targetPlayer,
		final @NotNull String placeholderParams
	) {
		
		if (
			targetPlayer == null
		) {
			return "";
		}
		
		if (
			!placeholderParams.startsWith("currency_") &&
			!placeholderParams.startsWith("player_currency_") &&
			!placeholderParams.startsWith("player_formatted_currency_")
		) {
			return "";
		}
		
		final String resolvedPlaceholder = this.currencyPlaceholderUtil.processPlaceholder(
			targetPlayer,
			placeholderParams
		);
		
		return resolvedPlaceholder != null ? resolvedPlaceholder : "";
	}
}