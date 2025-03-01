package de.jexcellence.currency.command.console.withdraw;

import de.jexcellence.commands.ServerCommand;
import de.jexcellence.commands.utility.Command;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Command
public class CWithdraw extends ServerCommand {

	private final JECurrency jeCurrency;

	public CWithdraw(
			final @NotNull CWithdrawSection commandSection,
			final @NotNull JECurrency jeCurrency
	) {
		super(commandSection);
		this.jeCurrency = jeCurrency;
	}

	@Override
	protected void onPlayerInvocation(
			final @NotNull ConsoleCommandSender consoleCommandSender,
			final @NotNull String label,
			final @NotNull String[] args
	) {
		OfflinePlayer targetPlayer = this.offlinePlayerParameter(args, 0, true);
		String currencyIdentifier = this.stringParameter(args, 1);
		double withdrawAmount = this.doubleParameter(args, 2);

		Currency currency = this.jeCurrency.getCurrencies().values().stream().filter(pCurrency -> pCurrency.getIdentifier().equals(currencyIdentifier)).findFirst().orElse(null);

		if (currency == null) {
			jeCurrency.getPlatformLogger().logDebug("Currency with name: " + currencyIdentifier + " not found.");
			jeCurrency.getPlatformLogger().logDebug("Currencies available: " + String.join(", ", this.jeCurrency.getCurrencies().values().stream().map(Currency::getIdentifier).toList()));
			return;
		}

		this.jeCurrency.getCurrencyAdapter().getUserCurrency(targetPlayer, currencyIdentifier).thenAcceptAsync(
				usercurrency -> {
					if (usercurrency == null) {
						jeCurrency.getPlatformLogger().logDebug("PPlayerCurrency with player uuid: " + targetPlayer.getUniqueId() + " and currencies name: " + currencyIdentifier + " not found. (amount: " + withdrawAmount + ").");
						return;
					}

					if (! usercurrency.withdraw(withdrawAmount)) {
						jeCurrency.getPlatformLogger().logDebug("Could not withdraw the amount of " + withdrawAmount + " to the player uuid: " + targetPlayer.getUniqueId() + ".");
						return;
					}

					jeCurrency.getUsercurrencyRepository().update(usercurrency);
				}, jeCurrency.getExecutor()
		);
	}

	@Override
	protected List<String> onTabCompletion(CommandSender commandSender, String s, String[] strings) {
		return List.of();
	}
}