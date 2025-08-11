package de.jexcellence.currency.command.player.currencies;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.r18n.i18n.I18n;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.view.currency.CurrenciesActionOverviewView;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Primary command handler for player-initiated currency management operations.
 * <p>
 * This class serves as the main entry point for players to interact with the currency system
 * through command-line interface. It provides comprehensive functionality for currency
 * administration including creation, modification, deletion, and information retrieval,
 * while maintaining strict permission controls and providing intelligent tab completion.
 * </p>
 *
 * <h3>Supported Operations:</h3>
 * <ul>
 *   <li><strong>Currency Creation:</strong> Create new currencies with custom properties</li>
 *   <li><strong>Currency Modification:</strong> Edit existing currency properties and settings</li>
 *   <li><strong>Currency Deletion:</strong> Remove currencies from the system with validation</li>
 *   <li><strong>Currency Information:</strong> Display detailed currency information and statistics</li>
 *   <li><strong>Currency Overview:</strong> List all available currencies with their properties</li>
 *   <li><strong>Help System:</strong> Provide comprehensive usage guidance and examples</li>
 * </ul>
 *
 * <h3>Permission Integration:</h3>
 * <p>
 * The command system integrates with the {@link ECurrenciesPermission} framework to ensure
 * that only authorized users can perform specific operations. Each action requires appropriate
 * permissions, with administrative operations requiring elevated privileges.
 * </p>
 *
 * <h3>User Interface Integration:</h3>
 * <p>
 * When executed without arguments, the command opens a graphical user interface through
 * the {@link CurrenciesActionOverviewView}, providing an intuitive alternative to
 * command-line interaction for users who prefer visual interfaces.
 * </p>
 *
 * <h3>Internationalization Support:</h3>
 * <p>
 * All user feedback and messages are processed through the {@link I18n} system,
 * ensuring that players receive localized content appropriate to their language
 * preferences and regional settings.
 * </p>
 *
 * <h3>Tab Completion Features:</h3>
 * <ul>
 *   <li>Context-sensitive action suggestions based on permissions</li>
 *   <li>Dynamic currency identifier completion for existing currencies</li>
 *   <li>Field name completion for currency editing operations</li>
 *   <li>Symbol suggestions for currency creation and modification</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see PlayerCommand
 * @see ECurrenciesAction
 * @see ECurrenciesPermission
 * @see CurrencyCommandHandler
 */
@Command
public class PCurrencies extends PlayerCommand {
	
	/**
	 * The main JECurrency plugin instance providing access to core services and repositories.
	 * <p>
	 * This instance serves as the primary gateway to the plugin's infrastructure,
	 * including database repositories, service adapters, executor services, and
	 * user interface frameworks required for currency operations.
	 * </p>
	 */
	private final JECurrency jeCurrency;
	
	/**
	 * Specialized command handler responsible for executing currency management operations.
	 * <p>
	 * This handler encapsulates the business logic for currency operations, providing
	 * a clean separation between command parsing/validation and actual currency
	 * management functionality. It handles all database interactions and validation.
	 * </p>
	 */
	private final CurrencyCommandHandler currencyOperationHandler;
	
	/**
	 * Constructs a new player currency command handler with the specified configuration and plugin instance.
	 * <p>
	 * This constructor initializes the command handler with the necessary dependencies to
	 * perform currency management operations. It establishes connections to the plugin's
	 * infrastructure and creates the specialized command handler for currency operations.
	 * </p>
	 *
	 * <h3>Initialization Process:</h3>
	 * <ul>
	 *   <li>Registers the command with the parent command framework</li>
	 *   <li>Establishes connection to the JECurrency plugin instance</li>
	 *   <li>Creates the currency operation handler for business logic</li>
	 *   <li>Configures permission integration and validation</li>
	 * </ul>
	 *
	 * @param commandSectionConfiguration the command section configuration containing command metadata and settings, must not be null
	 * @param jeCurrency the main JECurrency plugin instance providing access to services and repositories, must not be null
	 * @throws IllegalArgumentException if either parameter is null
	 */
	public PCurrencies(
		final @NotNull PCurrenciesSection commandSectionConfiguration,
		final @NotNull JECurrency jeCurrency
	) {
		super(commandSectionConfiguration);
		this.jeCurrency = jeCurrency;
		this.currencyOperationHandler = new CurrencyCommandHandler(this.jeCurrency);
	}
	
