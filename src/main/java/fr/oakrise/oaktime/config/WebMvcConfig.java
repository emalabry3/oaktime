package fr.oakrise.oaktime.config;

import org.h2.server.web.JakartaWebServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration Spring MVC de l'application.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Enregistre la servlet H2 directement dans le conteneur Tomcat,
     * en dehors du DispatcherServlet Spring MVC.
     *
     * Cela évite que Spring MVC intercepte les requêtes POST de la console H2
     * (NoResourceFoundException sur /h2-console/login.do).
     *
     * La console reste accessible sur : http://localhost:8080/h2-console/
     */
    @Bean
    public ServletRegistrationBean<?> h2ConsoleServlet() {
        JakartaWebServlet servlet = new JakartaWebServlet();
        ServletRegistrationBean<?> bean = new ServletRegistrationBean<>(servlet, "/h2-console/*");
        bean.addInitParameter("webAllowOthers", "false");
        bean.setLoadOnStartup(1);
        return bean;
    }
}