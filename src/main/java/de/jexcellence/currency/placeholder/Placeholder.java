package de.jexcellence.currency.placeholder;

import de.jexcellence.currency.JECurrency;
import de.jexcellence.jeplatform.placeholder.APlaceholder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Placeholder extends APlaceholder {

    private final CurrencyPlaceholderUtil placeholderUtil;

	public Placeholder(
			final @NotNull JECurrency currency
	) {
		super(currency.getPlatform());
        this.placeholderUtil = new CurrencyPlaceholderUtil(currency);
	}

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

	@Override
	public @Nullable String onPlaceholder(
			final Player player,
			final @NotNull String params
	) {
		if (
				player == null ||
						(!params.startsWith("currency_") &&
								!params.startsWith("player_currency_") &&
								!params.startsWith("player_formatted_currency_"))
		) return "";

		return placeholderUtil.processPlaceholder(player, params);
	}
}