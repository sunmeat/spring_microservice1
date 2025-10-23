package com.sunmeat.contactlist;

import java.awt.Desktop;
import java.net.URI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class Service1Application {
	public static void main(String[] args) {
		SpringApplication.run(Service1Application.class, args);
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