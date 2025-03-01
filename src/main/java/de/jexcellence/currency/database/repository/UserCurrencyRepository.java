package de.jexcellence.currency.database.repository;

import de.jexcellence.currency.database.entity.Currency;
import de.jexcellence.currency.database.entity.UserCurrency;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class UserCurrencyRepository extends GenericCachedRepository<UserCurrency, Long, UUID> {

	public UserCurrencyRepository(
		final ExecutorService executor,
		final EntityManagerFactory entityManagerFactory
	) {
		super(executor, entityManagerFactory, UserCurrency.class, user -> user.getPlayer().getUniqueId());
	}

	public List<UserCurrency> findTopByCurrency(final Currency currency, final int limit) {
		List<UserCurrency> foundUserCurrencies = new ArrayList<>(this.findListByAttributes(Map.of("currency", currency)));

		return foundUserCurrencies.size() > limit ? foundUserCurrencies.subList(0, limit) : foundUserCurrencies;
	}
}
