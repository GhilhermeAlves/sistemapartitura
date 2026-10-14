package com.gestao.partituras.sistema_partituras;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PartituraController.class)
class SistemaPartiturasApplicationTests {

	private static final String RAIZ = "raiz";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private PartituraService service;

	@Test
	void listaPastaRaizPorPadrao() throws Exception {
		when(service.pastaRaizId()).thenReturn(RAIZ);
		when(service.arvorePastas()).thenReturn(List.of());
		when(service.listarPasta(RAIZ)).thenReturn(List.of(new PartituraService.Partitura("abc", "Ave Maria.pdf")));

		mvc.perform(get("/busca"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Ave Maria.pdf")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("https://drive.google.com/file/d/abc/view")));
	}

	@Test
	void rejeitaPastaForaDoAcervo() throws Exception {
		when(service.pastaRaizId()).thenReturn(RAIZ);
		when(service.arvorePastas()).thenReturn(List.of());
		when(service.nomePasta("x' in parents or '")).thenReturn(Optional.empty());

		mvc.perform(get("/busca").param("pastaId", "x' in parents or '"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("pastaSelecionadaId", RAIZ))
				.andExpect(model().attribute("erro", "Pasta não encontrada no acervo."));

		verify(service, never()).listarPasta("x' in parents or '");
	}

	@Test
	void mostraErroQuandoDriveIndisponivel() throws Exception {
		when(service.pastaRaizId()).thenReturn(RAIZ);
		when(service.arvorePastas()).thenThrow(new PartituraService.AcervoIndisponivelException("falha", null));

		mvc.perform(get("/busca").param("termo", "ave"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Não foi possível acessar o Google Drive")));

		verify(service, never()).buscar(anyString());
	}

	@Test

	void normalizarIgnoraAcentosECaixa() {
		assertThat(PartituraService.normalizar("  Avé MARÍA Ação ")).isEqualTo("ave maria acao");
	}
}