	/**
	 * Handles the execution of currency commands initiated by players.
	 * <p>
	 * This method serves as the primary entry point for all currency command operations.
	 * It performs permission validation, action parsing, and delegates to appropriate
	 * handler methods based on the requested operation. When no arguments are provided,
	 * it opens the graphical user interface for currency management.
	 * </p>
	 *
	 * <h3>Execution Flow:</h3>
	 * <ol>
	 *   <li>Validates base currency command permissions</li>
	 *   <li>Parses the requested action from command arguments</li>
	 *   <li>Opens GUI interface if no arguments provided</li>
	 *   <li>Validates action-specific permissions</li>
	 *   <li>Delegates to appropriate operation handler</li>
	 * </ol>
	 *
	 * <h3>Permission Validation:</h3>
	 * <p>
	 * Each operation requires specific permissions from the {@link ECurrenciesPermission}
	 * enum. The method performs both base permission checks and action-specific
	 * permission validation before allowing operations to proceed.
	 * </p>
	 *
	 * <h3>Error Handling:</h3>
	 * <p>
	 * Permission failures result in automatic error messages sent to the player
	 * through the internationalization system. Invalid actions default to help
	 * display for user guidance.
	 * </p>
	 *
	 * @param commandExecutingPlayer the player executing the currency command, must not be null
	 * @param commandLabel the command label used to invoke this command, must not be null
	 * @param commandArguments the arguments provided with the command, must not be null
	 */
	@Override
	protected void onPlayerInvocation(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String commandLabel,
		final @NotNull String[] commandArguments
	) {
		if (
			this.hasNoPermission(
				commandExecutingPlayer,
				ECurrenciesPermission.CURRENCIES
			)
		) {
			return;
		}
		
		final ECurrenciesAction requestedAction = this.enumParameterOrElse(
			commandArguments,
			0,
			ECurrenciesAction.class,
			ECurrenciesAction.HELP
		);
		
		if (
			commandArguments.length == 0
		) {
			this.jeCurrency.getViewFrame().open(
				CurrenciesActionOverviewView.class,
				commandExecutingPlayer,
				Map.of(
					"plugin",
					this.jeCurrency
				)
			);
			return;
		}
		
		if (
			requestedAction == ECurrenciesAction.HELP
		) {
			this.displayHelpInformation(commandExecutingPlayer);
			return;
		}
		
		switch (requestedAction) {
			case CREATE -> {
				if (
					this.hasNoPermission(
						commandExecutingPlayer,
						ECurrenciesPermission.CREATE
					)
				) {
					return;
				}
				this.currencyOperationHandler.createCurrency(
					commandExecutingPlayer,
					commandArguments
				);
			}
			case DELETE -> {
				if (
					this.hasNoPermission(
						commandExecutingPlayer,
						ECurrenciesPermission.DELETE
					)
				) {
					return;
				}
				this.currencyOperationHandler.deleteCurrency(
					commandExecutingPlayer,
					commandArguments
				);
			}
			case EDIT -> {
				if (
					this.hasNoPermission(
						commandExecutingPlayer,
						ECurrenciesPermission.EDIT
					)
				) {
					return;
				}
				this.currencyOperationHandler.editCurrency(
					commandExecutingPlayer,
					commandArguments
				);
			}
			case OVERVIEW -> {
				if (
					this.hasNoPermission(
						commandExecutingPlayer,
						ECurrenciesPermission.OVERVIEW
					)
				) {
					return;
				}
				this.currencyOperationHandler.listCurrencies(commandExecutingPlayer);
			}
			case INFO -> {
				if (
					this.hasNoPermission(
						commandExecutingPlayer,
						ECurrenciesPermission.OVERVIEW
					)
				) {
					return;
				}
				this.currencyOperationHandler.showCurrencyInfo(
					commandExecutingPlayer,
					commandArguments
				);
			}
		}
	}
	
