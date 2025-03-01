package de.jexcellence.currency.database.repository;

import de.jexcellence.currency.database.entity.User;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

public class UserRepository extends GenericCachedRepository<User, Long, UUID> {

	public UserRepository(
		final ExecutorService executor,
		final EntityManagerFactory entityManagerFactory
	) {
		super(executor, entityManagerFactory, User.class, User::getUniqueId);
	}
}
