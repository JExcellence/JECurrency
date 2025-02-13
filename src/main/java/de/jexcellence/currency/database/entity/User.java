package de.jexcellence.currency.database.entity;

import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

@Table(name = "p_player")
@Entity
public class User extends AbstractEntity {

	@Column(name = "unique_id", unique = true, nullable = false)
	private UUID uniqueId;

	@Column(name = "player_name", nullable = false)
	private String playerName;

	protected User() {}

	public User(
		final @NotNull UUID uniqueId,
		final @NotNull String playerName
	) {
		this.uniqueId = uniqueId;
		this.playerName = playerName;
	}

	public User(
		final @NotNull Player player
	) {
		this(player.getUniqueId(), player.getName());
	}

	public UUID getUniqueId() {
		return uniqueId;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		User user = (User) o;
		return Objects.equals(uniqueId, user.uniqueId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(uniqueId);
	}
}
