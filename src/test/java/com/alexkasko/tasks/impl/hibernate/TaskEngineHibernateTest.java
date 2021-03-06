package com.alexkasko.tasks.impl.hibernate;

import com.alexkasko.tasks.TaskEngine;
import com.alexkasko.tasks.TaskProcessorProvider;
import com.alexkasko.tasks.impl.TaskImpl;
import com.alexkasko.tasks.impl.TaskManagerIface;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: alexkasko
 * Date: 5/23/12
 */

//long multithreaded test, may fail like 'Finish all fail expected:<45> but was:<42>'
//because of race conditions in test checks
// Disabled by default
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = TaskEngineHibernateTest.Config.class)
public class TaskEngineHibernateTest {
    @Inject
    private TaskEngine taskEngine;
    @Inject
    private TaskManagerIface taskManager;
    @Inject
    private DataSource ds;

    @Test
    public void dummmy() {
//      I'm dummy
    }

//    @Test
    public void test() throws InterruptedException {
        JdbcTemplate jt = new JdbcTemplate(ds);
        // finish
        for (int i = 0; i < 42; i++) taskManager.add(new TaskImpl(0));
        taskEngine.fire();
        Thread.sleep(600);
        int finished = jt.queryForInt("select count(id) from tasks where stage='FINISHED'");
        assertEquals("Finish fail", 42, finished);
        // suspend
        List<Long> forSuspend = Lists.newArrayList();
        for (int i = 0; i < 3; i++) {
            long id = taskManager.add(new TaskImpl(500));
            forSuspend.add(id);
        }
        taskEngine.fire();
        Thread.sleep(100); // pass engine suspension checker
        for (long id : forSuspend) {
            boolean res = taskEngine.suspend(id);
            assertTrue("Suspend fail", res);
        }
        Thread.sleep(100); // wait for tasks to marked suspended
        int suspended = jt.queryForInt("select count(id) from tasks where status='SUSPENDED'");
        assertEquals("Suspend fail", 3, suspended);
        // resume
        for (long id : forSuspend) taskManager.resume(id);
        taskEngine.fire();
        Thread.sleep(2000);
        int finishedAll = jt.queryForInt("select count(id) from tasks where stage='FINISHED'");
        assertEquals("Finish all fail", 45, finishedAll);
    }

    @Configuration
    @ComponentScan(basePackages = "com.alexkasko.tasks.impl",
            excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, value = NoSpringJdbcImplFilter.class))
    @EnableTransactionManagement
    static class Config {
        @Inject private TaskManagerIface taskManager;
        @Inject private TaskProcessorProvider processorProvider;

        @Bean
        public TaskEngine taskEngine() {
            return new TaskEngine(newCachedThreadPool(), taskManager, processorProvider);
        }

        @Bean
        public DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("org.h2.Driver");
            ds.setUrl("jdbc:h2:mem:TaskEngineHibernateTest;DB_CLOSE_DELAY=-1");
            return ds;
        }

        @Bean
        public AnnotationSessionFactoryBean sessionFactory() {
            AnnotationSessionFactoryBean fac = new AnnotationSessionFactoryBean();
            fac.setDataSource(dataSource());
            fac.setAnnotatedClasses(new Class[]{TaskImpl.class});
            Properties hiber = new Properties();
            String dialect = "org.hibernate.dialect.H2Dialect";
            hiber.setProperty("hibernate.dialect", dialect);
            hiber.setProperty("hibernate.hbm2ddl.auto", "create");
            fac.setHibernateProperties(hiber);
            return fac;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return new HibernateTransactionManager(sessionFactory().getObject());
        }

    }

    private static class NoSpringJdbcImplFilter extends RegexPatternTypeFilter {
        private NoSpringJdbcImplFilter() {
            super(Pattern.compile("com\\.alexkasko\\.tasks\\.impl\\.springjdbc\\..+"));
        }
    }
}
