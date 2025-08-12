package de.jexcellence.currency.command.player.currency;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import com.raindropcentral.r18n.i18n.I18n;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.entity.UserCurrency;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command handler for individual player currency operations and balance inquiries.
 * <p>
 * This class provides comprehensive functionality for players to interact with individual
 * currencies within the JECurrency system. It supports both personal balance inquiries
 * and administrative operations for viewing other players' currency information, with
 * appropriate permission validation and localized user feedback.
 * </p>
 *
 * <h3>Supported Operations:</h3>
 * <ul>
 *   <li><strong>Personal Balance Inquiry:</strong> View own balance for specific currencies</li>
 *   <li><strong>Complete Balance Overview:</strong> Display all currency balances for a player</li>
 *   <li><strong>Cross-Player Balance Inquiry:</strong> View other players' currency balances (with permissions)</li>
 *   <li><strong>Administrative Oversight:</strong> Monitor currency usage across multiple players</li>
 * </ul>
 *
 * <h3>Command Syntax:</h3>
 * <ul>
 *   <li><code>/pcurrency</code> - Display all personal currency balances</li>
 *   <li><code>/pcurrency all</code> - Display all personal currency balances (explicit)</li>
 *   <li><code>/pcurrency &lt;currency&gt;</code> - Display personal balance for specific currency</li>
 *   <li><code>/pcurrency all &lt;player&gt;</code> - Display all balances for another player</li>
 *   <li><code>/pcurrency &lt;currency&gt; &lt;player&gt;</code> - Display specific currency balance for another player</li>
 * </ul>
 *
 * <h3>Permission Integration:</h3>
 * <p>
 * The command system integrates with {@link ECurrencyPermission} to ensure appropriate
 * access control. Personal operations require basic currency permissions, while cross-player
 * operations require elevated permissions for privacy and security protection.
 * </p>
 *
 * <h3>Internationalization Support:</h3>
 * <p>
 * All user feedback is processed through the {@link I18n} system, providing localized
 * content based on player language preferences. Error messages, balance displays, and
 * help information are all culturally and linguistically appropriate.
 * </p>
 *
 * <h3>Tab Completion Features:</h3>
 * <ul>
 *   <li>Dynamic currency identifier completion based on available currencies</li>
 *   <li>Online player name completion for cross-player operations</li>
 *   <li>Permission-aware suggestions that respect user access levels</li>
 *   <li>Context-sensitive completion based on argument position</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see PlayerCommand
 * @see ECurrencyPermission
 * @see ECurrencyAction
 */
@Command
public class PCurrency extends PlayerCommand {
	
	/**
	 * The main JECurrency plugin instance providing access to core services and repositories.
	 * <p>
	 * This instance serves as the primary gateway to the plugin's infrastructure,
	 * including currency repositories, adapter services, executor services, and
	 * user interface frameworks required for currency balance operations.
	 * </p>
	 */
	private final JECurrency jeCurrency;
	
	/**
	 * Constructs a new player currency command handler with the specified configuration and plugin instance.
	 * <p>
	 * This constructor initializes the command handler with the necessary dependencies to
	 * perform individual currency operations. It establishes connections to the plugin's
	 * infrastructure and configures the command for player accessibility and permission validation.
	 * </p>
	 *
	 * <h3>Initialization Process:</h3>
	 * <ul>
	 *   <li>Registers the command with the parent command framework</li>
	 *   <li>Establishes connection to the JECurrency plugin instance</li>
	 *   <li>Configures permission integration and validation</li>
	 *   <li>Sets up internationalization and localization support</li>
	 * </ul>
	 *
	 * @param commandSectionConfiguration the command section configuration containing command metadata and settings, must not be null
	 * @param jeCurrency the main JECurrency plugin instance providing access to services and repositories, must not be null
	 * @throws IllegalArgumentException if either parameter is null
	 */
	public PCurrency(
		final @NotNull PCurrencySection commandSectionConfiguration,
		final @NotNull JECurrency jeCurrency
	) {
		super(commandSectionConfiguration);
		this.jeCurrency = jeCurrency;
	}
	
