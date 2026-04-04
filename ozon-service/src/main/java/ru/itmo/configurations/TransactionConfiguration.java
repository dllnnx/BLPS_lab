package ru.itmo.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@Profile("wildfly")
public class TransactionConfiguration {
    @Bean
    public PlatformTransactionManager transactionManager() {
        JtaTransactionManager tm = new JtaTransactionManager();
        tm.setUserTransactionName("java:jboss/UserTransaction");
        tm.setTransactionManagerName("java:jboss/TransactionManager");
        return tm;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
        LocalContainerEntityManagerFactoryBean emf =
                new LocalContainerEntityManagerFactoryBean();

        emf.setDataSource(ds);
        emf.setPackagesToScan("ru.itmo");

        emf.setJpaVendorAdapter(new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter());

        Properties props = new Properties();
        props.put("hibernate.transaction.coordinator_class", "jta");
        props.put("hibernate.transaction.jta.platform",
                "org.hibernate.service.jta.platform.internal.JBossAppServerJtaPlatform");

        emf.setJpaProperties(props);

        return emf;
    }

    @Bean
    public DataSource dataSource() throws Exception {
        return (DataSource) new InitialContext().lookup("java:/OzonDS");
    }
}
