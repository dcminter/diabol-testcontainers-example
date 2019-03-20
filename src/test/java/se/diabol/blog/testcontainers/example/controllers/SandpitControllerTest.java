package se.diabol.blog.testcontainers.example.controllers;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import se.diabol.blog.testcontainers.example.services.SandpitService;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(SandpitController.class)
public class SandpitControllerTest {
	@Autowired
	private MockMvc mock;
	
	@MockBean
	private SandpitService ipfsService; 
	
	@Test
	public void expectNamePassedToService() throws Exception {
		this.mock.perform(post("/sandpit/diabol"))
			.andExpect(status().isOk());
		
		verify(ipfsService).addName("diabol");
	}
	
	@Test
	public void expectServiceNamesReflectedInOutput() throws Exception {
		when(ipfsService.listNames()).thenReturn(asList("tom","dick","harry"));
		
		this.mock.perform(get("/sandpit/"))
			.andExpect(status().isOk())
			.andExpect(content().string("Names are [tom, dick, harry]"));
	}
}