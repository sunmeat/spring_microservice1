package com.sunmeat.contactlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringMvcApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringMvcApplication.class, args);
	}
}

@Component
class BrowserLauncher {
	@EventListener(ApplicationReadyEvent.class)
	public void launchBrowser() {
		System.setProperty("java.awt.headless", "false"); 
		var desktop = Desktop.getDesktop();
		try {
			desktop.browse(new URI("http://localhost:8080/profiles"));
		} catch (Exception e) {
			
		}
	}
}
