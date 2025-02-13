package de.jexcellence.currency.database.entity;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Table(name = "p_currency")
@Entity
public class Currency extends AbstractEntity {

	@Column(name = "prefix")
	private String prefix;

	@Column(name = "suffix")
	private String suffix;

	@Column(name = "currency_name", unique = true, nullable = false)
	private String identifier;

	@Column(name = "symbol", nullable = false)
	private String symbol;

	protected Currency() {}

	public Currency(
		@NotNull String prefix,
		@NotNull String suffix,
		@NotNull String identifier,
		@NotNull String symbol
	) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.identifier = identifier;
		this.symbol = symbol;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Currency currency = (Currency) o;
		return Objects.equals(identifier, currency.identifier);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(identifier);
	}
}
