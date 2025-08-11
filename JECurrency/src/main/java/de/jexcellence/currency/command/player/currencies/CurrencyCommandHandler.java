package de.jexcellence.currency.command.player.currencies;

import com.raindropcentral.r18n.i18n.I18n;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.entity.UserCurrency;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Comprehensive command handler for currency management operations within the JECurrency system.
 * <p>
 * This class provides a complete command-line interface for currency administration, offering
 * functionality for creating, deleting, editing, listing, and displaying detailed information
 * about currencies. It serves as the primary interface between player commands and the
 * underlying currency management system.
 * </p>
 *
 * <h3>Supported Operations:</h3>
 * <ul>
 *   <li><strong>Currency Creation:</strong> Create new currencies with customizable properties</li>
 *   <li><strong>Currency Deletion:</strong> Remove existing currencies from the system</li>
 *   <li><strong>Currency Listing:</strong> Display all available currencies with their properties</li>
 *   <li><strong>Currency Editing:</strong> Modify existing currency properties (symbol, prefix, suffix, identifier)</li>
 *   <li><strong>Currency Information:</strong> Show detailed information about specific currencies</li>
 *   <li><strong>Tab Completion:</strong> Provide intelligent auto-completion for currency identifiers and fields</li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Asynchronous Operations:</strong> All database operations execute on background threads</li>
 *   <li><strong>Internationalization:</strong> All user feedback uses the R18n localization system</li>
 *   <li><strong>Input Validation:</strong> Comprehensive validation of command arguments and parameters</li>
 *   <li><strong>Error Handling:</strong> Graceful handling of edge cases and error conditions</li>
 *   <li><strong>User Experience:</strong> Clear feedback messages and helpful usage information</li>
 * </ul>
 *
 * <h3>Integration Points:</h3>
 * <ul>
 *   <li>Integrates with JECurrency plugin repositories for data persistence</li>
 *   <li>Uses CurrencyAdapter for standardized currency operations</li>
 *   <li>Leverages R18n system for localized user messaging</li>
 *   <li>Provides tab completion support for enhanced user experience</li>
 * </ul>
 *
 * <h3>Security Considerations:</h3>
 * <ul>
 *   <li>All operations validate currency existence before modification</li>
 *   <li>Identifier uniqueness is enforced during creation and editing</li>
 *   <li>Asynchronous operations prevent server lag during database access</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see JECurrency
 * @see Currency
 * @see UserCurrency
 * @see I18n
 */
public class CurrencyCommandHandler {
	
	/**
	 * Reference to the main JECurrency plugin instance.
	 * <p>
	 * Provides access to currency repositories, adapter services, executor services,
	 * and other plugin infrastructure required for executing currency management
	 * operations and maintaining data consistency.
	 * </p>
	 */
	private final JECurrency jeCurrency;
	
	/**
	 * Constructs a new currency command handler with access to the plugin infrastructure.
	 * <p>
	 * Initializes the handler with the necessary dependencies to perform currency
	 * management operations, including access to repositories, adapters, and
	 * executor services for asynchronous operations.
	 * </p>
	 *
	 * @param jeCurrency the main JECurrency plugin instance providing access to services, must not be null
	 * @throws IllegalArgumentException if jeCurrency is null
	 */
	public CurrencyCommandHandler(final @NotNull JECurrency jeCurrency) {
		if (jeCurrency == null) {
			throw new IllegalArgumentException("Currency plugin instance cannot be null");
		}
		this.jeCurrency = jeCurrency;
	}
	
