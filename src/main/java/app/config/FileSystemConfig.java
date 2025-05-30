package app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

@Configuration
public class FileSystemConfig {

    @Bean
    public FileSystem getFileSystem() {
        return FileSystems.getDefault();
    }
}
