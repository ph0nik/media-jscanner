package app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class EnvValidator {
    @Value("${tmdb.api.token}")
    private String tmdbApiToken;

    private final ApplicationContext context;

    public EnvValidator(ApplicationContext context) {
        this.context = context;
    }

    /*
    * Validate environment variable API_TOKEN, if not present exit application
    * and notify user in console
    * */
    @PostConstruct
    public void validateEnvironmentVariables() {
        if (tmdbApiToken == null || tmdbApiToken.trim().isEmpty()) {
            System.err.println("Environment variable API_TOKEN is not set");
            System.err.println("Run application with API_TOKEN=<token> parameter");
            SpringApplication.exit(context, () -> 1);
            System.exit(1);
        }
    }

    public String getTmdbApiToken() {
        return tmdbApiToken;
    }


}
