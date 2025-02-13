package de.jexcellence.currency.command.player.currencies;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class PCurrenciesSection extends ACommandSection {
	
	private static final String COMMAND_NAME = "pcurrencies";
	
	public PCurrenciesSection(
			final EvaluationEnvironmentBuilder environmentBuilder
	) {
		super(COMMAND_NAME, environmentBuilder);
	}
}