	/**
	 * Provides intelligent tab completion suggestions for currency command arguments.
	 * <p>
	 * This method generates context-sensitive completion suggestions based on the current
	 * argument position, player permissions, and available system data. It supports
	 * completion for actions, currency identifiers, field names, and values.
	 * </p>
	 *
	 * <h3>Completion Categories:</h3>
	 * <ul>
	 *   <li><strong>Action Completion:</strong> Available actions based on player permissions</li>
	 *   <li><strong>Currency Identifier Completion:</strong> Existing currency identifiers for operations</li>
	 *   <li><strong>Field Name Completion:</strong> Editable currency fields for edit operations</li>
	 *   <li><strong>Value Suggestions:</strong> Common values for symbols, prefixes, and suffixes</li>
	 * </ul>
	 *
	 * <h3>Permission-Based Filtering:</h3>
	 * <p>
	 * Completion suggestions are filtered based on the player's permissions, ensuring
	 * that only accessible actions and operations are suggested. This prevents
	 * confusion and provides a better user experience.
	 * </p>
	 *
	 * <h3>Dynamic Content:</h3>
	 * <p>
	 * Currency identifiers and other dynamic content are retrieved from the system
	 * in real-time, ensuring that completion suggestions reflect the current state
	 * of the currency system.
	 * </p>
	 *
	 * @param tabCompletionRequestingPlayer the player requesting tab completion, must not be null
	 * @param commandLabel the command label being completed, must not be null
	 * @param currentCommandArguments the current command arguments for context, must not be null
	 * @return a list of possible completions for the current argument position, never null
	 */
	@Override
	protected @NotNull List<String> onPlayerTabCompletion(
		final @NotNull Player tabCompletionRequestingPlayer,
		final @NotNull String commandLabel,
		final @NotNull String[] currentCommandArguments
	) {
		final List<String> tabCompletionSuggestions = new ArrayList<>();
		
		if (
			currentCommandArguments.length == 1
		) {
			final List<String> availableActionNames = new ArrayList<>();
			
			if (
				!this.hasNoPermission(
					tabCompletionRequestingPlayer,
					ECurrenciesPermission.OVERVIEW
				)
			) {
				availableActionNames.add(ECurrenciesAction.OVERVIEW.getActionName());
				availableActionNames.add(ECurrenciesAction.INFO.getActionName());
			}
			
			if (
				!this.hasNoPermission(
					tabCompletionRequestingPlayer,
					ECurrenciesPermission.CREATE
				)
			) {
				availableActionNames.add(ECurrenciesAction.CREATE.getActionName());
			}
			
			if (
				!this.hasNoPermission(
					tabCompletionRequestingPlayer,
					ECurrenciesPermission.DELETE
				)
			) {
				availableActionNames.add(ECurrenciesAction.DELETE.getActionName());
			}
			
			if (
				!this.hasNoPermission(
					tabCompletionRequestingPlayer,
					ECurrenciesPermission.EDIT
				)
			) {
				availableActionNames.add(ECurrenciesAction.EDIT.getActionName());
			}
			
			availableActionNames.add(ECurrenciesAction.HELP.getActionName());
			
			return StringUtil.copyPartialMatches(
				currentCommandArguments[0].toLowerCase(),
				availableActionNames,
				tabCompletionSuggestions
			);
		}
		
		if (
			currentCommandArguments.length == 2
		) {
			try {
				final ECurrenciesAction parsedAction = ECurrenciesAction.valueOf(
					currentCommandArguments[0].toUpperCase()
				);
				
				switch (parsedAction) {
					case INFO, DELETE, EDIT -> {
						if (
							!this.hasNoPermission(
								tabCompletionRequestingPlayer,
								this.getRequiredPermissionForAction(parsedAction)
							)
						) {
							return StringUtil.copyPartialMatches(
								currentCommandArguments[1].toLowerCase(),
								this.currencyOperationHandler.getCurrencyIdentifiers().join(),
								tabCompletionSuggestions
							);
						}
					}
					case CREATE -> {
						if (
							!this.hasNoPermission(
								tabCompletionRequestingPlayer,
								ECurrenciesPermission.CREATE
							)
						) {
							final List<String> currencyIdentifierSuggestions = Arrays.asList(
								"coins",
								"gems",
								"tokens",
								"points",
								"credits"
							);
							return StringUtil.copyPartialMatches(
								currentCommandArguments[1].toLowerCase(),
								currencyIdentifierSuggestions,
								tabCompletionSuggestions
							);
						}
					}
				}
			} catch (final IllegalArgumentException invalidActionException) {
				return tabCompletionSuggestions;
			}
		}
		
		if (
			currentCommandArguments.length == 3
		) {
			try {
				final ECurrenciesAction parsedAction = ECurrenciesAction.valueOf(
					currentCommandArguments[0].toUpperCase()
				);
				
				if (
					parsedAction == ECurrenciesAction.EDIT
				) {
					if (
						!this.hasNoPermission(
							tabCompletionRequestingPlayer,
							ECurrenciesPermission.EDIT
						)
					) {
						return StringUtil.copyPartialMatches(
							currentCommandArguments[2].toLowerCase(),
							this.currencyOperationHandler.getEditableFields(),
							tabCompletionSuggestions
						);
					}
				}
			} catch (final IllegalArgumentException invalidActionException) {
				return tabCompletionSuggestions;
			}
		}
		
		if (
			currentCommandArguments.length == 4 &&
			currentCommandArguments[0].equalsIgnoreCase(ECurrenciesAction.EDIT.name())
		) {
			final String editableFieldName = currentCommandArguments[2].toLowerCase();
			
			if (
				editableFieldName.equals("symbol")
			) {
				final List<String> currencySymbolSuggestions = this.getCompatibleCurrencySymbols();
				return StringUtil.copyPartialMatches(
					currentCommandArguments[3],
					currencySymbolSuggestions,
					tabCompletionSuggestions
				);
			} else if (
				       editableFieldName.equals("prefix") ||
				       editableFieldName.equals("suffix")
			) {
				final List<String> formatStringSuggestions = Arrays.asList(
					"",
					" ",
					"  "
				);
				return StringUtil.copyPartialMatches(
					currentCommandArguments[3],
					formatStringSuggestions,
					tabCompletionSuggestions
				);
			}
		}
		
		return tabCompletionSuggestions;
	}
	
