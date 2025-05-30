package app.config;

import app.controller.CommonHandler;
import app.controller.UserPathValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableJpaRepositories("dao")
@EntityScan("model")
@ComponentScan({"dao","service", "runner", "util", "websocket", "scanner"})
public class AppConfig implements WebMvcConfigurer {

    private final UserPathValidator userPathValidator;

    @Autowired
    public AppConfig(UserPathValidator userPathValidator) {
        this.userPathValidator = userPathValidator;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userPathValidator)
                .excludePathPatterns(
                        CommonHandler.CONFIG + "/**",
                        "/css/**",
                        "/js/**",
                        "/webjars/**",
                        "/images/**",
                        "/webfonts/**",
                        "/style.css",
                        "/favicon.ico",
                        "/error"
                )
                .addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/webjars/**")
                .addResourceLocations("/webjars/");
    }

}
