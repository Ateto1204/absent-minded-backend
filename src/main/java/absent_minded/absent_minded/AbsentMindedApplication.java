package absent_minded.absent_minded;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AbsentMindedApplication {

	public static void main(String[] args) {
		System.out.println("DATASOURCE_URL=" + System.getenv("SPRING_DATASOURCE_URL"));
		SpringApplication.run(AbsentMindedApplication.class, args);
	}

}
