package de.jexcellence.currency.placeholder;

import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.entity.UserCurrency;
import de.jexcellence.jeplatform.placeholder.APlaceholder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class Placeholder extends APlaceholder {

	private static final Pattern PARAMS_PATTERN = Pattern.compile("_");
	private final JECurrency currency;

	public Placeholder(
		final @NotNull JECurrency currency
	) {
		super(currency.getPlatform());
		this.currency = currency;
	}

	@Override
	public @NotNull List<String> setPlaceholder() {
		return List.of(
			"currency_<currency>_name",
			"currency_<currency>_symbol",
			"currency_<currency>_currencies",
			"player_currency_<currency>_amount",
			"player_currency_<currency>_amount-rounded",
			"player_currency_<currency>_amount-rounded-dots"
		);
	}

	@Override
	public @Nullable String onPlaceholder(
		final Player player,
		final @NotNull String params
	) {
		if (
			player == null ||
				(!params.startsWith("currency_") && !params.startsWith("player_currency_"))
		) return "";

		if (
			params.startsWith("currency_")
		) return this.processCurrency(params);
		else return this.processPlayerCurrency(player, params);
	}

	/**
	 * Processes the currency-related placeholder based on the parameters provided.
	 *
	 * @param params The parameters for the currency placeholder.
	 * @return The processed currency placeholder result.
	 */
	private String processCurrency(final @NotNull String params) {
		String[] paramsSplit = PARAMS_PATTERN.split(params);

		if (
			paramsSplit.length < 3
		) return "";

		String currencyName = paramsSplit[1];
		String currencyDetail = paramsSplit[2];

		Optional<Currency> oCurrency = this.currency.getCurrencies().values().stream()
			.filter(currency -> currency.getIdentifier().equalsIgnoreCase(currencyName))
			.findFirst();

		return oCurrency.map(currency -> switch (currencyDetail) {
			case "name" -> currency.getIdentifier();
			case "symbol" -> currency.getSymbol();
			case "currencies" -> currency.getPrefix() + currency.getIdentifier() + currency.getSuffix();
			default -> "";
		}).orElse("");
	}

	/**
	 * Processes the player currency-related placeholder based on the player and parameters provided.
	 *
	 * @param player The player associated with the player currency placeholder.
	 * @param params The parameters for the player currency placeholder.
	 * @return The processed player currency placeholder result.
	 */
	private String processPlayerCurrency(final @NotNull Player player, final @NotNull String params) {
		String[] paramsSplit = PARAMS_PATTERN.split(params);

		if (paramsSplit.length < 3)
			return "";

		String currencyName = paramsSplit[2];
		String currencyDetail = paramsSplit[3];

		Optional<Currency> oCurrency = this.currency.getCurrencies().values().stream()
			.filter(currency -> currency.getIdentifier().equalsIgnoreCase(currencyName))
			.findFirst();

		if (
			oCurrency.isEmpty()
		) return "N/A";

		if (currencyDetail.equalsIgnoreCase("amount")) {
			UserCurrency usercurrency = this.currency.getUsercurrencyRepository().findByUniqueIdAndCurrency(player.getUniqueId(), oCurrency.get());
			return usercurrency == null ? "N/A" : String.format("%.2f", usercurrency.getBalance());
		}

		if (currencyDetail.equalsIgnoreCase("amount-rounded")) {
			UserCurrency usercurrency = this.currency.getUsercurrencyRepository().findByUniqueIdAndCurrency(player.getUniqueId(), oCurrency.get());
			return usercurrency == null ? "N/A" : String.format("%.0f", usercurrency.getBalance());
		}

		if (currencyDetail.equalsIgnoreCase("amount-rounded-dots")) {
			UserCurrency usercurrency = this.currency.getUsercurrencyRepository().findByUniqueIdAndCurrency(player.getUniqueId(), oCurrency.get());
			NumberFormat numberFormat = NumberFormat.getInstance(Locale.GERMANY);
			numberFormat.setMaximumFractionDigits(0); // No decimal places

			return usercurrency == null ? "N/A" : numberFormat.format(usercurrency.getBalance());
		}

		return "N/A";
	}
}