	/**
	 * Handles the execution of individual currency commands initiated by players.
	 * <p>
	 * This method serves as the primary entry point for all individual currency operations.
	 * It performs permission validation, argument parsing, and delegates to appropriate
	 * handler methods based on the requested operation type and target scope.
	 * </p>
	 *
	 * <h3>Execution Flow:</h3>
	 * <ol>
	 *   <li>Validates base currency command permissions</li>
	 *   <li>Parses command arguments to determine operation type</li>
	 *   <li>Handles personal balance inquiries for no arguments or "all" keyword</li>
	 *   <li>Processes cross-player operations with additional permission validation</li>
	 *   <li>Resolves currency identifiers and validates currency existence</li>
	 *   <li>Delegates to appropriate balance display methods</li>
	 * </ol>
	 *
	 * <h3>Permission Validation:</h3>
	 * <p>
	 * Personal operations require {@link ECurrencyPermission#CURRENCY} permission,
	 * while cross-player operations require {@link ECurrencyPermission#CURRENCY_OTHER}
	 * permission for privacy and security protection.
	 * </p>
	 *
	 * <h3>Error Handling:</h3>
	 * <p>
	 * Invalid currency identifiers, non-existent players, and permission failures
	 * result in localized error messages sent to the player through the
	 * internationalization system.
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
				ECurrencyPermission.CURRENCY
			)
		) {
			return;
		}
		
		if (
			commandArguments.length == 0 ||
			(commandArguments.length == 1 && commandArguments[0].equalsIgnoreCase("all"))
		) {
			this.displayAllCurrencyBalances(
				commandExecutingPlayer,
				commandExecutingPlayer
			);
			return;
		}
		
		if (
			commandArguments.length == 2 &&
			commandArguments[0].equalsIgnoreCase("all")
		) {
			if (
				this.hasNoPermission(
					commandExecutingPlayer,
					ECurrencyPermission.CURRENCY_OTHER
				)
			) {
				return;
			}
			
			final OfflinePlayer targetPlayerForAllBalances = this.offlinePlayerParameter(
				commandArguments,
				1,
				true
			);
			
			if (
				!targetPlayerForAllBalances.hasPlayedBefore()
			) {
				new I18n.Builder(
					"general.invalid_player",
					commandExecutingPlayer
				)
					.includingPrefix()
					.withPlaceholders(Map.of(
						"player_name",
						commandArguments[1]
					))
					.build().send();
				return;
			}
			
			this.displayAllCurrencyBalances(
				commandExecutingPlayer,
				targetPlayerForAllBalances
			);
			return;
		}
		
		final String requestedCurrencyIdentifier = this.stringParameter(
			commandArguments,
			0
		);
		
		final Currency resolvedCurrency = this.jeCurrency.getCurrencies().values().stream()
		                                                             .filter(currencyEntity -> currencyEntity.getIdentifier().equalsIgnoreCase(requestedCurrencyIdentifier))
		                                                             .findFirst()
		                                                             .orElse(null);
		
		if (
			resolvedCurrency == null
		) {
			new I18n.Builder(
				"general.invalid_currency",
				commandExecutingPlayer
			)
				.includingPrefix()
				.withPlaceholders(Map.of(
					"currency",
					requestedCurrencyIdentifier,
					"available_currencies",
					this.jeCurrency.getCurrencies().values().stream()
					                           .map(Currency::getIdentifier)
					                           .collect(Collectors.joining(", "))
				))
				.build().send();
			return;
		}
		
		if (
			commandArguments.length > 1
		) {
			if (
				this.hasNoPermission(
					commandExecutingPlayer,
					ECurrencyPermission.CURRENCY_OTHER
				)
			) {
				return;
			}
			
			final OfflinePlayer targetPlayerForSpecificCurrency = this.offlinePlayerParameter(
				commandArguments,
				1,
				true
			);
			
			if (
				!targetPlayerForSpecificCurrency.hasPlayedBefore()
			) {
				new I18n.Builder(
					"general.invalid_player",
					commandExecutingPlayer
				)
					.includingPrefix()
					.withPlaceholders(Map.of(
						"player_name",
						commandArguments[1]
					))
					.build().send();
				return;
			}
			
			this.displaySpecificCurrencyBalance(
				commandExecutingPlayer,
				targetPlayerForSpecificCurrency,
				resolvedCurrency
			);
		} else {
			this.displaySpecificCurrencyBalance(
				commandExecutingPlayer,
				commandExecutingPlayer,
				resolvedCurrency
			);
		}
	}
	
	/**
	 * Provides intelligent tab completion suggestions for currency command arguments.
	 * <p>
	 * This method generates context-sensitive completion suggestions based on the current
	 * argument position, player permissions, and available system data. It supports
	 * completion for currency identifiers, the "all" keyword, and player names.
	 * </p>
	 *
	 * <h3>Completion Categories:</h3>
	 * <ul>
	 *   <li><strong>First Argument:</strong> "all" keyword and available currency identifiers</li>
	 *   <li><strong>Second Argument:</strong> Online player names (with appropriate permissions)</li>
	 * </ul>
	 *
	 * <h3>Permission-Based Filtering:</h3>
	 * <p>
	 * Player name completion is only provided to users with
	 * {@link ECurrencyPermission#CURRENCY_OTHER} permission, ensuring that
	 * completion suggestions respect access control policies.
	 * </p>
	 *
	 * <h3>Dynamic Content:</h3>
	 * <p>
	 * Currency identifiers are retrieved from the system in real-time, and
	 * player names reflect currently online players, ensuring that completion
	 * suggestions are always current and relevant.
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
			final List<String> firstArgumentSuggestions = new ArrayList<>();
			
			firstArgumentSuggestions.add("all");
			firstArgumentSuggestions.addAll(
				this.jeCurrency.getCurrencies().values().stream()
				                           .map(Currency::getIdentifier)
				                           .toList()
			);
			
			return StringUtil.copyPartialMatches(
				currentCommandArguments[0],
				firstArgumentSuggestions,
				tabCompletionSuggestions
			);
		}
		
		if (
			currentCommandArguments.length == 2
		) {
			if (
				!this.hasNoPermission(
					tabCompletionRequestingPlayer,
					ECurrencyPermission.CURRENCY_OTHER
				)
			) {
				final List<String> onlinePlayerNames = Bukkit.getOnlinePlayers().stream()
				                                             .map(Player::getName)
				                                             .collect(Collectors.toList());
				
				return StringUtil.copyPartialMatches(
					currentCommandArguments[1],
					onlinePlayerNames,
					tabCompletionSuggestions
				);
			}
		}
		
		return tabCompletionSuggestions;
	}
	
	/**
	 * Displays the balance of a specific currency for a target player.
	 * <p>
	 * This method retrieves and displays the balance information for a specific currency
	 * and player combination. It handles both personal balance inquiries and cross-player
	 * balance viewing, with appropriate message formatting based on the relationship
	 * between the requesting player and the target player.
	 * </p>
	 *
	 * <h3>Display Features:</h3>
	 * <ul>
	 *   <li>Asynchronous balance retrieval to prevent server lag</li>
	 *   <li>Localized message formatting based on sender-target relationship</li>
	 *   <li>Complete currency information including symbols, prefixes, and suffixes</li>
	 *   <li>Formatted balance display with appropriate decimal precision</li>
	 * </ul>
	 *
	 * <h3>Message Differentiation:</h3>
	 * <p>
	 * The method uses different internationalization keys for personal balance
	 * inquiries versus cross-player balance viewing, ensuring that messages
	 * are contextually appropriate and user-friendly.
	 * </p>
	 *
	 * <h3>Asynchronous Processing:</h3>
	 * <p>
	 * Balance retrieval is performed asynchronously using the plugin's executor
	 * service to prevent blocking the main server thread during database operations.
	 * </p>
	 *
	 * @param messageRecipientPlayer the player who will receive the balance information message, must not be null
	 * @param balanceTargetPlayer the player whose balance is being queried, must not be null
	 * @param queriedCurrency the currency for which to display the balance, must not be null
	 */
	private void displaySpecificCurrencyBalance(
		final @NotNull Player messageRecipientPlayer,
		final @NotNull OfflinePlayer balanceTargetPlayer,
		final @NotNull Currency queriedCurrency
	) {
		this.jeCurrency.getCurrencyAdapter().getBalance(
			    balanceTargetPlayer,
			    queriedCurrency
		    )
		                           .thenAcceptAsync(
			                           retrievedBalance -> {
				                           if (
					                           messageRecipientPlayer.equals(balanceTargetPlayer)
				                           ) {
					                           new I18n.Builder(
						                           "currency.balance.self",
						                           messageRecipientPlayer
					                           )
						                           .includingPrefix()
						                           .withPlaceholders(Map.of(
							                           "currency",
							                           queriedCurrency.getIdentifier(),
							                           "symbol",
							                           queriedCurrency.getSymbol(),
							                           "balance",
							                           String.format("%.2f", retrievedBalance),
							                           "prefix",
							                           queriedCurrency.getPrefix() != null ?
							                           queriedCurrency.getPrefix() : "",
							                           "suffix",
							                           queriedCurrency.getSuffix() != null ?
							                           queriedCurrency.getSuffix() : ""
						                           ))
						                           .build().send();
				                           } else {
					                           new I18n.Builder(
						                           "currency.balance.other",
						                           messageRecipientPlayer
					                           )
						                           .includingPrefix()
						                           .withPlaceholders(Map.of(
							                           "player_name",
							                           balanceTargetPlayer.getName(),
							                           "currency",
							                           queriedCurrency.getIdentifier(),
							                           "symbol",
							                           queriedCurrency.getSymbol(),
							                           "balance",
							                           String.format("%.2f", retrievedBalance),
							                           "prefix",
							                           queriedCurrency.getPrefix() != null ?
							                           queriedCurrency.getPrefix() : "",
							                           "suffix",
							                           queriedCurrency.getSuffix() != null ?
							                           queriedCurrency.getSuffix() : ""
						                           ))
						                           .build().send();
				                           }
			                           },
			                           this.jeCurrency.getExecutor()
		                           );
	}
	
