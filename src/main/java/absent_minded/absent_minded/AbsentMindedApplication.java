package absent_minded.absent_minded;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AbsentMindedApplication {

    public static void main(String[] args) {
        System.out.println("=== ENV CHECK ===");
        System.out.println("URL=" + System.getenv("SPRING_DATASOURCE_URL"));
        System.out.println("USER=" + System.getenv("SPRING_DATASOURCE_USERNAME"));
        System.out.println("PWD=" + System.getenv("SPRING_DATASOURCE_PASSWORD"));
		SpringApplication.run(AbsentMindedApplication.class, args);
	}
}
