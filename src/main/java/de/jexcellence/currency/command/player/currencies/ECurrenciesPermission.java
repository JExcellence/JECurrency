package de.jexcellence.currency.command.player.currencies;

import de.jexcellence.evaluable.section.IPermissionNode;

public enum ECurrenciesPermission implements IPermissionNode {

	CURRENCIES("command", "currencies.command"),
	CREATE("commandCreate", "currencies.command.create"),
	DELETE("commandDelete", "currencies.command.delete"),
	EDIT("commandEdit", "currencies.command.update"),
	OVERVIEW("commandOverview", "currencies.command.overview");

	private final String internalName;
	private final String fallbackNode;

	ECurrenciesPermission(
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