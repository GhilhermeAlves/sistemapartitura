package com.gestao.partituras.sistema_partituras;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class PartituraController {

    private static final Logger log = LoggerFactory.getLogger(PartituraController.class);

    private final PartituraService partituraService;

    public PartituraController(PartituraService partituraService) {
        this.partituraService = partituraService;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/busca";
    }


    @GetMapping("/busca")
    public String paginaBusca(
            @RequestParam(value = "termo", required = false) String termo,
            @RequestParam(value = "pastaId", required = false) String pastaId,
            Model model) {

        String raiz = partituraService.pastaRaizId();
        String termoLimpo = (termo == null || termo.isBlank()) ? null : termo.trim();
        String pastaSelecionada = raiz;
        String nomePasta = "Todas as Pastas";
        List<PartituraService.Partitura> partituras = List.of();
        List<PartituraService.PastaItem> arvore = List.of();
        String erro = null;

        try {
            arvore = partituraService.arvorePastas();

            // Só aceita pastas que pertencem ao acervo
            if (pastaId != null && !pastaId.isBlank() && !pastaId.equals(raiz)) {
                Optional<String> nome = partituraService.nomePasta(pastaId);
                if (nome.isPresent()) {
                    pastaSelecionada = pastaId;
                    nomePasta = nome.get();
                } else {
                    erro = "Pasta não encontrada no acervo.";
                }
            }

            partituras = (termoLimpo != null)
                    ? partituraService.buscar(termoLimpo)
                    : partituraService.listarPasta(pastaSelecionada);

        } catch (PartituraService.AcervoIndisponivelException e) {
            log.error("Erro ao carregar acervo", e);
            erro = "Não foi possível acessar o Google Drive agora. Tente novamente em instantes.";
        }

        model.addAttribute("arvorePastas", arvore);
        model.addAttribute("nomePasta", nomePasta);
        model.addAttribute("partituras", partituras);
        model.addAttribute("termoBuscado", termoLimpo);
        model.addAttribute("pastaSelecionadaId", pastaSelecionada);
        model.addAttribute("erro", erro);
        model.addAttribute("raizId", raiz);

        return "busca";
    }
}
