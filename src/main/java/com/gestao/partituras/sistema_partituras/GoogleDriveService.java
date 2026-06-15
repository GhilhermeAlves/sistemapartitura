package com.gestao.partituras.sistema_partituras;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleDriveService {

    private static final String APPLICATION_NAME = "Sistema de Gestao de Partituras";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    
    // Esse escopo permite APENAS ler os arquivos (mais seguro)
    private static final java.util.Collection<String> SCOPES = 
        Collections.singleton(DriveScopes.DRIVE_READONLY);

    /**
     * Método responsável por autenticar e retornar a instância do serviço do Google Drive
     */
    public Drive getDriveService() throws IOException, GeneralSecurityException {
        // 1. Carrega o arquivo JSON que você colocou em src/main/resources
        InputStream in = GoogleDriveService.class.getResourceAsStream("/google-credentials.json");
        
        if (in == null) {
            throw new IOException("Arquivo google-credentials.json não foi encontrado em src/main/resources!");
        }

        // 2. Cria as credenciais a partir do arquivo
        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                .createScoped(SCOPES);

        // 3. Inicializa o transporte HTTP (comunicação com a web)
        final HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // 4. Retorna o objeto Drive configurado e pronto para uso
        return new Drive.Builder(httpTransport, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}