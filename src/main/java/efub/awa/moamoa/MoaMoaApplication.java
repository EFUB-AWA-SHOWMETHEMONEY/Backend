package efub.awa.moamoa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MoaMoaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoaMoaApplication.class, args);
    }

}
