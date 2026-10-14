package com.gestao.partituras.sistema_partituras;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Lê a árvore de pastas e arquivos do Drive e mantém um retrato em memória por
 * {@code partituras.cache-ttl}. Buscas e navegação usam esse retrato, então o
 * Drive é consultado no máximo uma vez a cada intervalo de cache.
 */
@Service
public class PartituraService {

    private static final Logger log = LoggerFactory.getLogger(PartituraService.class);
    private static final String MIME_PASTA = "application/vnd.google-apps.folder";
    private static final Comparator<Partitura> POR_NOME = Comparator.comparing(p -> normalizar(p.nome()));

    private final Drive drive;
    private final PartiturasProperties props;

    private volatile Acervo acervo;

    public PartituraService(Drive drive, PartiturasProperties props) {
        this.drive = drive;
        this.props = props;
    }

    public record Partitura(String id, String nome) {
        public String link() {
            return "https://drive.google.com/file/d/" + id + "/view?usp=sharing";
        }
    }

    public record PastaItem(String id, String nome, List<PastaItem> subpastas, int nivel) {
    }

    /** Retrato completo do acervo: árvore para a sidebar e índices por pasta. */
    private record Acervo(List<PastaItem> arvore,
                          Map<String, String> nomesPastas,
                          Map<String, List<Partitura>> arquivosPorPasta,
                          Map<String, List<String>> filhosPorPasta,
                          Instant carregadoEm) {
    }

    public String pastaRaizId() {
        return props.folderId();
    }

    public List<PastaItem> arvorePastas() {
        return acervo().arvore();
    }

    /** Nome da pasta, apenas se ela pertencer ao acervo. */
    public Optional<String> nomePasta(String pastaId) {
        return Optional.ofNullable(acervo().nomesPastas().get(pastaId));
    }

    /** Todos os arquivos da pasta e de suas subpastas, em ordem alfabética. */
    public List<Partitura> listarPasta(String pastaId) {
        Acervo a = acervo();
        List<Partitura> resultado = new ArrayList<>();
        coletar(a, pastaId, resultado);
        resultado.sort(POR_NOME);
        return resultado;
    }

    /** Busca no acervo inteiro, ignorando maiúsculas e acentos. Todas as palavras precisam aparecer. */
    public List<Partitura> buscar(String termo) {
        String[] palavras = normalizar(termo).split("\\s+");
        return listarPasta(props.folderId()).stream()
                .filter(p -> {
                    String nome = normalizar(p.nome());
                    for (String palavra : palavras) {
                        if (!nome.contains(palavra)) return false;
                    }
                    return true;
                })
                .toList();
    }

    private void coletar(Acervo a, String pastaId, List<Partitura> resultado) {
        resultado.addAll(a.arquivosPorPasta().getOrDefault(pastaId, List.of()));
        for (String filho : a.filhosPorPasta().getOrDefault(pastaId, List.of())) {
            coletar(a, filho, resultado);
        }
    }

    private boolean valido(Acervo a) {
        return a != null && a.carregadoEm().plus(props.cacheTtl()).isAfter(Instant.now());
    }

    private Acervo acervo() {
        Acervo atual = acervo;
        if (valido(atual)) return atual;

        synchronized (this) {
            atual = acervo;
            if (valido(atual)) return atual;
            try {
                acervo = carregar();
                return acervo;
            } catch (IOException e) {
                if (atual != null) {
                    // Drive indisponível: continua servindo o retrato antigo
                    log.warn("Falha ao atualizar acervo do Drive; usando cache anterior", e);
                    return atual;
                }
                throw new AcervoIndisponivelException("Não foi possível acessar o Google Drive", e);
            }
        }
    }

    private Acervo carregar() throws IOException {
        long inicio = System.currentTimeMillis();
        String raiz = props.folderId();

        Map<String, String> nomes = new HashMap<>();
        Map<String, List<Partitura>> arquivos = new HashMap<>();
        Map<String, List<String>> filhos = new HashMap<>();

        nomes.put(raiz, drive.files().get(raiz).setFields("name").execute().getName());
        List<PastaItem> arvore = carregarPasta(raiz, 0, nomes, arquivos, filhos);

        log.info("Acervo carregado: {} pastas, {} arquivos em {} ms", nomes.size(),
                arquivos.values().stream().mapToInt(List::size).sum(), System.currentTimeMillis() - inicio);
        return new Acervo(arvore, Map.copyOf(nomes), Map.copyOf(arquivos), Map.copyOf(filhos), Instant.now());
    }

    /** Lê o conteúdo de uma pasta (uma só listagem para arquivos e subpastas) e desce recursivamente. */
    private List<PastaItem> carregarPasta(String pastaId, int nivel, Map<String, String> nomes,
                                          Map<String, List<Partitura>> arquivos,
                                          Map<String, List<String>> filhos) throws IOException {
        List<File> subpastas = new ArrayList<>();
        List<Partitura> partituras = new ArrayList<>();
        String pageToken = null;

        do {
            // pastaId aqui vem sempre do próprio Drive, nunca do usuário
            FileList result = drive.files().list()
                    .setQ("'" + pastaId + "' in parents and trashed = false")
                    .setFields("nextPageToken, files(id, name, mimeType)")
                    .setPageSize(1000)
                    .setPageToken(pageToken)
                    .execute();

            if (result.getFiles() != null) {
                for (File f : result.getFiles()) {
                    if (MIME_PASTA.equals(f.getMimeType())) {
                        subpastas.add(f);
                    } else {
                        partituras.add(new Partitura(f.getId(), f.getName()));
                    }
                }
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        subpastas.sort(Comparator.comparing(f -> normalizar(f.getName())));
        arquivos.put(pastaId, List.copyOf(partituras));
        filhos.put(pastaId, subpastas.stream().map(File::getId).toList());

        List<PastaItem> itens = new ArrayList<>();
        for (File sub : subpastas) {
            nomes.put(sub.getId(), sub.getName());
            itens.add(new PastaItem(sub.getId(), sub.getName(),
                    carregarPasta(sub.getId(), nivel + 1, nomes, arquivos, filhos), nivel));
        }
        return itens;
    }

    /** Remove acentos e caixa: "Avé María" -> "ave maria". */
    static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    public static class AcervoIndisponivelException extends RuntimeException {
        public AcervoIndisponivelException(String msg, Throwable causa) {
            super(msg, causa);
        }
    }
}
