package com.gestao.partituras.sistema_partituras;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Cria uma única instância do cliente do Google Drive, reaproveitada por toda a aplicação.
 * As credenciais são lidas de um arquivo FORA do jar (partituras.credentials-path).
 */
@Configuration
public class GoogleDriveService {

    private static final String APPLICATION_NAME = "Sistema de Gestao de Partituras";

    @Bean
    public Drive drive(PartiturasProperties props) throws IOException, GeneralSecurityException {
        Path credenciais = Path.of(props.credentialsPath());
        if (!Files.isReadable(credenciais)) {
            throw new IOException("Credenciais do Google não encontradas em '" + credenciais.toAbsolutePath()
                    + "'. Defina GOOGLE_APPLICATION_CREDENTIALS ou partituras.credentials-path.");
        }

        GoogleCredentials credentials;
        try (InputStream in = Files.newInputStream(credenciais)) {
            // Escopo somente leitura (mais seguro)
            credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(DriveScopes.DRIVE_READONLY));
        }

        return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
