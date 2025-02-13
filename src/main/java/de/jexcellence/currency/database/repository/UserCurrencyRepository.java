package de.jexcellence.currency.database.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.entity.UserCurrency;
import de.jexcellence.hibernate.repository.AbstractCRUDRepository;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class UserCurrencyRepository extends AbstractCRUDRepository<UserCurrency, Long> {

	private final ExecutorService executor;
	private final Cache<Long, UserCurrency> cache;

	public UserCurrencyRepository(
		final ExecutorService executor,
		final EntityManagerFactory entityManagerFactory
	) {
		super(entityManagerFactory, UserCurrency.class);
		this.cache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
		this.executor = executor;
	}

	@Override
	public List<UserCurrency> findAll(
		final int pageNumber,
		final int pageSize
	) {
		if (! this.cache.asMap().values().isEmpty())
			return this.cache.asMap().values().stream().toList();

		List<UserCurrency> foundUserCurrency = super.findAll(pageNumber, pageSize);
		foundUserCurrency.forEach(usercurrency -> this.cache.put(usercurrency.getId(), usercurrency));
		return foundUserCurrency;
	}

	public CompletableFuture<List<UserCurrency>> findAllAsync(
		final int pageNumber,
		final int pageSize
	) {
		return CompletableFuture.supplyAsync(() -> this.findAll(pageNumber, pageSize), this.executor);
	}

	public UserCurrency findByUniqueIdAndCurrency(
		final UUID uniqueId,
		final Currency pCurrency
	) {
		for (
			final UserCurrency usercurrency : this.cache.asMap().values()
		) {
			if (usercurrency.getPlayer().getUniqueId().equals(uniqueId) && usercurrency.getCurrency().equals(pCurrency))
				return usercurrency;
		}

		UserCurrency usercurrency = super.findByAttributes(Map.of("user.uniqueId", uniqueId, "currency", pCurrency));
		if (usercurrency != null)
			this.cache.put(usercurrency.getId(), usercurrency);

		return usercurrency;
	}

	public CompletableFuture<UserCurrency> findByUniqueIdAndCurrencyAsync(
		final UUID uniqueId,
		final Currency pCurrency
	) {
		return CompletableFuture.supplyAsync(() -> this.findByUniqueIdAndCurrency(uniqueId, pCurrency));
	}

	public List<UserCurrency> findAllByCurrency(
		final Currency pCurrency
	) {
		List<UserCurrency> currencies = this.cache.asMap().values().stream().filter(usercurrency -> usercurrency.getCurrency().equals(pCurrency)).toList();
		if (
			! currencies.isEmpty()
		) return currencies;

		List<UserCurrency> pPlayerCurrencies = super.findListByAttributes(Map.of("currency", pCurrency));
		if (! pPlayerCurrencies.isEmpty()) {
			for (UserCurrency usercurrency : pPlayerCurrencies)
				this.cache.put(usercurrency.getId(), usercurrency);
		}

		return pPlayerCurrencies;
	}
}
