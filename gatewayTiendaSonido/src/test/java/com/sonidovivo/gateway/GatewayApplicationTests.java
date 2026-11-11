package com.sonidovivo.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class GatewayApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    /**
     * El Gateway no debe tener una base de datos propia. La ausencia del bean
     * dataSource confirma que no se activo ninguna autoconfiguracion JDBC/JPA.
     */
    @Test
    void noTieneBeanDeBaseDeDatos() {
        assertThat(applicationContext.containsBean("dataSource")).isFalse();
        assertThat(applicationContext.containsBean("entityManagerFactory")).isFalse();
    }
}
