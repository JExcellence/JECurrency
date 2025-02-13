package de.jexcellence.currency.view;

import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.je18n.i18n.I18n;
import de.jexcellence.jeplatform.utility.itemstack.ItemBuildable;
import me.devnatan.inventoryframework.AnvilInput;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.ViewType;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class DynamicAnvilView extends View {

	private final AnvilInput anvilInput = AnvilInput.createAnvilInput();

	private final State<JECurrency> sCurrencyPlugin = initialState("plugin");
	private final State<Currency> sCurrency = initialState("currency");
	private final State<String> sAttribute = initialState("attribute");
	private final State<Consumer<String>> sSetter = initialState("setter");
	private final State<Predicate<String>> sValidator = initialState("validator");

	private JECurrency currencyPlugin;
	private Currency currency;
	private String attribute;
	private Consumer<String> setter;
	private Predicate<String> validator;

	@Override
	public void onInit(@NotNull ViewConfigBuilder config) {
		config.type(ViewType.ANVIL).use(anvilInput).build();
	}

	@Override
	public void onOpen(@NotNull OpenContext open) {
		this.currencyPlugin = sCurrencyPlugin.get(open);
		this.currency = sCurrency.get(open);
		this.attribute = sAttribute.get(open);
		this.setter = sSetter.get(open);
		this.validator = sValidator.get(open);

		open.modifyConfig().title(
				new I18n.Builder(
						"currencies_anvil_ui." + this.attribute.toLowerCase() + ".title", open.getPlayer()
				).build().display()
		);
	}

	@Override
	public void onFirstRender(@NotNull RenderContext render) {
		render.firstSlot(
				new ItemBuildable.Builder(
						Material.NAME_TAG
				).setName(
						new I18n.Builder("currencies_anvil_ui.first_slot." + this.attribute.toLowerCase() + ".name", render.getPlayer()).build().display()
				).setLore(
						new I18n.Builder("currencies_anvil_ui.first_slot." + this.attribute.toLowerCase() + ".lore", render.getPlayer()).withPlaceholder("attribute", this.attribute).build().display()
				).build()
		);

		render.resultSlot().onClick(clickContext -> {
			final String input = this.anvilInput.get(render);

			if (
					this.validator.test(input)
			) {
				this.setter.accept(input);
				new I18n.Builder(
						"currencies_anvil_ui." + this.attribute.toLowerCase() + ".setter", render.getPlayer()
				).withPlaceholder("value", input).includingPrefix().build().send();
			} else {
				new I18n.Builder(
						"currencies_anvil_ui." + this.attribute.toLowerCase() + ".validator", render.getPlayer()
				).withPlaceholders(
						Map.of(
								"invalid_value", input,
								"validator", this.validator.toString()
						)
				).build().send();
			}

			this.back(
					clickContext.getViewer(),
					Map.of(
							"plugin", this.currencyPlugin,
							"currency", this.currency
					)
			);
		});
	}
}