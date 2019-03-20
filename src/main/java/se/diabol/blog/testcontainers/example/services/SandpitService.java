package se.diabol.blog.testcontainers.example.services;

import java.util.List;

public interface SandpitService {
	public void addName(String message);
	public List<String> listNames();
}
