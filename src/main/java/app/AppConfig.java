package app;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"dao","service", "runner", "util", "websocket", "scanner"})
//@PropertySources({
//        @PropertySource("classpath:mediafolders.properties")
//}) test for reading properties automatically by spring
public class AppConfig {
}
