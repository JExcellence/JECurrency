package de.jexcellence.currency.command.console.deposit;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class CDepositSection extends ACommandSection {

	private final static String COMMAND_NAME = "cdeposit";

	public CDepositSection(EvaluationEnvironmentBuilder environmentBuilder) {
		super(COMMAND_NAME, environmentBuilder);
	}
}
