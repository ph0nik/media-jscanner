package dao;

//import javax.persistence.EntityManagerFactory;
//import javax.persistence.Persistence;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class MediaEntityManager {

    private static final String PERSISTENCE_UNIT_NAME = "jscanner-sqlite";
    private static EntityManagerFactory factory;
    private static String persistenceName;

    public static EntityManagerFactory getEntityManagerFactory() {
        setPersistenceConfig();
        if (factory == null || !factory.isOpen()) {
            factory = Persistence.createEntityManagerFactory(persistenceName);
        }

        return factory;
    }

    /*
    * Load persistence profile based on spring profile being used
    * */
    public static void setPersistenceConfig() {
        String property = System.getProperty("spring.profiles.active");
        persistenceName = (property.equals("dev")) ? "jscanner-sqlite" : "jscanner-prod";
        System.out.println("persistence unit name: " + persistenceName);
    }

    public static void shutdown() {
        if (factory != null) {
            factory.close();
        }
    }

}