	/**
	 * Handles the creation of a new currency with comprehensive validation and account initialization.
	 * <p>
	 * This method creates a new currency entity with the specified parameters and automatically
	 * initializes player accounts for all existing users. The operation includes validation
	 * to prevent duplicate currency identifiers and provides detailed feedback to the user.
	 * </p>
	 *
	 * <h3>Operation Flow:</h3>
	 * <ol>
	 *   <li>Validates command arguments and parameter count</li>
	 *   <li>Checks for existing currency with the same identifier</li>
	 *   <li>Creates new currency entity with specified properties</li>
	 *   <li>Initializes player accounts for all existing users</li>
	 *   <li>Provides success/failure feedback to the user</li>
	 * </ol>
	 *
	 * <h3>Parameter Requirements:</h3>
	 * <ul>
	 *   <li><strong>args[1]:</strong> Currency identifier (required, must be unique)</li>
	 *   <li><strong>args[2]:</strong> Currency symbol (required)</li>
	 *   <li><strong>args[3]:</strong> Currency prefix (optional, defaults to empty string)</li>
	 *   <li><strong>args[4]:</strong> Currency suffix (optional, defaults to empty string)</li>
	 * </ul>
	 *
	 * @param commandExecutingPlayer the player executing the currency creation command, must not be null
	 * @param commandArguments command arguments containing currency parameters, must not be null
	 */
	public void createCurrency(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String[] commandArguments
	) {
		if (
			commandArguments.length < 3
		) {
			this.sendUsageMessage(
				commandExecutingPlayer,
				"create <identifier> <symbol> [prefix] [suffix]"
			);
			return;
		}
		
		final String currencyIdentifier = commandArguments[1];
		final String currencySymbol = commandArguments[2];
		final String currencyPrefix = commandArguments.length > 3 ?
		                              commandArguments[3] :
		                              "";
		final String currencySuffix = commandArguments.length > 4 ?
		                              commandArguments[4] :
		                              "";
		
		this.jeCurrency.getCurrencyAdapter().hasGivenCurrency(currencyIdentifier)
		                           .thenAcceptAsync(
			                           currencyExists -> {
				                           if (currencyExists) {
					                           this.sendCurrencyAlreadyExistsMessage(commandExecutingPlayer, currencyIdentifier);
					                           return;
				                           }
				                           
				                           this.executeNewCurrencyCreation(
					                           commandExecutingPlayer,
					                           currencyIdentifier,
					                           currencySymbol,
					                           currencyPrefix,
					                           currencySuffix
				                           );
			                           },
			                           this.jeCurrency.getExecutor()
		                           );
	}
	
	/**
	 * Handles the deletion of an existing currency with validation and confirmation.
	 * <p>
	 * This method removes a currency from the system after validating its existence.
	 * The operation includes comprehensive error handling and provides clear feedback
	 * about the success or failure of the deletion operation.
	 * </p>
	 *
	 * <h3>Operation Flow:</h3>
	 * <ol>
	 *   <li>Validates command arguments and parameter count</li>
	 *   <li>Verifies currency exists in the system</li>
	 *   <li>Locates currency entity in the database</li>
	 *   <li>Performs deletion operation</li>
	 *   <li>Provides success/failure feedback to the user</li>
	 * </ol>
	 *
	 * <h3>Parameter Requirements:</h3>
	 * <ul>
	 *   <li><strong>args[1]:</strong> Currency identifier (required, must exist)</li>
	 * </ul>
	 *
	 * @param commandExecutingPlayer the player executing the currency deletion command, must not be null
	 * @param commandArguments command arguments containing the currency identifier, must not be null
	 */
	public void deleteCurrency(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String[] commandArguments
	) {
		if (
			commandArguments.length < 2
		) {
			this.sendUsageMessage(
				commandExecutingPlayer,
				"delete <identifier>"
			);
			return;
		}
		
		final String currencyIdentifier = commandArguments[1];
		
		this.jeCurrency.getCurrencyAdapter().hasGivenCurrency(currencyIdentifier)
		                           .thenAcceptAsync(
			                           currencyExists -> {
				                           if (!currencyExists) {
					                           this.sendCurrencyNotFoundMessage(commandExecutingPlayer, currencyIdentifier);
					                           return;
				                           }
				                           
				                           this.executeCurrencyDeletion(commandExecutingPlayer, currencyIdentifier);
			                           },
			                           this.jeCurrency.getExecutor()
		                           );
	}
	
