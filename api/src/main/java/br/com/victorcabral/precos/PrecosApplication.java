package br.com.victorcabral.precos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PrecosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrecosApplication.class, args);
    }
}
