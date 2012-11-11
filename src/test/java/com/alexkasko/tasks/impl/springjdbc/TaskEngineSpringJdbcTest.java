package com.alexkasko.tasks.impl.springjdbc;

import com.alexkasko.tasks.TaskEngine;
import com.alexkasko.tasks.TaskProcessorProvider;
import com.alexkasko.tasks.impl.TaskImpl;
import com.alexkasko.tasks.impl.TaskManagerIface;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 * User: alexkasko
 * Date: 5/23/12
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TaskEngineSpringJdbcTest.Config.class)
public class TaskEngineSpringJdbcTest {
    @Inject
    private TaskEngine taskEngine;
    @Inject
    private TaskManagerIface taskManager;
    @Inject
    private DataSource ds;

    @Test
    public void test() throws InterruptedException {
        JdbcTemplate jt = new JdbcTemplate(ds);
        // finish
        for(int i=0; i<42; i++) taskManager.add(new TaskImpl(0));
        taskEngine.fire();
        int finished = jt.queryForInt("select count(id) from tasks where stage='FINISHED'");
        assertEquals("Finish fail", 42, finished);
    }

    @Configuration
    @ComponentScan(basePackages = "com.alexkasko.tasks.impl",
            excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, value = NoHibernateImplFilter.class))
    @EnableTransactionManagement
    static class Config {
        @Inject private TaskManagerIface taskManager;
        @Inject private TaskProcessorProvider processorProvider;

        @Bean(initMethod = "init")
        public TaskEngine taskEngine() {
            return new TaskEngine(MoreExecutors.sameThreadExecutor(), taskManager, processorProvider);
        }

        @Bean
        public DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("org.h2.Driver");
            ds.setUrl("jdbc:h2:mem:TaskEngineSpringJdbcTest;DB_CLOSE_DELAY=-1");
            return ds;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            DataSource ds = dataSource();
            JdbcTemplate jt = new JdbcTemplate(ds);
            jt.execute("create table tasks(id bigint primary key, stage varchar(255), status varchar(255), payload bigint)");
            jt.execute("create sequence tasks_id_seq");
            return new DataSourceTransactionManager(ds);
        }

    }

    private static class NoHibernateImplFilter extends RegexPatternTypeFilter {
        private NoHibernateImplFilter() {
            super(Pattern.compile("com\\.alexkasko\\.tasks\\.impl\\.hibernate\\..+"));
        }
    }
}