	/**
	 * Lists all available currencies with their properties and statistics.
	 * <p>
	 * This method retrieves all currencies from the database and displays them
	 * in a formatted list with comprehensive information about each currency.
	 * If no currencies exist, it provides appropriate feedback to the user.
	 * </p>
	 *
	 * <h3>Display Information:</h3>
	 * <ul>
	 *   <li>Currency identifier and symbol</li>
	 *   <li>Prefix and suffix formatting</li>
	 *   <li>Total count of available currencies</li>
	 *   <li>Empty state message if no currencies exist</li>
	 * </ul>
	 *
	 * @param commandExecutingPlayer the player requesting the currency list, must not be null
	 */
	public void listCurrencies(final @NotNull Player commandExecutingPlayer) {
		CompletableFuture.supplyAsync(
			() -> this.jeCurrency.getCurrencyRepository().findAll(0, 128),
			this.jeCurrency.getExecutor()
		).thenAcceptAsync(
			availableCurrencies -> {
				if (
					availableCurrencies.isEmpty()
				) {
					this.sendEmptyCurrencyListMessage(commandExecutingPlayer);
					return;
				}
				
				this.sendCurrencyListHeader(commandExecutingPlayer, availableCurrencies.size());
				this.sendCurrencyListEntries(commandExecutingPlayer, availableCurrencies);
			},
			this.jeCurrency.getExecutor()
		);
	}
	
	/**
	 * Edits a specific field of an existing currency with validation and persistence.
	 * <p>
	 * This method allows modification of currency properties including symbol, prefix,
	 * suffix, and identifier. It includes comprehensive validation to ensure data
	 * integrity and prevent conflicts with existing currencies.
	 * </p>
	 *
	 * <h3>Editable Fields:</h3>
	 * <ul>
	 *   <li><strong>symbol:</strong> The currency symbol displayed in transactions</li>
	 *   <li><strong>prefix:</strong> Text displayed before currency amounts</li>
	 *   <li><strong>suffix:</strong> Text displayed after currency amounts</li>
	 *   <li><strong>identifier:</strong> Unique currency identifier (validated for uniqueness)</li>
	 * </ul>
	 *
	 * <h3>Parameter Requirements:</h3>
	 * <ul>
	 *   <li><strong>args[1]:</strong> Currency identifier (required, must exist)</li>
	 *   <li><strong>args[2]:</strong> Field name to edit (required, must be valid)</li>
	 *   <li><strong>args[3]:</strong> New value for the field (required)</li>
	 * </ul>
	 *
	 * @param commandExecutingPlayer the player executing the currency edit command, must not be null
	 * @param commandArguments command arguments containing currency identifier, field, and value, must not be null
	 */
	public void editCurrency(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String[] commandArguments
	) {
		if (
			commandArguments.length < 4
		) {
			this.sendUsageMessage(
				commandExecutingPlayer,
				"edit <identifier> <field> <value>"
			);
			this.sendEditableFieldsMessage(commandExecutingPlayer);
			return;
		}
		
		final String currencyIdentifier = commandArguments[1];
		final String fieldToEdit = commandArguments[2].toLowerCase();
		final String newFieldValue = commandArguments[3];
		
		this.jeCurrency.getCurrencyAdapter().hasGivenCurrency(currencyIdentifier)
		                           .thenAcceptAsync(
			                           currencyExists -> {
				                           if (!currencyExists) {
					                           this.sendCurrencyNotFoundMessage(commandExecutingPlayer, currencyIdentifier);
					                           return;
				                           }
				                           
				                           this.executeCurrencyFieldEdit(
					                           commandExecutingPlayer,
					                           currencyIdentifier,
					                           fieldToEdit,
					                           newFieldValue
				                           );
			                           },
			                           this.jeCurrency.getExecutor()
		                           );
	}
	
