package de.jexcellence.currency.command.player.currencies;

import de.jexcellence.commands.PlayerCommand;
import de.jexcellence.commands.utility.Command;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.je18n.i18n.I18n;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Command
public class PCurrencies extends PlayerCommand {

	private final JECurrency currency;
	private final CurrencyCommandHandler commandHandler;

	public PCurrencies(
			final @NotNull PCurrenciesSection commandSection,
			final @NotNull JECurrency currency
	) {
		super(commandSection);
		this.currency = currency;
		this.commandHandler = new CurrencyCommandHandler(currency);
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
			case CREATE -> {
				if (this.hasNoPermission(player, ECurrenciesPermission.CREATE)) return;
				this.commandHandler.createCurrency(player, args);
			}
			case DELETE -> {
				if (this.hasNoPermission(player, ECurrenciesPermission.DELETE)) return;
				this.commandHandler.deleteCurrency(player, args);
			}
			case EDIT -> {
				if (this.hasNoPermission(player, ECurrenciesPermission.EDIT)) return;
				this.commandHandler.editCurrency(player, args);
			}
			case OVERVIEW -> {
				if (this.hasNoPermission(player, ECurrenciesPermission.OVERVIEW)) return;
				this.commandHandler.listCurrencies(player);
			}
			case INFO -> {
				if (this.hasNoPermission(player, ECurrenciesPermission.OVERVIEW)) return;
				this.commandHandler.showCurrencyInfo(player, args);
			}
		}
	}

	@Override
	protected List<String> onPlayerTabCompletion(
			final @NotNull Player player,
			final @NotNull String label,
			final @NotNull String[] args
	) {
		List<String> completions = new ArrayList<>();

		if (args.length == 1) {
			List<String> availableCommands = new ArrayList<>();

			if (! this.hasNoPermission(player, ECurrenciesPermission.OVERVIEW)) {
				availableCommands.add(ECurrenciesAction.OVERVIEW.name().toLowerCase());
				availableCommands.add(ECurrenciesAction.INFO.name().toLowerCase());
			}

			if (! this.hasNoPermission(player, ECurrenciesPermission.CREATE)) {
				availableCommands.add(ECurrenciesAction.CREATE.name().toLowerCase());
			}

			if (! this.hasNoPermission(player, ECurrenciesPermission.DELETE)) {
				availableCommands.add(ECurrenciesAction.DELETE.name().toLowerCase());
			}

			if (! this.hasNoPermission(player, ECurrenciesPermission.EDIT)) {
				availableCommands.add(ECurrenciesAction.EDIT.name().toLowerCase());
			}

			availableCommands.add(ECurrenciesAction.HELP.name().toLowerCase());

			return StringUtil.copyPartialMatches(args[0].toLowerCase(), availableCommands, completions);
		}

		if (args.length == 2) {
			try {
				ECurrenciesAction action = ECurrenciesAction.valueOf(args[0].toUpperCase());

				switch (action) {
					case INFO, DELETE, EDIT -> {
						if (! this.hasNoPermission(player, getRequiredPermissionForAction(action))) {
							return StringUtil.copyPartialMatches(
									args[1].toLowerCase(),
									this.commandHandler.getCurrencyIdentifiers().join(),
									completions
							);
						}
					}
					case CREATE -> {
						if (! this.hasNoPermission(player, ECurrenciesPermission.CREATE)) {
							List<String> suggestions = Arrays.asList("coins", "gems", "tokens", "points", "credits");
							return StringUtil.copyPartialMatches(args[1].toLowerCase(), suggestions, completions);
						}
					}
				}
			} catch (IllegalArgumentException e) {
				return completions;
			}
		}

		if (args.length == 3) {
			try {
				ECurrenciesAction action = ECurrenciesAction.valueOf(args[0].toUpperCase());

				switch (action) {
					case EDIT -> {
						if (! this.hasNoPermission(player, ECurrenciesPermission.EDIT)) {
							return StringUtil.copyPartialMatches(
									args[2].toLowerCase(),
									this.commandHandler.getEditableFields(),
									completions
							);
						}
					}
				}
			} catch (IllegalArgumentException e) {
				return completions;
			}
		}

		if (args.length == 4 && args[0].equalsIgnoreCase(ECurrenciesAction.EDIT.name())) {
			String field = args[2].toLowerCase();

			if (field.equals("symbol")) {
				List<String> symbols = Arrays.asList("$", "€", "£", "¥", "₿", "⭐", "💎", "🪙");
				return StringUtil.copyPartialMatches(args[3], symbols, completions);
			} else if (field.equals("prefix") || field.equals("suffix")) {
				List<String> suggestions = Arrays.asList("", " ", "  ");
				return StringUtil.copyPartialMatches(args[3], suggestions, completions);
			}
		}

		return completions;
	}

	/**
	 * Gets the required permission for a specific action
	 *
	 * @param action The action to check
	 * @return The permission required for the action
	 */
	private ECurrenciesPermission getRequiredPermissionForAction(ECurrenciesAction action) {
		return switch (action) {
			case OVERVIEW, INFO, HELP -> ECurrenciesPermission.OVERVIEW;
			case CREATE -> ECurrenciesPermission.CREATE;
			case DELETE -> ECurrenciesPermission.DELETE;
			case EDIT -> ECurrenciesPermission.EDIT;
		};
	}
	
	private void help(
			final @NotNull Player player
	) {
		new I18n.Builder("currencies.help", player).includingPrefix().build().send();
	}
}
