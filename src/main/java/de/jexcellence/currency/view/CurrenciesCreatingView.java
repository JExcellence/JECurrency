package de.jexcellence.currency.view;

import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.je18n.i18n.I18n;
import de.jexcellence.jeplatform.utility.AnvilUIFactory;
import de.jexcellence.jeplatform.utility.itemstack.ItemBuildable;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class CurrenciesCreatingView extends View {

	private final State<JECurrency> sCurrencyPlugin = initialState("plugin");
	private final State<Currency> sCurrency = initialState("currency");

	private Currency currency;
	private JECurrency currencyPlugin;

	@Override
	public void onInit(@NotNull ViewConfigBuilder config) {
		config.size(3).build();
	}

	@Override
	public void onFirstRender(@NotNull RenderContext context) {
		this.setIdentifier(context);
	}

	@Override
	public void onOpen(@NotNull OpenContext open) {
		this.currency = sCurrency.get(open);
		this.currencyPlugin = sCurrencyPlugin.get(open);

		open.modifyConfig().title(
			new I18n.Builder(
				"currencies_creating_ui.title", open.getPlayer()
			).build().display()
		);
	}

	private void setIdentifier(
			@NotNull RenderContext context
	) {
		context.slot(
				2, 2,
				new ItemBuildable.Builder(
						Material.NAME_TAG
				).setName(
						new I18n.Builder("currencies_creating_ui.currency_identifier.name", context.getPlayer()).build().display()
				).setLore(
						new I18n.Builder("currencies_creating_ui.currency_identifier.lore", context.getPlayer()).withPlaceholder("currency_identifier", this.currency.getIdentifier()).build().display()
				).build()
		).onClick(clickContext -> {
/*			this.currencyPlugin.getViewFrame().open(DynamicAnvilView.class, clickContext.getPlayer(), Map.of(
					"plugin", this.currencyPlugin,
					"currency", this.currency,
					"attribute", "currency_identifier",
					"setter", (Consumer<String>) this.currency::setIdentifier,
					"validator", Pattern.compile("^[a-zA-Z_-]$").asMatchPredicate()
			));*/
		});
	}
}
