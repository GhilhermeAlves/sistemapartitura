package com.gestao.partituras.sistema_partituras;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestDriveController {

    @Autowired
    private GoogleDriveService googleDriveService;

    @GetMapping("/testar-drive")
    public String testarConexao() {
        try {
            // 1. Pega a instância do Drive que configuramos
            Drive service = googleDriveService.getDriveService();

            // 2. PREENCHA AQUI: Cole o ID da sua pasta do Google Drive entre as aspas
            String folderId = "1BwFnb28EgRYPak9W_N2iXuPjg-wqxrXX";

            // 3. Monta a consulta para buscar arquivos dentro daquela pasta específica
            String query = "'" + folderId + "' in parents and trashed = false";

            // 4. Executa a busca trazendo o ID e o Nome dos arquivos
            FileList result = service.files().list()
                    .setQ(query)
                    .setFields("nextPageToken, files(id, name)")
                    .execute();

            List<File> files = result.getFiles();

            if (files == null || files.isEmpty()) {
                return "Conexão funcionou, mas nenhum arquivo foi encontrado na pasta.";
            }

            // 5. Mostra os arquivos no console do VS Code
            System.out.println("======= PARTITURAS ENCONTRADAS =======");
            for (File file : files) {
                System.out.printf("Nome: %s | ID: %s\n", file.getName(), file.getId());
            }
            System.out.println("======================================");

            return "Sucesso! Foram encontrados " + files.size() + " arquivos. Olhe o console do VS Code.";

        } catch (Exception e) {
            e.printStackTrace();
            return "Erro ao conectar ao Google Drive: " + e.getMessage();
        }
    }
}