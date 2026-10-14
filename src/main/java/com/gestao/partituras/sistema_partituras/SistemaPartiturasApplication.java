package com.gestao.partituras.sistema_partituras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SistemaPartiturasApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaPartiturasApplication.class, args);
	}

}
