package cz.strazovan.cvut.fel.diploma.messagebox.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
@ComponentScan("cz.strazovan.cvut.fel.diploma.messagebox.dao")
public class PersistenceConfiguration {

    private DatabaseProperties databaseProperties;

    @Autowired
    public PersistenceConfiguration(DatabaseProperties databaseProperties) {
        this.databaseProperties = databaseProperties;
    }

    @Bean(name = "postgreDataSource")
    public DataSource dataSource() {
        final var config = new HikariConfig();
        config.setDriverClassName(this.databaseProperties.getDriverClassName());
        config.setJdbcUrl(this.databaseProperties.getUrl());
        config.setUsername(this.databaseProperties.getUsername());
        config.setPassword(this.databaseProperties.getPassword());
        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("postgreDataSource") DataSource ds) {
        final LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(ds);
        emf.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        emf.setPackagesToScan("cz.strazovan.journalbackend.model");

        final Properties props = new Properties();
        props.setProperty("databasePlatform", this.databaseProperties.getPlatform());
        props.setProperty("generateDdl", "true");
        props.setProperty("showSql", "true");
        props.setProperty("eclipselink.weaving", "static");
        props.setProperty("eclipselink.ddl-generation", this.databaseProperties.getDdlgeneration());
        emf.setJpaProperties(props);
        return emf;
    }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }

}
