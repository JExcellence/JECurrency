package de.jexcellence.currency;

import de.jexcellence.commands.CommandFactory;
import de.jexcellence.currency.adapter.CurrencyAdapter;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.repository.CurrencyRepository;
import de.jexcellence.currency.database.repository.UserCurrencyRepository;
import de.jexcellence.currency.database.repository.UserRepository;
import de.jexcellence.currency.view.CurrenciesCreatingView;
import de.jexcellence.currency.view.DynamicAnvilView;
import de.jexcellence.jeplatform.JEPlatform;
import de.jexcellence.jeplatform.inventory.InventoryFactory;
import de.jexcellence.jeplatform.logger.JELogger;
import me.devnatan.inventoryframework.AnvilInputFeature;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JECurrency extends JavaPlugin {

	private final ExecutorService executor = Executors.newFixedThreadPool(5);
	private final Map<Long, Currency> currencies = new LinkedHashMap<>();

	private CommandFactory commandFactory;
	private InventoryFactory inventoryFactory;

	private CurrencyAdapter currencyAdapter;

	private UserCurrencyRepository usercurrencyRepository;
	private CurrencyRepository currencyRepository;
	private UserRepository userRepository;

	private JEPlatform platform;

	private ViewFrame viewFrame;

	@Override
	public void onLoad() {
		this.platform = new JEPlatform(this, true);
		this.getPlatformLogger().logInfo("JECurrency is starting...");
	}

	@Override
	public void onEnable() {
		this.commandFactory = new CommandFactory(this);
		this.inventoryFactory = new InventoryFactory(this.platform);

		this.commandFactory.registerAllCommandsAndListeners();

		this.userRepository = new UserRepository(this.executor, this.platform.getEntityManagerFactory());
		this.currencyRepository = new CurrencyRepository(this.executor, this.platform.getEntityManagerFactory());
		this.usercurrencyRepository = new UserCurrencyRepository(this.executor, this.platform.getEntityManagerFactory());

		this.currencyAdapter = new CurrencyAdapter(this);

		this.viewFrame = ViewFrame.create(this).defaultConfig(config -> {
			config.cancelOnClick();
			config.cancelOnDrag();
			config.cancelOnDrop();
			config.cancelOnPickup();
		}).with(new CurrenciesCreatingView(), new DynamicAnvilView()).install(AnvilInputFeature.AnvilInput).register();

		this.loadCurrencies();
		this.getPlatformLogger().logInfo("JECurrency is enabled!");
	}

	@Override
	public void onDisable() {
		super.onDisable();
	}

	public ExecutorService getExecutor() {
		return this.executor;
	}

	public JEPlatform getPlatform() {
		return this.platform;
	}

	public JELogger getPlatformLogger() {
		return this.platform.getLogger();
	}

	public CommandFactory getCommandFactory() {
		return this.commandFactory;
	}

	public InventoryFactory getInventoryFactory() {
		return this.inventoryFactory;
	}

	public CurrencyRepository getCurrencyRepository() {
		return this.currencyRepository;
	}

	public UserRepository getUserRepository() {
		return this.userRepository;
	}

	public UserCurrencyRepository getUsercurrencyRepository() {
		return this.usercurrencyRepository;
	}

	public Map<Long, Currency> getCurrencies() {
		return this.currencies;
	}

	public ViewFrame getViewFrame() {
		return this.viewFrame;
	}

	public CurrencyAdapter getCurrencyAdapter() {
		return this.currencyAdapter;
	}

	private void loadCurrencies() {
		this.currencyRepository.findAllAsync(0, 128).thenAcceptAsync(currencies -> {
			if (currencies == null)
				return;

			currencies.forEach(currency -> this.currencies.put(currency.getId(), currency));
		}, this.executor);
	}
}
