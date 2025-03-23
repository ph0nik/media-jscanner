//package dao.config;
//
//import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.core.env.Environment;
//import org.springframework.orm.hibernate5.HibernateTransactionManager;
//import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
//import org.springframework.transaction.PlatformTransactionManager;
//
//import javax.sql.DataSource;
//import java.util.Properties;
//
////@Configuration
////@EnableTransactionManagement
////@Profile("dev")
//public class HibernateConfigDev {
//
//    @Autowired
//    private Environment env;
//
//    @Bean
//    public LocalSessionFactoryBean sessionFactory() {
//        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
//        sessionFactory.setDataSource(dataSource());
//        sessionFactory.setPackagesToScan("model");
//        sessionFactory.setHibernateProperties(hibernateProperties());
//        return sessionFactory;
//    }
//
//    @Bean
//    public DataSource dataSource() {
//        BasicDataSource dataSource = new BasicDataSource();
//        dataSource.setDriverClassName(env.getProperty("jdbc.driverClassName"));
//        dataSource.setUrl(env.getProperty("jdbc.url"));
//        dataSource.setUsername(env.getProperty("jdbc.username"));
//        dataSource.setPassword(env.getProperty("jdbc.password"));
//        return dataSource;
//    }
//
//    @Bean
//    public PlatformTransactionManager hibernateTransactionManager() {
//        HibernateTransactionManager transactionManager = new HibernateTransactionManager();
//        transactionManager.setSessionFactory(sessionFactory().getObject());
//        return transactionManager;
//    }
//
//    private final Properties hibernateProperties() {
//        Properties hibernateProperties = new Properties();
//        hibernateProperties.setProperty("jakarta.persistence.schema-generation.database.action", "drop-and-create");
//        hibernateProperties.setProperty("jakarta.persistence.schema-generation.create-script-source", "META-INF/sql/create.sql");
//        hibernateProperties.setProperty("dialect", "org.hibernate.dialect.H2Dialect");
////        hibernateProperties.setProperty("javax.persistence.schema-generation.scripts.action", "create");
////        hibernateProperties.setProperty("javax.persistence.schema-generation.scripts.create-target","samplecreate.ddl");
//        hibernateProperties.setProperty("hibernate.show_sql", "true");
//        hibernateProperties.setProperty("format_sql", "true");
//        hibernateProperties.setProperty("hibernate.connection.charSet", "UTF-8");
//        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "none");
//        return hibernateProperties;
//    }
//
//}
