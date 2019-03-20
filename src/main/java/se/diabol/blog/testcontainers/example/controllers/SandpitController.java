package se.diabol.blog.testcontainers.example.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import se.diabol.blog.testcontainers.example.services.SandpitService;

@RestController
@RequestMapping("/sandpit")
public class SandpitController {
	private static final Logger logger = LoggerFactory.getLogger(SandpitController.class);
	
	private SandpitService sandpitService;
	
	@Autowired
	public SandpitController(final SandpitService ipfsService) {
		this.sandpitService = ipfsService;
	}

	@GetMapping
	public String hello() {
		logger.debug("Calling endpoint /sandpit");
		final var names = sandpitService.listNames();
		return String.format("Names are %s",names);
	}

	@PostMapping("/{name}")
	public void hello(@PathVariable("name") final String name) {
		logger.debug("Calling endpoint /sandpit/{}",name);
		sandpitService.addName(name);
	}
}
