package dao;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class MediaEntityManager {

    private static final String PERSISTENCE_UNIT_NAME = "jscanner-sqlite";
    private static EntityManagerFactory factory;

    public static EntityManagerFactory getEntityManagerFactory(String persistenceUnit) {
        if (factory == null || !factory.isOpen()) {
            factory = Persistence.createEntityManagerFactory(persistenceUnit);
        }

        return factory;
    }

    public static void shutdown() {
        if (factory != null) {
            factory.close();
        }
    }

}
