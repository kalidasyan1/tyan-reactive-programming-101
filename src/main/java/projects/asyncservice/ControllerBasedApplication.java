package projects.asyncservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ControllerBasedApplication {
  private static final Logger log = LoggerFactory.getLogger(ControllerBasedApplication.class);


  public static void main(String[] args) {
    log.info("=== ASYNC SERVICE WITH HANDLE PATTERN ===");
    log.info("Starting async service on port 8081...");
    System.setProperty("server.port", "8081");

    SpringApplication.run(ControllerBasedApplication.class, args);
  }
}
