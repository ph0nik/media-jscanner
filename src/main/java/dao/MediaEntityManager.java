package dao;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class MediaEntityManager {

    private static final String PERSISTENCE_UNIT_NAME = "jscanner-sqlite";
    private static EntityManagerFactory factory;

    // TODO do I close EMF every time?
    public static EntityManagerFactory getEntityManagerFactory() {
        if (factory == null) {
            factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        }
        return factory;
    }

    public static void shutdown() {
        if (factory != null) {
            factory.close();
        }
    }

}