	/**
	 * Shows detailed information about a specific currency including all properties.
	 * <p>
	 * This method retrieves and displays comprehensive information about a currency,
	 * including its identifier, symbol, prefix, suffix, and other relevant details.
	 * The information is formatted for easy reading and understanding.
	 * </p>
	 *
	 * <h3>Displayed Information:</h3>
	 * <ul>
	 *   <li>Currency identifier and symbol</li>
	 *   <li>Prefix and suffix formatting strings</li>
	 *   <li>Currency creation and modification details</li>
	 *   <li>Usage statistics and player account information</li>
	 * </ul>
	 *
	 * <h3>Parameter Requirements:</h3>
	 * <ul>
	 *   <li><strong>args[1]:</strong> Currency identifier (required, must exist)</li>
	 * </ul>
	 *
	 * @param commandExecutingPlayer the player requesting currency information, must not be null
	 * @param commandArguments command arguments containing the currency identifier, must not be null
	 */
	public void showCurrencyInfo(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String[] commandArguments
	) {
		if (
			commandArguments.length < 2
		) {
			this.sendUsageMessage(
				commandExecutingPlayer,
				"info <identifier>"
			);
			return;
		}
		
		final String currencyIdentifier = commandArguments[1];
		
		CompletableFuture.supplyAsync(
			() -> this.jeCurrency.getCurrencyRepository().findByAttributes(
				Map.of("identifier", currencyIdentifier)
			),
			this.jeCurrency.getExecutor()
		).thenAcceptAsync(
			currencyEntity -> {
				if (currencyEntity == null) {
					this.sendCurrencyNotFoundMessage(commandExecutingPlayer, currencyIdentifier);
					return;
				}
				
				this.sendCurrencyInformationDisplay(commandExecutingPlayer, currencyEntity);
			},
			this.jeCurrency.getExecutor()
		);
	}
	
	/**
	 * Retrieves a list of all available currency identifiers for tab completion support.
	 * <p>
	 * This method provides asynchronous access to currency identifiers for use in
	 * command tab completion, improving the user experience by offering intelligent
	 * auto-completion suggestions based on existing currencies in the system.
	 * </p>
	 *
	 * @return a CompletableFuture containing a list of currency identifiers for tab completion
	 */
	public @NotNull CompletableFuture<List<String>> getCurrencyIdentifiers() {
		return CompletableFuture.supplyAsync(
			() -> this.jeCurrency.getCurrencyRepository().findAll(0, 128)
			                                 .stream()
			                                 .map(Currency::getIdentifier)
			                                 .toList(),
			this.jeCurrency.getExecutor()
		);
	}
	
	/**
	 * Retrieves a list of editable currency fields for tab completion support.
	 * <p>
	 * This method provides a static list of field names that can be edited through
	 * the currency edit command, supporting tab completion for improved user experience
	 * and reduced command errors.
	 * </p>
	 *
	 * @return a list of editable field names for tab completion
	 */
	public @NotNull List<String> getEditableFields() {
		return Arrays.asList(
			"symbol",
			"prefix",
			"suffix",
			"identifier"
		);
	}
	
	/**
	 * Executes the actual currency creation process with account initialization.
	 * <p>
	 * This private method handles the core currency creation logic, including
	 * entity creation, database persistence, and automatic player account
	 * initialization for all existing users in the system.
	 * </p>
	 *
	 * @param commandExecutingPlayer the player executing the command, must not be null
	 * @param currencyIdentifier the unique identifier for the new currency, must not be null
	 * @param currencySymbol the symbol for the new currency, must not be null
	 * @param currencyPrefix the prefix for the new currency, must not be null
	 * @param currencySuffix the suffix for the new currency, must not be null
	 */
	private void executeNewCurrencyCreation(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String currencyIdentifier,
		final @NotNull String currencySymbol,
		final @NotNull String currencyPrefix,
		final @NotNull String currencySuffix
	) {
		final Currency newCurrencyEntity = new Currency(
			currencyPrefix,
			currencySuffix,
			currencyIdentifier,
			currencySymbol,
			Material.GOLD_INGOT
		);
		
		this.jeCurrency.getCurrencyAdapter().createCurrency(newCurrencyEntity)
		                           .thenAcceptAsync(
			                           creationSuccessful -> {
				                           if (creationSuccessful) {
					                           this.sendCurrencyCreationSuccessMessage(commandExecutingPlayer, currencyIdentifier);
					                           this.initializePlayerAccountsForNewCurrency(
						                           commandExecutingPlayer,
						                           newCurrencyEntity,
						                           currencyIdentifier
					                           );
				                           } else {
					                           this.sendCurrencyCreationFailedMessage(commandExecutingPlayer);
				                           }
			                           },
			                           this.jeCurrency.getExecutor()
		                           );
	}
	
