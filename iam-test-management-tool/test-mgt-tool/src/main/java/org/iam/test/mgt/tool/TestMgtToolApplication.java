package org.iam.test.mgt.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ApplicationContext;
/**
 * This is the start-class.
 */
@SpringBootApplication
public class TestMgtToolApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestMgtToolApplication.class);
	public static void main(String[] args) {

		LOGGER.info("Starting Server!");
		SpringApplication app = new SpringApplication(TestMgtToolApplication.class);
		app.addListeners(new ApplicationPidFileWriter());
		ApplicationContext ctx = app.run(args);
	}
}