	/**
	 * Displays all currency balances for a target player in a comprehensive overview format.
	 * <p>
	 * This method retrieves and displays a complete overview of all currency balances
	 * for a specified player. It includes header information, individual balance entries,
	 * and footer formatting to provide a professional and readable balance summary.
	 * </p>
	 *
	 * <h3>Display Structure:</h3>
	 * <ul>
	 *   <li><strong>Header:</strong> Contextual header indicating whose balances are displayed</li>
	 *   <li><strong>Balance Entries:</strong> Individual lines for each currency with complete formatting</li>
	 *   <li><strong>Footer:</strong> Closing information and summary details</li>
	 * </ul>
	 *
	 * <h3>Empty State Handling:</h3>
	 * <p>
	 * The method gracefully handles cases where no currencies exist in the system
	 * or where the target player has no currency accounts, providing appropriate
	 * informational messages for each scenario.
	 * </p>
	 *
	 * <h3>Asynchronous Processing:</h3>
	 * <p>
	 * All currency data retrieval is performed asynchronously to prevent server
	 * lag during database operations, with results processed on the plugin's
	 * dedicated executor service.
	 * </p>
	 *
	 * <h3>Internationalization:</h3>
	 * <p>
	 * All messages use different internationalization keys based on whether the
	 * inquiry is for personal balances or another player's balances, ensuring
	 * contextually appropriate messaging.
	 * </p>
	 *
	 * @param messageRecipientPlayer the player who will receive the balance overview message, must not be null
	 * @param balanceTargetPlayer the player whose balances are being displayed, must not be null
	 */
	private void displayAllCurrencyBalances(
		final @NotNull Player messageRecipientPlayer,
		final @NotNull OfflinePlayer balanceTargetPlayer
	) {
		if (
			this.jeCurrency.getCurrencies().isEmpty()
		) {
			new I18n.Builder(
				"currency.error.no_currencies",
				messageRecipientPlayer
			)
				.includingPrefix()
				.build().send();
			return;
		}
		
		this.jeCurrency.getCurrencyAdapter().getUserCurrencies(balanceTargetPlayer)
		                           .thenAcceptAsync(
			                           retrievedUserCurrencies -> {
				                           if (
					                           retrievedUserCurrencies.isEmpty()
				                           ) {
					                           if (
						                           messageRecipientPlayer.equals(balanceTargetPlayer)
					                           ) {
						                           new I18n.Builder(
							                           "currency.balance.no_currencies_self",
							                           messageRecipientPlayer
						                           )
							                           .includingPrefix()
							                           .build().send();
					                           } else {
						                           new I18n.Builder(
							                           "currency.balance.no_currencies_other",
							                           messageRecipientPlayer
						                           )
							                           .includingPrefix()
							                           .withPlaceholders(Map.of(
								                           "player_name",
								                           balanceTargetPlayer.getName()
							                           ))
							                           .build().send();
					                           }
					                           return;
				                           }
				                           
				                           if (
					                           messageRecipientPlayer.equals(balanceTargetPlayer)
				                           ) {
					                           new I18n.Builder(
						                           "currency.balance.all_header_self",
						                           messageRecipientPlayer
					                           )
						                           .build().send();
				                           } else {
					                           new I18n.Builder(
						                           "currency.balance.all_header_other",
						                           messageRecipientPlayer
					                           )
						                           .withPlaceholders(Map.of(
							                           "player_name",
							                           balanceTargetPlayer.getName()
						                           ))
						                           .build().send();
				                           }
				                           
				                           for (final UserCurrency individualUserCurrency : retrievedUserCurrencies) {
					                           final Currency associatedCurrency = individualUserCurrency.getCurrency();
					                           
					                           new I18n.Builder(
						                           "currency.balance.entry",
						                           messageRecipientPlayer
					                           )
						                           .withPlaceholders(Map.of(
							                           "currency",
							                           associatedCurrency.getIdentifier(),
							                           "symbol",
							                           associatedCurrency.getSymbol(),
							                           "balance",
							                           String.format("%.2f", individualUserCurrency.getBalance()),
							                           "prefix",
							                           associatedCurrency.getPrefix(),
							                           "suffix",
							                           associatedCurrency.getSuffix()
						                           ))
						                           .build().send();
				                           }
				                           
				                           new I18n.Builder(
					                           "currency.balance.all_footer",
					                           messageRecipientPlayer
				                           ).build().send();
			                           },
			                           this.jeCurrency.getExecutor()
		                           );
	}
}