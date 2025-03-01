package de.jexcellence.currency.command.console.withdraw;

import de.jexcellence.evaluable.section.ACommandSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class CWithdrawSection extends ACommandSection {

	private final static String COMMAND_NAME = "cwithdraw";

	public CWithdrawSection(EvaluationEnvironmentBuilder environmentBuilder) {
		super(COMMAND_NAME, environmentBuilder);
	}
}