	/**
	 * Initializes player accounts for a newly created currency.
	 * <p>
	 * This private method creates UserCurrency entities for all existing users
	 * when a new currency is added to the system, ensuring that all players
	 * have accounts for the new currency with zero initial balance.
	 * </p>
	 *
	 * @param commandExecutingPlayer the player who created the currency, must not be null
	 * @param newCurrencyEntity the newly created currency entity, must not be null
	 * @param currencyIdentifier the identifier of the new currency, must not be null
	 */
	private void initializePlayerAccountsForNewCurrency(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull Currency newCurrencyEntity,
		final @NotNull String currencyIdentifier
	) {
		this.jeCurrency.getUserRepository().findAllAsync(0, 128)
		                           .thenAcceptAsync(
			                           existingUsers -> {
				                           existingUsers.forEach(userEntity ->
					                                                 this.jeCurrency.getUserCurrencyRepository().create(
						                                                 new UserCurrency(userEntity, newCurrencyEntity)
					                                                 )
				                           );
				                           
				                           this.sendPlayerAccountsCreatedMessage(
					                           commandExecutingPlayer,
					                           existingUsers.size(),
					                           currencyIdentifier
				                           );
			                           },
			                           this.jeCurrency.getExecutor()
		                           );
	}
	
	/**
	 * Executes the actual currency deletion process with database cleanup.
	 * <p>
	 * This private method handles the core currency deletion logic, including
	 * entity lookup, database removal, and appropriate success/failure feedback.
	 * </p>
	 *
	 * @param commandExecutingPlayer the player executing the deletion command, must not be null
	 * @param currencyIdentifier the identifier of the currency to delete, must not be null
	 */
	private void executeCurrencyDeletion(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String currencyIdentifier
	) {
		CompletableFuture.supplyAsync(
			() -> {
				final Currency currencyToDelete = this.jeCurrency.getCurrencyRepository()
				                                                             .findByAttributes(Map.of("identifier", currencyIdentifier));
				
				if (currencyToDelete != null) {
					this.jeCurrency.getCurrencyRepository().delete(currencyToDelete.getId());
					return true;
				}
				return false;
			},
			this.jeCurrency.getExecutor()
		).thenAcceptAsync(
			deletionSuccessful -> {
				if (deletionSuccessful) {
					this.sendCurrencyDeletionSuccessMessage(commandExecutingPlayer, currencyIdentifier);
				} else {
					this.sendCurrencyDeletionFailedMessage(commandExecutingPlayer);
				}
			},
			this.jeCurrency.getExecutor()
		);
	}
	
