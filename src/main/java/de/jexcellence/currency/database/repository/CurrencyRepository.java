package de.jexcellence.currency.database.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.hibernate.repository.AbstractCRUDRepository;
import jakarta.persistence.EntityManagerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class CurrencyRepository extends AbstractCRUDRepository<Currency, Long> {

	private final ExecutorService executor;
	private final Cache<Long, Currency> cache;

	public CurrencyRepository(
		final ExecutorService executor,
		final EntityManagerFactory entityManagerFactory
	) {
		super(entityManagerFactory, Currency.class);
		this.cache = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
		this.executor = executor;
	}

}
