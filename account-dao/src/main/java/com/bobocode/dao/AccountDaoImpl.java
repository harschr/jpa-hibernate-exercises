package com.bobocode.dao;

import com.bobocode.exception.AccountDaoException;
import com.bobocode.model.Account;

import javax.persistence.EntityManagerFactory;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.persistence.EntityManager;

public class AccountDaoImpl implements AccountDao {

    private static EntityManagerFactory emf;

    public AccountDaoImpl(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public void save(Account account) {
        performWithinPersistenceContext(em -> em.persist(account));
    }

    @Override
    public Account findById(Long id) {
        return performReturningWithinPersistenceContext(em
                -> em.createQuery("select a from Account a where a.id = :id", Account.class)
                        .setParameter("id", id)
                        .getSingleResult()
        );
    }

    @Override
    public Account findByEmail(String email) {
        return performReturningWithinPersistenceContext(em
                -> em.createQuery("select a from Account a where a.email = :email", Account.class)
                        .setParameter("email", email)
                        .getSingleResult()
        );
    }

    @Override
    public List<Account> findAll() {
        return performReturningWithinPersistenceContext(em
                -> em.createQuery("select a from Account a", Account.class).getResultList()
        );
    }

    @Override
    public void update(Account account) {
        performWithinPersistenceContext(em -> em.merge(account));
    }

    @Override
    public void remove(Account account) {
        performWithinPersistenceContext(em -> {
            Account managedAccount = em.merge(account); // only managed entities can be removed
            em.remove(managedAccount);
        });
    }

    public static void performWithinPersistenceContext(Consumer<EntityManager> operation) {
        EntityManager entityManager = emf.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            // Consumer (=functional interface, egyetlen functional metódusa van, az accept) object - a muvelet, amit végre akarunk hajtani
            //accept metódus paramétere - amin a muveletet végre akarjuk hajtani
            operation.accept(entityManager);
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new AccountDaoException("Error performing JPA operation. Transaction is rolled back", e);
        } finally {
            entityManager.close();
        }
    }

    public static <T> T performReturningWithinPersistenceContext(Function<EntityManager, T> entityManagerFunction) {
        EntityManager entityManager = emf.createEntityManager();
        entityManager.getTransaction().begin();
        try {
            T result = entityManagerFunction.apply(entityManager);
            entityManager.getTransaction().commit();
            return result;
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
            throw new AccountDaoException("Error performing JPA operation. Transaction is rolled back", e);
        } finally {
            entityManager.close();
        }
    }
}
