package de.jexcellence.currency.database.repository;

import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;

import java.util.concurrent.ExecutorService;

public class CurrencyRepository extends GenericCachedRepository<Currency, Long, String> {

	public CurrencyRepository(
		final ExecutorService executor,
		final EntityManagerFactory entityManagerFactory
	) {
		super(executor, entityManagerFactory, Currency.class, Currency::getIdentifier);
	}

}
