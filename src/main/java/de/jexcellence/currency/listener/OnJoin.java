package de.jexcellence.currency.listener;

import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.User;
import de.jexcellence.currency.database.entity.UserCurrency;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class OnJoin implements Listener {

	private final JECurrency currency;

	public OnJoin(
		final @NotNull JECurrency currency
	) {
		this.currency = currency;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onJoin(
		final AsyncPlayerPreLoginEvent event
	) {
		this.currency.getUserRepository().findByUniqueIdAsync(event.getUniqueId()).thenAcceptAsync(
			player -> {
				if (player == null) {
					player = new User(event.getUniqueId(), event.getName());
					this.currency.getUserRepository().create(player);
					this.currency.getPlatformLogger().logDebug("Created Player: " + player.getUniqueId());
				} else {
					player.setPlayerName(event.getName());
					this.currency.getUserRepository().update(player);
					this.currency.getPlatformLogger().logDebug("Updated Player: " + player.getUniqueId());
				}

				this.updateCurrencies(player);
			}, this.currency.getExecutor()
		);
	}

	private void updateCurrencies(
		final @NotNull User player
	) {
		this.currency.getCurrencies().values().forEach(currency -> {
			this.currency.getUsercurrencyRepository().findByAttributesAsync(Map.of("user.uniqueId", player.getUniqueId(), "currency", currency)).thenAcceptAsync(
				usercurrency -> {
					if (usercurrency == null)
						this.currency.getUsercurrencyRepository().create(new UserCurrency(player, currency));
				}, this.currency.getExecutor()
			);
		});
	}
}
