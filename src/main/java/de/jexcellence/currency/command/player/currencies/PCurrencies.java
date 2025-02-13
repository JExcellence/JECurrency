package de.jexcellence.currency.command.player.currencies;

import de.jexcellence.commands.PlayerCommand;
import de.jexcellence.commands.utility.Command;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.view.CurrenciesCreatingView;
import de.jexcellence.je18n.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Command
public class PCurrencies extends PlayerCommand {
	
	private final JECurrency currency;
	
	public PCurrencies(
			final @NotNull PCurrenciesSection commandSection,
			final @NotNull JECurrency currency
	) {
		super(commandSection);
		this.currency = currency;
	}
	
	@Override
	protected void onPlayerInvocation(
			final @NotNull Player player,
			final @NotNull String label,
			final @NotNull String[] args
	) {
		if (
				this.hasNoPermission(player, ECurrenciesPermission.CURRENCIES)
		) return;
		
		ECurrenciesAction action = this.enumParameterOrElse(args, 0, ECurrenciesAction.class, ECurrenciesAction.HELP);
		
		if (action == ECurrenciesAction.HELP) {
			this.help(player);
			return;
		}
		
		switch (action) {
			case CREATE -> this.handleCreate(player);
			case DELETE -> {
				if (
					this.hasNoPermission(player, ECurrenciesPermission.DELETE)
				) return;
			}
			case EDIT -> {
				if (
					this.hasNoPermission(player, ECurrenciesPermission.EDIT)
				) return;
			}
			case OVERVIEW -> {
				if (
					this.hasNoPermission(player, ECurrenciesPermission.OVERVIEW)
				) return;
			}
		}
	}
	
	@Override
	protected List<String> onPlayerTabCompletion(
			final @NotNull Player player,
			final @NotNull String label,
			final @NotNull String[] args
	) {
		if (this.hasNoPermission(player, ECurrenciesPermission.OVERVIEW) || args.length != 1)
			return new ArrayList<>();

		return StringUtil.copyPartialMatches(
				args[0].toLowerCase(),
				Arrays.stream(ECurrenciesPermission.values()).map(ECurrenciesPermission::name).map(String::toLowerCase).toList(),
				new ArrayList<>()
		);
	}
	
	private void help(
			final @NotNull Player player
	) {
		new I18n.Builder("currency.help", player).includingPrefix().build().send();
	}

	private void handleCreate(
			final Player player
	) {
		if (
				this.hasNoPermission(player, ECurrenciesPermission.CREATE)
		) return;

		this.currency.getViewFrame().open(
				CurrenciesCreatingView.class,
				player,
				Map.of(
						"plugin", this.currency,
						"currency", new Currency("", "", "", "")
				)
		);
	}

	private void handleDelete(
		final Player player
	) {
		if (
			! this.hasNoPermission(player, ECurrenciesPermission.DELETE)
		) return;
	}

	private void handleEdit(
		final Player player
	) {
		if (
			! this.hasNoPermission(player, ECurrenciesPermission.EDIT)
		) return;
	}

	private void handleOverview(
		final Player player
	) {
		if (
			! this.hasNoPermission(player, ECurrenciesPermission.OVERVIEW)
		) return;
	}
}
