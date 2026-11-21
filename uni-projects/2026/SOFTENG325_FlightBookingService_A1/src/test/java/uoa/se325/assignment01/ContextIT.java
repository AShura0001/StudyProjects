package uoa.se325.assignment01;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

/**
 * General Spring and DB setup tests
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ContextIT {

    /**
     * Test that the Spring / SpringBoot context loads without crashing.
     * <p>
     * If this test fails, you likely have an error in the constructor of
     * one or more classes which are auto-instantiated by Spring (for example,
     * REST controllers, Components, Configurations, Autowired stuff, etc).
     */
    @Test
    public void testContextLoads() {
    }

    /**
     * Tests that the db-init.sql script can be run successfully.
     * <p>
     * If {@link #testContextLoads()} passes but this test fails, then you
     * likely have an error or omission in some of your JPA annotations which
     * is preventing the Hibernate-generated database schema from matching with
     * the one expected by the SQL statements.
     */
    @Test
    @Sql("classpath:db-init.sql")
    public void testContextLoadsDbInit() {
    }

}
