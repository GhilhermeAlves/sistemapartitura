package com.gestao.partituras.sistema_partituras;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class PartituraController {

    @Autowired
    private GoogleDriveService googleDriveService;

    private final String FOLDER_ID = "1BwFnb28EgRYPak9W_N2iXuPjg-wqxrXX";

    @GetMapping("/busca")
    public String paginaBusca(
            @RequestParam(value = "termo", required = false) String termo,
            @RequestParam(value = "pastaId", required = false) String pastaId,
            Model model) {

        List<File> partiturasEncontradas = new ArrayList<>();

        try {
            Drive service = googleDriveService.getDriveService();

            List<PastaItem> arvorePastas = montarArvorePastas(service, FOLDER_ID, 0);
            model.addAttribute("arvorePastas", arvorePastas);

            String buscaPasta = (pastaId != null && !pastaId.isEmpty()) ? pastaId : FOLDER_ID;

            String nomePasta = "Todas as Pastas";
            if (pastaId != null && !pastaId.isEmpty()) {
                nomePasta = buscarNomePasta(service, pastaId);
            }
            model.addAttribute("nomePasta", nomePasta);

            if (termo != null && !termo.trim().isEmpty()) {
                buscarRecursivo(service, FOLDER_ID, termo, partiturasEncontradas);
            } else {
                buscarRecursivo(service, buscaPasta, null, partiturasEncontradas);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("partituras", partiturasEncontradas);
        model.addAttribute("termoBuscado", termo);
        model.addAttribute("pastaSelecionadaId", (pastaId != null && !pastaId.isEmpty()) ? pastaId : FOLDER_ID);
        model.addAttribute("FOLDER_ID", FOLDER_ID);

        return "busca";
    }

    private String buscarNomePasta(Drive service, String pastaId) throws java.io.IOException {
        return service.files().get(pastaId).setFields("name").execute().getName();
    }

    private List<PastaItem> montarArvorePastas(Drive service, String folderId, int nivel) throws java.io.IOException {
        List<PastaItem> pastas = new ArrayList<>();
        String query = "'" + folderId + "' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false";
        String pageToken = null;

        do {
            FileList result = service.files().list()
                    .setQ(query)
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute();

            List<File> files = result.getFiles();
            if (files != null) {
                for (File file : files) {
                    PastaItem item = new PastaItem(
                            file.getId(),
                            file.getName(),
                            montarArvorePastas(service, file.getId(), nivel + 1),
                            nivel
                    );
                    pastas.add(item);
                }
            }

            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        return pastas;
    }

    private void buscarRecursivo(Drive service, String folderId, String termo, List<File> resultado)
            throws java.io.IOException {

        String query = "'" + folderId + "' in parents and trashed = false";
        String pageToken = null;

        do {
            FileList result = service.files().list()
                    .setQ(query)
                    .setFields("nextPageToken, files(id, name, mimeType)")
                    .setPageToken(pageToken)
                    .execute();

            List<File> files = result.getFiles();
            if (files != null) {
                for (File file : files) {
                    if ("application/vnd.google-apps.folder".equals(file.getMimeType())) {
                        buscarRecursivo(service, file.getId(), termo, resultado);
                    } else if (termo == null || termo.trim().isEmpty()
                            || file.getName().toLowerCase().contains(termo.toLowerCase())) {
                        resultado.add(file);
                    }
                }
            }

            pageToken = result.getNextPageToken();
        } while (pageToken != null);
    }

    public static class PastaItem {
        private String id;
        private String nome;
        private List<PastaItem> subpastas;
        private int nivel;

        public PastaItem(String id, String nome, List<PastaItem> subpastas, int nivel) {
            this.id = id;
            this.nome = nome;
            this.subpastas = subpastas;
            this.nivel = nivel;
        }

        public String getId() { return id; }
        public String getNome() { return nome; }
        public List<PastaItem> getSubpastas() { return subpastas; }
        public int getNivel() { return nivel; }
    }
}
