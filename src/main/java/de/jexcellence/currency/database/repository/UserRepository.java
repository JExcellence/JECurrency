package de.jexcellence.currency.database.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.jexcellence.currency.database.entity.User;
import de.jexcellence.hibernate.repository.AbstractCRUDRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class UserRepository extends AbstractCRUDRepository<User, Long> {

	private final ExecutorService executor;
	private final Cache<UUID, User> cache;

	public UserRepository(
		final ExecutorService executor,
		final EntityManagerFactory entityManagerFactory
	) {
		super(entityManagerFactory, User.class);
		this.cache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
		this.executor = executor;
	}

	@Override
	public List<User> findAll(
		final int pageNumber,
		final int pageSize
	) {
		if (! this.cache.asMap().values().isEmpty())
			return this.cache.asMap().values().stream().toList();

		List<User> foundWorlds = super.findAll(pageNumber, pageSize);
		foundWorlds.forEach(user -> this.cache.put(user.getUniqueId(), user));
		return foundWorlds;
	}

	public CompletableFuture<List<User>> findAllAsync(
		final int pageNumber,
		final int pageSize
	) {
		return CompletableFuture.supplyAsync(() -> this.findAll(pageNumber, pageSize), this.executor);
	}

	public User findByUniqueId(
		final @NotNull UUID uniqueId
	) {
		User cachedUser = this.cache.getIfPresent(uniqueId);

		if (cachedUser == null) {
			User foundUser = super.findByAttributes(Map.of("uniqueId", uniqueId));
			if (foundUser != null)
				this.cache.put(foundUser.getUniqueId(), foundUser);
			return foundUser;
		}

		return cachedUser;
	}

	public CompletableFuture<User> findByUniqueIdAsync(
		final @NotNull UUID uniqueId
	) {
		return CompletableFuture.supplyAsync(() -> this.findByUniqueId(uniqueId), this.executor);
	}

	@Override
	public User create(User entity) {
		User mvWorld = super.create(entity);
		this.cache.put(mvWorld.getUniqueId(), mvWorld);
		return mvWorld;
	}

	@Override
	public User update(User entity) {
		User mvWorld = super.update(entity);
		this.cache.put(mvWorld.getUniqueId(), mvWorld);
		return mvWorld;
	}

	@Override
	public void delete(Long id) {
		super.delete(id);
		this.cache.asMap().values().removeIf(user -> user.getId().equals(id));
	}
}
