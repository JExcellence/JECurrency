package de.jexcellence.currency.command.player.currency;

import de.jexcellence.evaluable.section.IPermissionNode;

public enum ECurrencyPermission implements IPermissionNode {

	CURRENCY("command", "currency.command"),
	CURRENCY_OTHER("commandOther", "currency.command.other")
	;

	private final String internalName;
	private final String fallbackNode;

	ECurrencyPermission(
		final String internalName,
		final String fallbackNode
	) {
		this.internalName = internalName;
		this.fallbackNode = fallbackNode;
	}

	@Override
	public String getInternalName() {
		return this.internalName;
	}

	@Override
	public String getFallbackNode() {
		return this.fallbackNode;
	}
}