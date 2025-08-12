package de.jexcellence.currency.command.player.currencies;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import de.jexcellence.currency.JECurrency;
import de.jexcellence.currency.view.currency.CurrenciesActionOverviewView;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Primary command handler for player-initiated currency management operations.
 * <p>
 * This class serves as the main entry point for players to interact with the currency system
 * through a graphical user interface. When executed, it opens the currency management GUI
 * where players can perform all currency-related operations including creation, editing,
 * viewing, and deletion (with appropriate permissions).
 * </p>
 *
 * <h3>GUI-Based Operations:</h3>
 * <ul>
 *   <li><strong>Currency Creation:</strong> Create new currencies through anvil GUI</li>
 *   <li><strong>Currency Editing:</strong> Edit existing currency properties (coming soon)</li>
 *   <li><strong>Currency Deletion:</strong> Remove currencies from the system (coming soon)</li>
 *   <li><strong>Currency Overview:</strong> View all currencies with leaderboards</li>
 *   <li><strong>Balance Reset:</strong> Reset currency balances for all players</li>
 * </ul>
 *
 * <h3>Permission Integration:</h3>
 * <p>
 * The command system integrates with the {@link ECurrenciesPermission} framework to ensure
 * that only authorized users can access the GUI. Individual operations within the GUI
 * perform their own permission checks as needed.
 * </p>
 *
 * <h3>User Interface Integration:</h3>
 * <p>
 * The command opens the {@link CurrenciesActionOverviewView} which provides an intuitive
 * graphical interface for all currency management operations, eliminating the need for
 * complex command-line syntax.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see PlayerCommand
 * @see CurrenciesActionOverviewView
 * @see ECurrenciesPermission
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
	 * Constructs a new player currency command handler with the specified configuration and plugin instance.
	 * <p>
	 * This constructor initializes the command handler with the necessary dependencies to
	 * open the currency management GUI. It establishes connections to the plugin's
	 * infrastructure for accessing the view framework.
	 * </p>
	 *
	 * <h3>Initialization Process:</h3>
	 * <ul>
	 *   <li>Registers the command with the parent command framework</li>
	 *   <li>Establishes connection to the JECurrency plugin instance</li>
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
	}
	
	/**
	 * Handles the execution of currency commands initiated by players.
	 * <p>
	 * This method serves as the primary entry point for currency command operations.
	 * It performs permission validation and opens the graphical user interface
	 * for currency management. All currency operations are now handled through
	 * the GUI interface for a more intuitive user experience.
	 * </p>
	 *
	 * <h3>Execution Flow:</h3>
	 * <ol>
	 *   <li>Validates base currency command permissions</li>
	 *   <li>Opens the currency management GUI interface</li>
	 * </ol>
	 *
	 * <h3>Permission Validation:</h3>
	 * <p>
	 * The command requires the base currencies permission to access the GUI.
	 * Individual operations within the GUI will perform their own permission
	 * checks as needed.
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
		
		// Always open the GUI interface regardless of arguments
		this.jeCurrency.getViewFrame().open(
			CurrenciesActionOverviewView.class,
			commandExecutingPlayer,
			Map.of(
				"plugin",
				this.jeCurrency
			)
		);
	}
	
	/**
	 * Provides tab completion for the currency command.
	 * <p>
	 * Since this command only opens a GUI interface, no tab completion
	 * suggestions are provided. All operations are handled through the
	 * graphical user interface.
	 * </p>
	 *
	 * @param tabCompletionRequestingPlayer the player requesting tab completion, must not be null
	 * @param commandLabel the command label being completed, must not be null
	 * @param currentCommandArguments the current command arguments for context, must not be null
	 * @return an empty list since no tab completion is needed for GUI-only command, never null
	 */
	@Override
	protected @NotNull List<String> onPlayerTabCompletion(
		final @NotNull Player tabCompletionRequestingPlayer,
		final @NotNull String commandLabel,
		final @NotNull String[] currentCommandArguments
	) {
		return new ArrayList<>();
	}
}