	/**
	 * Executes the currency field editing process with validation and persistence.
	 * <p>
	 * This private method handles the core field editing logic, including
	 * field validation, value assignment, uniqueness checking for identifiers,
	 * and database persistence of changes.
	 * </p>
	 *
	 * @param commandExecutingPlayer the player executing the edit command, must not be null
	 * @param currencyIdentifier the identifier of the currency to edit, must not be null
	 * @param fieldToEdit the name of the field to modify, must not be null
	 * @param newFieldValue the new value for the field, must not be null
	 */
	private void executeCurrencyFieldEdit(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String currencyIdentifier,
		final @NotNull String fieldToEdit,
		final @NotNull String newFieldValue
	) {
		CompletableFuture.supplyAsync(
			() -> {
				final Currency currencyToEdit = this.jeCurrency.getCurrencyRepository()
				                                                           .findByAttributes(Map.of("identifier", currencyIdentifier));
				
				if (currencyToEdit != null) {
					final boolean fieldUpdateSuccessful = this.updateCurrencyField(
						currencyToEdit,
						fieldToEdit,
						newFieldValue
					);
					
					if (fieldUpdateSuccessful) {
						this.jeCurrency.getCurrencyRepository().update(currencyToEdit);
						return true;
					}
				}
				return false;
			},
			this.jeCurrency.getExecutor()
		).thenAcceptAsync(
			editSuccessful -> {
				if (editSuccessful) {
					this.sendCurrencyEditSuccessMessage(
						commandExecutingPlayer,
						currencyIdentifier,
						fieldToEdit,
						newFieldValue
					);
				} else {
					this.sendCurrencyEditFailedMessage(commandExecutingPlayer);
				}
			},
			this.jeCurrency.getExecutor()
		);
	}
	
	/**
	 * Updates a specific field of a currency entity with validation.
	 * <p>
	 * This private method handles the actual field modification logic,
	 * including validation for identifier uniqueness and field existence.
	 * </p>
	 *
	 * @param currencyEntity the currency entity to modify, must not be null
	 * @param fieldName the name of the field to update, must not be null
	 * @param newValue the new value for the field, must not be null
	 * @return true if the field was successfully updated, false otherwise
	 */
	private boolean updateCurrencyField(
		final @NotNull Currency currencyEntity,
		final @NotNull String fieldName,
		final @NotNull String newValue
	) {
		switch (fieldName) {
			case "symbol" -> {
				currencyEntity.setSymbol(newValue);
				return true;
			}
			case "prefix" -> {
				currencyEntity.setPrefix(newValue);
				return true;
			}
			case "suffix" -> {
				currencyEntity.setSuffix(newValue);
				return true;
			}
			case "identifier" -> {
				final Currency existingCurrencyWithIdentifier = this.jeCurrency
					                                                .getCurrencyRepository()
					                                                .findByAttributes(Map.of("identifier", newValue));
				
				if (existingCurrencyWithIdentifier != null) {
					return false;
				}
				
				currencyEntity.setIdentifier(newValue);
				return true;
			}
			default -> {
				return false;
			}
		}
	}
	
	/**
	 * Sends currency list header message with count information.
	 * <p>
	 * This private method sends a formatted header message displaying
	 * the total number of available currencies in the system.
	 * </p>
	 *
	 * @param targetPlayer the player to send the message to, must not be null
	 * @param currencyCount the total number of currencies available
	 */
	private void sendCurrencyListHeader(
		final @NotNull Player targetPlayer,
		final int currencyCount
	) {
		new I18n.Builder("currency.list.header", targetPlayer)
			.includingPrefix()
			.withPlaceholder("count", String.valueOf(currencyCount))
			.build()
			.send();
	}
	
	/**
	 * Sends individual currency list entries with detailed information.
	 * <p>
	 * This private method iterates through the currency list and sends
	 * formatted entries displaying each currency's properties.
	 * </p>
	 *
	 * @param targetPlayer the player to send the entries to, must not be null
	 * @param currencyList the list of currencies to display, must not be null
	 */
	private void sendCurrencyListEntries(
		final @NotNull Player targetPlayer,
		final @NotNull List<Currency> currencyList
	) {
		for (final Currency currencyEntity : currencyList) {
			new I18n.Builder("currency.list.entry", targetPlayer)
				.includingPrefix()
				.withPlaceholder("identifier", currencyEntity.getIdentifier())
				.withPlaceholder("symbol", currencyEntity.getSymbol())
				.withPlaceholder("prefix", currencyEntity.getPrefix())
				.withPlaceholder("suffix", currencyEntity.getSuffix())
				.build()
				.send();
		}
	}
	
