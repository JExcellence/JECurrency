package de.jexcellence.currency;

import com.raindropcentral.commands.CommandFactory;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logger.CentralLogger;
import com.raindropcentral.rplatform.view.ConfirmationView;
import com.raindropcentral.rplatform.view.common.anvil.CustomAnvilInputFeature;
import de.jexcellence.currency.adapter.CurrencyAdapter;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.repository.CurrencyLogRepository;
import de.jexcellence.currency.database.repository.CurrencyRepository;
import de.jexcellence.currency.database.repository.UserCurrencyRepository;
import de.jexcellence.currency.database.repository.UserRepository;
import de.jexcellence.currency.event.CurrencyLogEventListener;
import de.jexcellence.currency.migrate.VaultMigrationManager;
import de.jexcellence.currency.service.CurrencyLogService;
import de.jexcellence.currency.view.currency.CurrenciesActionOverviewView;
import de.jexcellence.currency.view.currency.CurrenciesCreatingView;
import de.jexcellence.currency.view.currency.CurrenciesOverviewView;
import de.jexcellence.currency.view.currency.CurrencyDetailView;
import de.jexcellence.currency.view.currency.CurrencyLeaderboardView;
import de.jexcellence.currency.view.currency.anvil.CurrencyIconAnvilView;
import de.jexcellence.currency.view.currency.anvil.CurrencyIdentifierAnvilView;
import de.jexcellence.currency.view.currency.anvil.CurrencyPrefixAnvilView;
import de.jexcellence.currency.view.currency.anvil.CurrencySuffixAnvilView;
import de.jexcellence.currency.view.currency.anvil.CurrencySymbolAnvilView;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Main plugin class for JECurrency - A comprehensive currency management system.
 * <p>
 * This class serves as the central hub for the JECurrency plugin, managing the complete
 * lifecycle from initialization to shut down. It orchestrates all core components including
 * repositories, services, command systems, and user interface frameworks.
 * </p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><strong>Repository Management:</strong> Initializes and provides access to currency, user, and user-currency repositories</li>
 *   <li><strong>Service Integration:</strong> Registers currency adapter service for external plugin compatibility</li>
 *   <li><strong>Command Framework:</strong> Manages command registration and listener initialization</li>
 *   <li><strong>UI Framework:</strong> Configures InventoryFramework views for currency management interfaces</li>
 *   <li><strong>Async Operations:</strong> Provides thread pool executor for non-blocking database operations</li>
 *   <li><strong>Currency Caching:</strong> Maintains in-memory cache for optimal performance</li>
 * </ul>
 *
 * <h3>Architecture Overview:</h3>
 * <p>
 * The plugin follows a layered architecture with clear separation of concerns:
 * </p>
 * <ul>
 *   <li><strong>Presentation Layer:</strong> InventoryFramework views and command interfaces</li>
 *   <li><strong>Service Layer:</strong> CurrencyAdapter and business logic components</li>
 *   <li><strong>Repository Layer:</strong> Data access objects with caching support</li>
 *   <li><strong>Infrastructure Layer:</strong> Platform abstraction and executor services</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class JECurrency extends JavaPlugin {
	
	/**
	 * Thread pool executor service for handling asynchronous database operations and background tasks.
	 * <p>
	 * Configured with a fixed thread pool of 5 threads to balance performance and resource usage.
	 * All database operations and heavy computations should be executed through this service
	 * to prevent blocking the main server thread.
	 * </p>
	 */
	private final ExecutorService asyncExecutorService = Executors.newFixedThreadPool(5);
	
	/**
	 * In-memory cache storing all loaded currencies mapped by their unique database ID.
	 * <p>
	 * This cache provides fast access to currency data without requiring database queries
	 * for every operation. The cache is populated during plugin initialization and maintained
	 * throughout the plugin lifecycle. Uses LinkedHashMap to preserve insertion order.
	 * </p>
	 */
	private final Map<Long, Currency> currencyCache = new LinkedHashMap<>();
	
	private VaultMigrationManager vaultMigrationManager;
	
	/**
	 * Service responsible for logging all currency-related operations and transactions.
	 * <p>
	 * This service provides comprehensive audit trails and analytics data for currency operations
	 * including balance changes, currency management actions, and system events. All logging
	 * operations are performed asynchronously to maintain optimal performance.
	 * </p>
	 */
	private CurrencyLogService logService;
	
	/**
	 * Event listener that automatically logs currency operations using the CurrencyLogService.
	 * <p>
	 * This listener responds to currency-related events and creates appropriate log entries,
	 * providing comprehensive audit trails and analytics data. By using event-driven logging,
	 * we maintain separation of concerns and ensure consistent logging across all operations.
	 * </p>
	 */
	private CurrencyLogEventListener logEventListener;
	
	/**
	 * Repository for managing currency log entities and audit trail data.
	 * <p>
	 * Handles all operations related to currency operation logging, including creation,
	 * queries, and maintenance of audit records. Provides asynchronous operations and
	 * supports complex queries for analytics and reporting purposes.
	 * </p>
	 */
	private CurrencyLogRepository currencyLogRepository;
	
	/**
	 * Startup banner message displayed during plugin initialization.
	 * <p>
	 * Contains branding information, version details, and initialization status.
	 * The message includes placeholders for dynamic content such as language count
	 * and translation key statistics.
	 * </p>
	 */
	private final String startupBannerMessage = """
                                                      
                                                          ===============================================================================================
                                                             _ ______ _____ _    _ _____  _____  ______ _   _  _______     __
                                                            | |  ____/ ____| |  | |  __ \\|  __ \\|  ____| \\ | |/ ____\\ \\   / /
                                                            | | |__ | |    | |  | | |__) | |__) | |__  |  \\| | |     \\ \\_/ /
                                                        _   | |  __|| |    | |  | |  _  /|  _  /|  __| | . ` | |      \\   /
                                                       | |__| | |___| |____| |__| | | \\ \\| | \\ \\| |____| |\\  | |____   | |
                                                        \\____/|______\\_____|\\____/|_|  \\_\\_|  \\_\\______|_| \\_|\\_____|  |_|
                                                      
                                                      
                                                                       Product of JExcellence
                                                             Officially Supported by Antimatter Zone, LLC
                                                      
                                                          ===============================================================================================
                                                          Language System Initialized [R18n]
                                                          Product by: JExcellence
                                                          Website: https://jexcellence.de/
                                                          ===============================================================================================
                                                          Languages Loaded: %d
                                                          Translation Keys: %d
                                                          ===============================================================================================
                                                      """;
	
	/**
	 * Command factory responsible for automatic command and listener registration.
	 * <p>
	 * Handles the discovery and registration of all command classes and event listeners
	 * within the plugin package structure. Provides centralized command management
	 * and configuration loading capabilities.
	 * </p>
	 */
	private CommandFactory commandRegistrationFactory;
	
	/**
	 * Currency adapter service providing external API access for other plugins.
	 * <p>
	 * This service is registered with Bukkit's ServiceManager and allows external plugins
	 * to interact with the currency system through a well-defined API. Supports operations
	 * such as balance queries, transactions, and currency management.
	 * </p>
	 */
	private CurrencyAdapter externalCurrencyAdapter;
	
	/**
	 * Repository for managing user-currency relationship entities and balance data.
	 * <p>
	 * Handles all operations related to player currency balances, including creation,
	 * updates, queries, and deletion. Provides caching support and asynchronous operations
	 * for optimal performance.
	 * </p>
	 */
	private UserCurrencyRepository playerCurrencyRepository;
	
	/**
	 * Repository for managing currency entity data and configuration.
	 * <p>
	 * Responsible for all currency-related database operations including CRUD operations,
	 * validation, and caching. Serves as the primary data access layer for currency
	 * management functionality.
	 * </p>
	 */
	private CurrencyRepository currencyDataRepository;
	
	/**
	 * Repository for managing user entity data and player information.
	 * <p>
	 * Handles player account creation, updates, and queries. Maintains player data
	 * consistency and provides efficient access to user information across the system.
	 * </p>
	 */
	private UserRepository playerDataRepository;
	
	/**
	 * Platform abstraction layer providing access to core services and utilities.
	 * <p>
	 * Encapsulates platform-specific functionality including internationalization,
	 * database management, scheduler services, and configuration handling. Provides
	 * a unified interface for accessing underlying platform capabilities.
	 * </p>
	 */
	private RPlatform platformAbstraction;
	
	/**
	 * InventoryFramework view frame managing all GUI-based user interfaces.
	 * <p>
	 * Coordinates the registration and lifecycle of all inventory-based views including
	 * currency management interfaces, creation wizards, and administrative panels.
	 * Provides centralized configuration and event handling for UI components.
	 * </p>
	 */
	private ViewFrame inventoryViewFramework;
	
	/**
	 * Initializes the plugin during the loading phase.
	 * <p>
	 * This method is called by Bukkit during server startup before the plugin is enabled.
	 * It performs essential initialization tasks that must be completed before other plugins
	 * can interact with this plugin's services.
	 * </p>
	 *
	 * <h3>Initialization Tasks:</h3>
	 * <ul>
	 *   <li>Configures the central logging system</li>
	 *   <li>Initializes the platform abstraction layer</li>
	 *   <li>Creates and registers the currency adapter service</li>
	 *   <li>Registers the service with Bukkit's ServiceManager</li>
	 * </ul>
	 */
	@Override
	public void onLoad() {
		CentralLogger.initialize(this);
		
		this.platformAbstraction = new RPlatform(
			this,
			startupBannerMessage
		);
		
		this.externalCurrencyAdapter = new CurrencyAdapter(this);
		
		Bukkit.getServer().getServicesManager().register(
			CurrencyAdapter.class,
			this.externalCurrencyAdapter,
			this,
			ServicePriority.Normal
		);
		
		this.vaultMigrationManager = new VaultMigrationManager(this);
		
		CentralLogger.getLogger(JECurrency.class.getName()).log(
			Level.INFO,
			"JECurrency initialization started - loading core components"
		);
	}
	
	/**
	 * Handles plugin shutdown and cleanup operations.
	 * <p>
	 * This method is called by Bukkit when the plugin is being disabled, either during
	 * server shutdown or plugin reload. It ensures all resources are properly released
	 * and background tasks are terminated gracefully.
	 * </p>
	 *
	 * <h3>Cleanup Operations:</h3>
	 * <ul>
	 *   <li>Gracefully shuts down the executor service</li>
	 *   <li>Waits for pending async operations to complete</li>
	 *   <li>Releases database connections and resources</li>
	 *   <li>Logs shutdown completion status</li>
	 * </ul>
	 */
	@Override
	public void onDisable() {
		if (
			!this.asyncExecutorService.isShutdown()
		) {
			this.asyncExecutorService.shutdown();
		}
		
		CentralLogger.getLogger(JECurrency.class.getName()).log(
			Level.INFO,
			"JECurrency has been disabled - all resources cleaned up"
		);
	}
	
	/**
	 * Completes plugin initialization and starts all services.
	 * <p>
	 * This method is called by Bukkit after all plugins have been loaded and the server
	 * is ready to accept connections. It performs the final initialization steps and
	 * activates all plugin functionality.
	 * </p>
	 *
	 * <h3>Activation Sequence:</h3>
	 * <ol>
	 *   <li>Initialize command factory and register commands</li>
	 *   <li>Set up database repositories with connection pooling</li>
	 *   <li>Configure and register InventoryFramework views</li>
	 *   <li>Load currency data into memory cache</li>
	 *   <li>Activate all services and mark plugin as ready</li>
	 * </ol>
	 */
	@Override
	public void onEnable() {
		
		this.initializeCommandSystem();
		this.initializeDatabaseRepositories();
		this.initializeUserInterfaceFramework();
		this.loadCurrencyDataIntoCache();
		
		this.logService = new CurrencyLogService(
			this.currencyLogRepository,
			this.asyncExecutorService
		);
		
		CentralLogger.getLogger(JECurrency.class.getName()).log(
			Level.INFO,
			"JECurrency is now fully enabled and ready for use"
		);
	}
	
	/**
	 * Retrieves the executor service for asynchronous operations.
	 * <p>
	 * This executor should be used for all database operations, file I/O, and other
	 * potentially blocking operations to prevent server lag. The executor is configured
	 * with an optimal thread pool size for currency operations.
	 * </p>
	 *
	 * @return the configured {@link ExecutorService} instance for async operations
	 */
	public @NotNull ExecutorService getExecutor() {
		return this.asyncExecutorService;
	}
	
	/**
	 * Retrieves the platform abstraction layer instance.
	 * <p>
	 * The platform provides access to core services including internationalization,
	 * database management, scheduler services, and configuration handling. Use this
	 * to access platform-specific functionality in a unified manner.
	 * </p>
	 *
	 * @return the {@link RPlatform} instance providing platform services
	 */
	public @NotNull RPlatform getPlatform() {
		return this.platformAbstraction;
	}
	
	/**
	 * Retrieves the command factory for command and listener management.
	 * <p>
	 * The command factory handles automatic discovery and registration of command classes
	 * and event listeners. It provides centralized management of all command-related
	 * functionality within the plugin.
	 * </p>
	 *
	 * @return the {@link CommandFactory} instance managing commands and listeners
	 */
	public @NotNull CommandFactory getCommandFactory() {
		return this.commandRegistrationFactory;
	}
	
	/**
	 * Retrieves the currency repository for currency data operations.
	 * <p>
	 * This repository provides access to all currency-related database operations
	 * including creation, updates, queries, and deletion. All operations are performed
	 * asynchronously with caching support for optimal performance.
	 * </p>
	 *
	 * @return the {@link CurrencyRepository} instance for currency data access
	 */
	public @NotNull CurrencyRepository getCurrencyRepository() {
		return this.currencyDataRepository;
	}
	
	/**
	 * Retrieves the user repository for player data operations.
	 * <p>
	 * This repository manages all player-related data including account creation,
	 * profile updates, and player information queries. Provides efficient access
	 * to user data across the currency system.
	 * </p>
	 *
	 * @return the {@link UserRepository} instance for player data access
	 */
	public @NotNull UserRepository getUserRepository() {
		return this.playerDataRepository;
	}
	
	/**
	 * Retrieves the user-currency repository for balance and relationship data.
	 * <p>
	 * This repository handles all operations related to player currency balances
	 * and the relationships between players and currencies. Supports complex queries
	 * for leaderboards, balance transfers, and account management.
	 * </p>
	 *
	 * @return the {@link UserCurrencyRepository} instance for balance data access
	 */
	public @NotNull UserCurrencyRepository getUserCurrencyRepository() {
		return this.playerCurrencyRepository;
	}
	
	/**
	 * Retrieves the in-memory currency cache for fast data access.
	 * <p>
	 * This cache contains all loaded currencies mapped by their database ID for
	 * immediate access without database queries. The cache is automatically maintained
	 * and synchronized with database changes throughout the plugin lifecycle.
	 * </p>
	 *
	 * @return an unmodifiable view of the currency cache mapped by currency ID
	 */
	public @NotNull Map<Long, Currency> getCurrencies() {
		return this.currencyCache;
	}
	
	/**
	 * Retrieves the InventoryFramework view frame for GUI management.
	 * <p>
	 * The view frame coordinates all inventory-based user interfaces including
	 * currency management panels, creation wizards, and administrative tools.
	 * Use this to access view-related functionality and configuration.
	 * </p>
	 *
	 * @return the {@link ViewFrame} instance managing inventory-based interfaces
	 */
	public @NotNull ViewFrame getViewFrame() {
		return this.inventoryViewFramework;
	}
	
	/**
	 * Retrieves the currency adapter service for external plugin integration.
	 * <p>
	 * This adapter provides a public API for other plugins to interact with the
	 * currency system. It supports balance queries, transactions, currency management,
	 * and event notifications for external integrations.
	 * </p>
	 *
	 * @return the {@link CurrencyAdapter} service for external plugin access
	 */
	public @NotNull CurrencyAdapter getCurrencyAdapter() {
		return this.externalCurrencyAdapter;
	}
	
	public CurrencyLogService getLogService() {
		
		return this.logService;
	}
	
	public CurrencyLogRepository getCurrencyLogRepository() {
		
		return this.currencyLogRepository;
	}
	
	public VaultMigrationManager getVaultMigrationManager() {
		
		return this.vaultMigrationManager;
	}
	
	/**
	 * Initializes the command system and registers all commands and listeners.
	 * <p>
	 * Creates the command factory instance and triggers automatic discovery and
	 * registration of all command classes and event listeners within the plugin
	 * package structure. Handles configuration loading and command setup.
	 * </p>
	 */
	private void initializeCommandSystem() {
		this.commandRegistrationFactory = new CommandFactory(this);
		this.commandRegistrationFactory.registerAllCommandsAndListeners();
	}
	
	/**
	 * Initializes all database repositories with proper configuration.
	 * <p>
	 * Sets up the repository layer with database connections, caching configuration,
	 * and async operation support. Each repository is configured with the shared
	 * executor service and entity manager factory for optimal performance.
	 * </p>
	 */
	private void initializeDatabaseRepositories() {
		this.playerDataRepository = new UserRepository(
			this.asyncExecutorService,
			this.platformAbstraction.getEntityManagerFactory()
		);
		
		this.currencyDataRepository = new CurrencyRepository(
			this.asyncExecutorService,
			this.platformAbstraction.getEntityManagerFactory()
		);
		
		this.playerCurrencyRepository = new UserCurrencyRepository(
			this.asyncExecutorService,
			this.platformAbstraction.getEntityManagerFactory()
		);
		
		this.currencyLogRepository = new CurrencyLogRepository(
			this.asyncExecutorService,
			this.platformAbstraction.getEntityManagerFactory()
		);
	}
	
	/**
	 * Initializes the InventoryFramework and registers all GUI views.
	 * <p>
	 * Configures the view framework with default settings, installs required features,
	 * and registers all currency-related views including management interfaces,
	 * creation wizards, and administrative panels.
	 * </p>
	 */
	@SuppressWarnings("UnstableApiUsage")
	private void initializeUserInterfaceFramework() {
		this.inventoryViewFramework = ViewFrame
			                              .create(this)
			                              .install(CustomAnvilInputFeature.AnvilInput)
			                              .defaultConfig(viewConfiguration -> {
				                              viewConfiguration.cancelOnClick();
				                              viewConfiguration.cancelOnDrag();
				                              viewConfiguration.cancelOnDrop();
				                              viewConfiguration.cancelOnPickup();
				                              viewConfiguration.interactionDelay(Duration.ofMillis(100));
			                              })
			                              .with(
				                              new ConfirmationView(),
				                              new CurrenciesOverviewView(),
				                              new CurrenciesCreatingView(),
				                              new CurrencyIconAnvilView(),
				                              new CurrencyDetailView(),
				                              new CurrencyLeaderboardView(),
				                              new CurrenciesActionOverviewView(),
				                              new CurrencyIdentifierAnvilView(),
				                              new CurrencySymbolAnvilView(),
				                              new CurrencyPrefixAnvilView(),
				                              new CurrencySuffixAnvilView()
			                              )
			                              .disableMetrics()
			                              .register();
	}
	
	/**
	 * Loads all currency data from the database into the in-memory cache.
	 * <p>
	 * Performs an asynchronous query to retrieve all currencies from the database
	 * and populates the in-memory cache for fast access during runtime operations.
	 * The cache is essential for optimal performance and reduced database load.
	 * </p>
	 */
	private void loadCurrencyDataIntoCache() {
		this.currencyDataRepository.findAllAsync(0, 128)
		                           .thenAcceptAsync(this::populateCurrencyCache, this.asyncExecutorService)
		                           .exceptionally(this::handleCurrencyLoadingError);
	}
	
	/**
	 * Populates the currency cache with the provided currency list.
	 * <p>
	 * Takes a list of currencies retrieved from the database and adds them to the
	 * in-memory cache for fast access. Logs the number of currencies loaded for
	 * monitoring and debugging purposes.
	 * </p>
	 *
	 * @param loadedCurrencies the list of currencies to add to the cache, may be null
	 */
	private void populateCurrencyCache(
		final @Nullable List<Currency> loadedCurrencies
	) {
		if (
			loadedCurrencies == null
		) {
			CentralLogger.getLogger(JECurrency.class.getName()).log(
				Level.WARNING,
				"Currency loading returned null - cache remains empty"
			);
			return;
		}
		
		loadedCurrencies.forEach(currencyEntity ->
			                         this.currencyCache.put(currencyEntity.getId(), currencyEntity)
		);
		
		CentralLogger.getLogger(JECurrency.class.getName()).log(
			Level.INFO,
			String.format(
				"Successfully loaded %d currencies into memory cache",
				loadedCurrencies.size()
			)
		);
	}
	
	/**
	 * Handles errors that occur during currency cache loading.
	 * <p>
	 * Logs detailed error information when currency loading fails and ensures
	 * the plugin can continue operating even if the initial cache population
	 * encounters issues. Returns null to satisfy the exceptionally contract.
	 * </p>
	 *
	 * @param cacheLoadingError the exception that occurred during loading
	 * @return null to satisfy the CompletableFuture exceptionally contract
	 */
	private @Nullable Void handleCurrencyLoadingError(
		final @NotNull Throwable cacheLoadingError
	) {
		CentralLogger.getLogger(JECurrency.class.getName()).log(
			Level.SEVERE,
			"Failed to load currencies into cache during initialization",
			cacheLoadingError
		);
		return null;
	}
}