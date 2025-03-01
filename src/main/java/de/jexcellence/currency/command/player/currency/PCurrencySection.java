package de.jexcellence.currency.command.player.currency;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class PCurrencySection extends ACommandSection {

	private static final String COMMAND_NAME = "pcurrency";

	public PCurrencySection(
			final EvaluationEnvironmentBuilder environmentBuilder
	) {
		super(COMMAND_NAME, environmentBuilder);
	}
}
