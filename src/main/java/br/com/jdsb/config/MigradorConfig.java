package br.com.jdsb.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.com.jdsb.controller.FileController;

@Configuration
public class MigradorConfig {
	
	@Autowired
	private FileController controller;
	
	
	@Bean
	public void start() {
		controller.realizaProcessamento("DEVELOP");
	}

}
