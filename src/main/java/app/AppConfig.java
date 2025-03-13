package app;

import app.controller.CommonHandler;
import app.controller.UserPathValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan({"dao","service", "runner", "util", "websocket", "scanner"})
//@PropertySources({
//        @PropertySource("classpath:mediafolders.properties")
//}) test for reading properties automatically by spring
public class AppConfig implements WebMvcConfigurer {

    private final UserPathValidator userPathValidator;

    @Autowired
    public AppConfig(UserPathValidator userPathValidator) {
        this.userPathValidator = userPathValidator;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userPathValidator)
                .excludePathPatterns(CommonHandler.CONFIG + "/**")
                .addPathPatterns("/**");
    }
}
