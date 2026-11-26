package uniamerica.abarbeirados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class AbarbeiradosApplication {

	public static void main(String[] args) {
		SpringApplication.run(AbarbeiradosApplication.class, args);
	}

}
