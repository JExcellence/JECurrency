package de.jexcellence.currency.command.player.currency;

import de.jexcellence.commands.PlayerCommand;
import de.jexcellence.commands.utility.Command;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.entity.UserCurrency;
import de.jexcellence.je18n.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Command
public class PCurrency extends PlayerCommand {

	private final JECurrency plugin;

	/**
	 * Constructs a new PCurrency command
	 *
	 * @param commandSection the command section
	 * @param plugin the JECurrency plugin instance
	 */
	public PCurrency(
			final @NotNull PCurrencySection commandSection,
			final @NotNull JECurrency plugin
	) {
		super(commandSection);
		this.plugin = plugin;
	}

	/**
	 * Handles the command execution when a player invokes it
	 *
	 * @param player the player who executed the command
	 * @param label the command label used
	 * @param args the command arguments
	 */
	@Override
	protected void onPlayerInvocation(
			final @NotNull Player player,
			final @NotNull String label,
			final @NotNull String[] args
	) {
		if (this.hasNoPermission(player, ECurrencyPermission.CURRENCY)) {
			return;
		}

		if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("all"))) {
			showAllCurrencies(player, player);
			return;
		}

		if (args.length == 2 && args[0].equalsIgnoreCase("all")) {
			if (this.hasNoPermission(player, ECurrencyPermission.CURRENCY_OTHER)) {
				return;
			}

			String targetName = args[1];
			OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);

			if (!targetPlayer.hasPlayedBefore()) {
				new I18n.Builder("general.invalid_player", player)
						.includingPrefix()
						.withPlaceholders(Map.of("player", targetName))
						.build().send();
				return;
			}

			showAllCurrencies(player, targetPlayer);
			return;
		}

		String currencyName = args[0];
		Currency currency = this.plugin.getCurrencies().values().stream()
				.filter(c -> c.getIdentifier().equalsIgnoreCase(currencyName))
				.findFirst()
				.orElse(null);

		if (currency == null) {
			new I18n.Builder("general.invalid_currency", player)
					.includingPrefix()
					.withPlaceholders(Map.of(
							"currency", currencyName,
							"available_currencies", this.plugin.getCurrencies().values().stream()
									.map(Currency::getIdentifier)
									.collect(Collectors.joining(", "))
					))
					.build().send();
			return;
		}

		if (args.length > 1) {
			if (this.hasNoPermission(player, ECurrencyPermission.CURRENCY_OTHER)) {
				return;
			}

			String playerName = args[1];
			OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerName);

			if (!targetPlayer.hasPlayedBefore()) {
				new I18n.Builder("general.invalid_player", player)
						.includingPrefix()
						.withPlaceholders(Map.of("player", playerName))
						.build().send();
				return;
			}

			showCurrencyBalance(player, targetPlayer, currency);
		} else {
			showCurrencyBalance(player, player, currency);
		}
	}

	/**
	 * Provides tab completion suggestions for the command
	 *
	 * @param player the player requesting tab completion
	 * @param label the command label used
	 * @param args the current command arguments
	 * @return a list of tab completion suggestions
	 */
	@Override
	protected List<String> onPlayerTabCompletion(
			final @NotNull Player player,
			final @NotNull String label,
			final @NotNull String[] args
	) {
		List<String> completions = new ArrayList<>();

		if (args.length == 1) {
			List<String> suggestions = new ArrayList<>();

			suggestions.add("all");

			suggestions.addAll(this.plugin.getCurrencies().values().stream().map(Currency::getIdentifier).toList());

			return StringUtil.copyPartialMatches(args[0], suggestions, completions);
		}

		if (args.length == 2) {
			if (!this.hasNoPermission(player, ECurrencyPermission.CURRENCY_OTHER)) {
				List<String> playerNames = Bukkit.getOnlinePlayers().stream()
						.map(Player::getName)
						.collect(Collectors.toList());
				return StringUtil.copyPartialMatches(args[1], playerNames, completions);
			}
		}

		return completions;
	}

	/**
	 * Shows the balance of a specific currency for a player
	 *
	 * @param sender the player who will receive the message
	 * @param target the player whose balance is being checked
	 * @param currency the currency to check
	 */
	private void showCurrencyBalance(Player sender, OfflinePlayer target, Currency currency) {
		this.plugin.getCurrencyAdapter().getBalance(target, currency)
				.thenAcceptAsync(balance -> {
					if (sender.equals(target)) {
						new I18n.Builder("currency.balance.self", sender)
								.includingPrefix()
								.withPlaceholders(Map.of(
										"currency", currency.getIdentifier(),
										"symbol", currency.getSymbol(),
										"balance", String.format("%.2f", balance),
										"prefix", currency.getPrefix() != null ? currency.getPrefix() : "",
										"suffix", currency.getSuffix() != null ? currency.getSuffix() : ""
								))
								.build().send();
					} else {
						new I18n.Builder("currency.balance.other", sender)
								.includingPrefix()
								.withPlaceholders(Map.of(
										"player", target.getName(),
										"currency", currency.getIdentifier(),
										"symbol", currency.getSymbol(),
										"balance", String.format("%.2f", balance),
										"prefix", currency.getPrefix() != null ? currency.getPrefix() : "",
										"suffix", currency.getSuffix() != null ? currency.getSuffix() : ""
								))
								.build().send();
					}
				}, this.plugin.getExecutor());
	}

	/**
	 * Shows all currency balances for a player
	 *
	 * @param sender the player who will receive the message
	 * @param target the player whose balances are being checked
	 */
	private void showAllCurrencies(Player sender, OfflinePlayer target) {
		if (this.plugin.getCurrencies().isEmpty()) {
			new I18n.Builder("currency.error.no_currencies", sender)
					.includingPrefix()
					.build().send();
			return;
		}

		this.plugin.getCurrencyAdapter().getUserCurrencies(target)
				.thenAcceptAsync(userCurrencies -> {
					if (userCurrencies.isEmpty()) {
						if (sender.equals(target)) {
							new I18n.Builder("currency.balance.no_currencies_self", sender)
									.includingPrefix()
									.build().send();
						} else {
							new I18n.Builder("currency.balance.no_currencies_other", sender)
									.includingPrefix()
									.withPlaceholders(Map.of("player", target.getName()))
									.build().send();
						}
						return;
					}

					if (sender.equals(target)) {
						new I18n.Builder("currency.balance.all_header_self", sender)
								.includingPrefix()
								.build().send();
					} else {
						new I18n.Builder("currency.balance.all_header_other", sender)
								.includingPrefix()
								.withPlaceholders(Map.of("player", target.getName()))
								.build().send();
					}

					for (UserCurrency userCurrency : userCurrencies) {
						Currency currency = userCurrency.getCurrency();
						new I18n.Builder("currency.balance.entry", sender)
								.withPlaceholders(Map.of(
										"currency", currency.getIdentifier(),
										"symbol", currency.getSymbol(),
										"balance", String.format("%.2f", userCurrency.getBalance()),
										"prefix", currency.getPrefix() != null ? currency.getPrefix() : "",
										"suffix", currency.getSuffix() != null ? currency.getSuffix() : ""
								))
								.build().send();
					}

					new I18n.Builder("currency.balance.all_footer", sender)
							.build().send();
				}, this.plugin.getExecutor());
	}
}