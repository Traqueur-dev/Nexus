package fr.traqueur.nexus.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NexusCoreApplication {

	/*
	 * Must be public. Java 25 lets the JVM launch a non-public main (JEP 512), so
	 * `gradlew bootRun` and the IDE both work with a package-private one — but
	 * Spring Boot's main-class resolution scans compiled classes for a public
	 * static main, so bootJar fails to find it and `gradlew build` never gets
	 * past assemble.
	 */
	public static void main(String[] args) {
		SpringApplication.run(NexusCoreApplication.class, args);
	}

}
