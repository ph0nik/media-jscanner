package app;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"dao","service", "runner", "util", "websocket"})
public class AppConfig {
}