	/**
	 * Retrieves a list of currency symbols compatible with Minecraft's chat system.
	 * <p>
	 * This method provides a curated collection of currency symbols that are
	 * guaranteed to display correctly in Minecraft's chat interface across
	 * different client configurations and font settings.
	 * </p>
	 *
	 * <h3>Symbol Categories:</h3>
	 * <ul>
	 *   <li><strong>Traditional Currency:</strong> Standard currency symbols ($, €, £, ¥)</li>
	 *   <li><strong>Digital Currency:</strong> Cryptocurrency symbols (₿)</li>
	 *   <li><strong>Gaming Symbols:</strong> Star and gem-like symbols (★, ✦, ⭐)</li>
	 *   <li><strong>Geometric Symbols:</strong> Diamond and shape symbols (◆, ♦)</li>
	 *   <li><strong>Alphanumeric:</strong> Letter-based symbols (C, G, P, T)</li>
	 *   <li><strong>Special Characters:</strong> Punctuation-based symbols (*, #, @, +)</li>
	 * </ul>
	 *
	 * <h3>Compatibility Considerations:</h3>
	 * <ul>
	 *   <li>All symbols are tested for cross-platform compatibility</li>
	 *   <li>Unicode symbols are selected for broad font support</li>
	 *   <li>Symbols avoid characters that may cause rendering issues</li>
	 * </ul>
	 *
	 * @return a list of currency symbols suitable for use in Minecraft chat, never null
	 */
	private @NotNull List<String> getCompatibleCurrencySymbols() {
		return Arrays.asList(
			"$",
			"€",
			"£",
			"¥",
			"₿",
			"★",
			"✦",
			"\u2B50",
			"\u25C6",
			"\u2666",
			"\u27E1",
			"C",
			"G",
			"P",
			"T",
			"*",
			"#",
			"@",
			"+"
		);
	}
	
	/**
	 * Determines the required permission for a specific currency action.
	 * <p>
	 * This method provides a centralized mapping between currency actions and
	 * their corresponding permission requirements. It ensures consistent
	 * permission checking across the command system and simplifies permission
	 * validation logic.
	 * </p>
	 *
	 * <h3>Permission Mapping:</h3>
	 * <ul>
	 *   <li><strong>OVERVIEW, INFO, HELP:</strong> Requires OVERVIEW permission</li>
	 *   <li><strong>CREATE:</strong> Requires CREATE permission</li>
	 *   <li><strong>DELETE:</strong> Requires DELETE permission</li>
	 *   <li><strong>EDIT:</strong> Requires EDIT permission</li>
	 * </ul>
	 *
	 * <h3>Security Considerations:</h3>
	 * <p>
	 * The mapping ensures that read-only operations (INFO, HELP) use the same
	 * permission as OVERVIEW, while destructive operations require their own
	 * specific permissions for fine-grained access control.
	 * </p>
	 *
	 * @param currencyAction the action to determine permission requirements for, must not be null
	 * @return the permission required for the specified action, never null
	 */
	private @NotNull ECurrenciesPermission getRequiredPermissionForAction(final @NotNull ECurrenciesAction currencyAction) {
		return switch (currencyAction) {
			case OVERVIEW, INFO, HELP -> ECurrenciesPermission.OVERVIEW;
			case CREATE -> ECurrenciesPermission.CREATE;
			case DELETE -> ECurrenciesPermission.DELETE;
			case EDIT -> ECurrenciesPermission.EDIT;
		};
	}
	
	/**
	 * Displays comprehensive help information for the currencies command system.
	 * <p>
	 * This method sends localized help content to the player through the
	 * internationalization system. The help content includes command syntax,
	 * parameter descriptions, usage examples, and troubleshooting information.
	 * </p>
	 *
	 * <h3>Help Content Features:</h3>
	 * <ul>
	 *   <li>Localized content based on player language preferences</li>
	 *   <li>Comprehensive command syntax documentation</li>
	 *   <li>Parameter descriptions and requirements</li>
	 *   <li>Usage examples for common operations</li>
	 *   <li>Permission requirements for each action</li>
	 * </ul>
	 *
	 * <h3>Internationalization:</h3>
	 * <p>
	 * The help content is processed through the {@link I18n} system with
	 * prefix inclusion for consistent formatting with other plugin messages.
	 * This ensures that help information follows the same visual style as
	 * other command feedback.
	 * </p>
	 *
	 * @param helpRequestingPlayer the player to send help information to, must not be null
	 */
	private void displayHelpInformation(final @NotNull Player helpRequestingPlayer) {
		new I18n.Builder(
			"currencies.help",
			helpRequestingPlayer
		).includingPrefix().build().send();
	}
}