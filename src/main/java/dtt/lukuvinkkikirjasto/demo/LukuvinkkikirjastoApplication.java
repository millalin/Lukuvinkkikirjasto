package dtt.lukuvinkkikirjasto.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan
public class LukuvinkkikirjastoApplication {

	public static void main(String[] args) {
		SpringApplication.run(LukuvinkkikirjastoApplication.class, args);
	}
}