package de.jexcellence.currency.database.entity;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

@Table(name = "join_p_player_currency")
@Entity
public class UserCurrency extends AbstractEntity {

	@ManyToOne
	@JoinColumn(name = "p_player_id", nullable = false)
	private User player;

	@ManyToOne
	@JoinColumn(name = "p_currency_id", nullable = false)
	private Currency currency;

	@Column(name = "balance", nullable = false, columnDefinition = "DECIMAL(64, 2) DEFAULT '0.00'")
	private double balance;

	protected UserCurrency() {}

	public UserCurrency(
		final @NotNull User player,
		final @NotNull Currency currency
	) {
		this(player, currency, 0.00);
	}

	public UserCurrency(
		final @NotNull User player,
		final @NotNull Currency currency,
		final double balance
	) {
		this.player = player;
		this.currency = currency;
		this.balance = balance;
	}

	public User getPlayer() {
		return player;
	}

	public Currency getCurrency() {
		return currency;
	}

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}

	public boolean deposit(
		final Double amount
	) {
		try {
			this.balance += amount;
			return true;
		} catch (
			final Exception ignored
		) {
			return false;
		}
	}

	public boolean withdraw(
		final Double amount
	) {
		try {
			if (this.balance < amount)
				return false;

			this.balance -= amount;
			return true;
		} catch (
			final Exception ignored
		) {
			return false;
		}
	}
}