	/**
	 * Sends comprehensive currency information display to the player.
	 * <p>
	 * This private method formats and sends detailed information about
	 * a specific currency, including all its properties and configuration.
	 * </p>
	 *
	 * @param targetPlayer the player to send the information to, must not be null
	 * @param currencyEntity the currency entity to display information for, must not be null
	 */
	private void sendCurrencyInformationDisplay(
		final @NotNull Player targetPlayer,
		final @NotNull Currency currencyEntity
	) {
		new I18n.Builder("currency.info.header", targetPlayer)
			.includingPrefix()
			.withPlaceholder("identifier", currencyEntity.getIdentifier())
			.build()
			.send();
		
		new I18n.Builder("currency.info.details", targetPlayer)
			.includingPrefix()
			.withPlaceholder("symbol", currencyEntity.getSymbol())
			.withPlaceholder("prefix", currencyEntity.getPrefix())
			.withPlaceholder("suffix", currencyEntity.getSuffix())
			.build()
			.send();
	}
	
	/**
	 * Sends a usage message to the player for command syntax guidance.
	 * <p>
	 * This private method provides standardized usage information to help
	 * players understand the correct syntax for currency commands.
	 * </p>
	 *
	 * @param targetPlayer the player to send the usage message to, must not be null
	 * @param usageSyntax the usage syntax string to display, must not be null
	 */
	private void sendUsageMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String usageSyntax
	) {
		new I18n.Builder("currency.command.usage", targetPlayer)
			.includingPrefix()
			.withPlaceholder("usage", usageSyntax)
			.build()
			.send();
	}
	
	/**
	 * Sends information about editable currency fields to the player.
	 * <p>
	 * This private method provides guidance about which fields can be
	 * modified through the currency edit command.
	 * </p>
	 *
	 * @param targetPlayer the player to send the fields information to, must not be null
	 */
	private void sendEditableFieldsMessage(final @NotNull Player targetPlayer) {
		new I18n.Builder("currency.edit.fields", targetPlayer)
			.includingPrefix()
			.build()
			.send();
	}
	
	/**
	 * Sends currency already exists error message to the player.
	 * <p>
	 * This private method notifies the player when attempting to create
	 * a currency with an identifier that already exists in the system.
	 * </p>
	 *
	 * @param targetPlayer the player to send the error message to, must not be null
	 * @param currencyIdentifier the identifier that already exists, must not be null
	 */
	private void sendCurrencyAlreadyExistsMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String currencyIdentifier
	) {
		new I18n.Builder("currency.create.already_exists", targetPlayer)
			.includingPrefix()
			.withPlaceholder("identifier", currencyIdentifier)
			.build()
			.send();
	}
	
	/**
	 * Sends currency not found error message to the player.
	 * <p>
	 * This private method notifies the player when attempting to operate
	 * on a currency that doesn't exist in the system.
	 * </p>
	 *
	 * @param targetPlayer the player to send the error message to, must not be null
	 * @param currencyIdentifier the identifier that was not found, must not be null
	 */
	private void sendCurrencyNotFoundMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String currencyIdentifier
	) {
		new I18n.Builder("currency.delete.not_found", targetPlayer)
			.includingPrefix()
			.withPlaceholder("identifier", currencyIdentifier)
			.build()
			.send();
	}
	
	/**
	 * Sends empty currency list message to the player.
	 * <p>
	 * This private method notifies the player when no currencies
	 * exist in the system during a list operation.
	 * </p>
	 *
	 * @param targetPlayer the player to send the message to, must not be null
	 */
	private void sendEmptyCurrencyListMessage(final @NotNull Player targetPlayer) {
		new I18n.Builder("currency.list.empty", targetPlayer)
			.includingPrefix()
			.build()
			.send();
	}
	
	/**
	 * Sends currency creation success message to the player.
	 * <p>
	 * This private method confirms successful currency creation
	 * with the specified identifier.
	 * </p>
	 *
	 * @param targetPlayer the player to send the success message to, must not be null
	 * @param currencyIdentifier the identifier of the created currency, must not be null
	 */
	private void sendCurrencyCreationSuccessMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String currencyIdentifier
	) {
		new I18n.Builder("currency.create.success", targetPlayer)
			.includingPrefix()
			.withPlaceholder("identifier", currencyIdentifier)
			.build()
			.send();
	}
	
	/**
	 * Sends currency creation failed message to the player.
	 * <p>
	 * This private method notifies the player when currency
	 * creation fails due to system errors.
	 * </p>
	 *
	 * @param targetPlayer the player to send the error message to, must not be null
	 */
	private void sendCurrencyCreationFailedMessage(final @NotNull Player targetPlayer) {
		new I18n.Builder("currency.create.failed", targetPlayer)
			.includingPrefix()
			.build()
			.send();
	}
	
	/**
	 * Sends player accounts created confirmation message.
	 * <p>
	 * This private method confirms that player accounts have been
	 * successfully created for a new currency.
	 * </p>
	 *
	 * @param targetPlayer the player to send the confirmation to, must not be null
	 * @param playerCount the number of player accounts created
	 * @param currencyIdentifier the identifier of the currency, must not be null
	 */
	private void sendPlayerAccountsCreatedMessage(
		final @NotNull Player targetPlayer,
		final int playerCount,
		final @NotNull String currencyIdentifier
	) {
		new I18n.Builder("currency.create.success.player_accounts_created", targetPlayer)
			.includingPrefix()
			.withPlaceholders(Map.of(
				"player_amount", playerCount,
				"identifier", currencyIdentifier
			))
			.build()
			.send();
	}
	
	/**
	 * Sends currency deletion success message to the player.
	 * <p>
	 * This private method confirms successful currency deletion
	 * with the specified identifier.
	 * </p>
	 *
	 * @param targetPlayer the player to send the success message to, must not be null
	 * @param currencyIdentifier the identifier of the deleted currency, must not be null
	 */
	private void sendCurrencyDeletionSuccessMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String currencyIdentifier
	) {
		new I18n.Builder("currency.delete.success", targetPlayer)
			.includingPrefix()
			.withPlaceholder("identifier", currencyIdentifier)
			.build()
			.send();
	}
	
	/**
	 * Sends currency deletion failed message to the player.
	 * <p>
	 * This private method notifies the player when currency
	 * deletion fails due to system errors.
	 * </p>
	 *
	 * @param targetPlayer the player to send the error message to, must not be null
	 */
	private void sendCurrencyDeletionFailedMessage(final @NotNull Player targetPlayer) {
		new I18n.Builder("currency.delete.failed", targetPlayer)
			.includingPrefix()
			.build()
			.send();
	}
	
	/**
	 * Sends currency edit success message to the player.
	 * <p>
	 * This private method confirms successful currency field
	 * modification with details about the change.
	 * </p>
	 *
	 * @param targetPlayer the player to send the success message to, must not be null
	 * @param currencyIdentifier the identifier of the edited currency, must not be null
	 * @param fieldName the name of the field that was edited, must not be null
	 * @param newValue the new value that was set, must not be null
	 */
	private void sendCurrencyEditSuccessMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String currencyIdentifier,
		final @NotNull String fieldName,
		final @NotNull String newValue
	) {
		new I18n.Builder("currency.edit.success", targetPlayer)
			.includingPrefix()
			.withPlaceholder("identifier", currencyIdentifier)
			.withPlaceholder("field", fieldName)
			.withPlaceholder("value", newValue)
			.build()
			.send();
	}
	
	/**
	 * Sends currency edit failed message to the player.
	 * <p>
	 * This private method notifies the player when currency
	 * editing fails due to validation or system errors.
	 * </p>
	 *
	 * @param targetPlayer the player to send the error message to, must not be null
	 */
	private void sendCurrencyEditFailedMessage(final @NotNull Player targetPlayer) {
		new I18n.Builder("currency.edit.failed", targetPlayer)
			.includingPrefix()
			.build()
			.send();
	}
}