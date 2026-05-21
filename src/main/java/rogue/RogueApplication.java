package rogue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RogueApplication – Spring Boot entry point.
 *
 * Replaces the original RogueMain (which launched a Swing window).
 * Now starts an embedded Tomcat/Jetty HTTP server that:
 *   - Serves the HTML frontend at  GET  /
 *   - Exposes REST endpoints used by the browser frontend
 *   - Provides  GET  /actuator/health  for AWS load-balancer health checks
 *
 * The app binds to 0.0.0.0 (all interfaces) on the port defined by the
 * PORT environment variable (default 8080), which is what AWS App Runner,
 * ECS Fargate, and Elastic Beanstalk all expect.
 */
@SpringBootApplication
public class RogueApplication {

    public static void main(String[] args) {

        System.out.println("Starting RogueApplication...");

        SpringApplication.run(RogueApplication.class, args);
    }
}
