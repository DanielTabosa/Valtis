package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.comum.ConflitoException;
import br.com.manutec.valtis.comum.ModoDeOperacao;
import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.servico.Servico;
import br.com.manutec.valtis.servico.ServicoRepository;
import br.com.manutec.valtis.usuario.UsuarioService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** As telas de admin do modo integrado: pendências (RF-75) e estado (RF-78). */
@Service
public class IntegracaoService {

    public record ServicoDaPendencia(UUID id, String codigoEquipamento, LocalDate dataRealizada, boolean ativo,
                                     UUID valvulaId, UUID bombaId) {}

    /** Uma pendência como a tela mostra: o motivo e um resumo legível do item (sem o corpo inteiro). */
    public record Pendencia(UUID id, OrigemIntegracao origem, TipoEventoRecebido tipo, long idExterno, String versao,
                            EstadoEventoRecebido estado, String motivo, String ultimoErro, int tentativas,
                            OffsetDateTime recebidoEm, String resumo, List<ServicoDaPendencia> servicos) {}

    public record Cursor(String origem, String recurso, String desde, OffsetDateTime ultimaLeituraEm,
                         OffsetDateTime ultimaLeituraOkEm, String ultimoErro) {}

    public record Estado(String modoCadastros, boolean leituraAtiva, List<Cursor> cursores,
                         Map<EstadoEventoRecebido, Long> eventos) {}

    private final EventoRecebidoRepository eventos;
    private final CursorIntegracaoRepository cursores;
    private final ServicoRepository servicos;
    private final UsuarioService usuarios;
    private final ModoDeOperacao modo;
    private final ObjectProvider<LeitorDaIntegracao> leitor;
    private final ObjectMapper json;
    private final Clock relogio;

    public IntegracaoService(EventoRecebidoRepository eventos, CursorIntegracaoRepository cursores,
                             ServicoRepository servicos, UsuarioService usuarios, ModoDeOperacao modo,
                             ObjectProvider<LeitorDaIntegracao> leitor, ObjectMapper json, Clock relogio) {
        this.eventos = eventos;
        this.cursores = cursores;
        this.servicos = servicos;
        this.usuarios = usuarios;
        this.modo = modo;
        this.leitor = leitor;
        this.json = json;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public List<Pendencia> pendencias() {
        return eventos.comEstado(EnumSet.of(EstadoEventoRecebido.PENDENCIA, EstadoEventoRecebido.FALHOU)).stream()
                .map(this::pendencia)
                .toList();
    }

    @Transactional
    public void reprocessar(UUID id, UUID usuarioId) {
        buscar(id).reprocessar(usuarios.buscarAtivo(usuarioId), OffsetDateTime.now(relogio));
    }

    @Transactional
    public void descartar(UUID id, String justificativa, UUID usuarioId) {
        buscar(id).descartar(usuarios.buscarAtivo(usuarioId), Textos.limpar(justificativa), OffsetDateTime.now(relogio));
    }

    @Transactional(readOnly = true)
    public Estado estado() {
        Map<CursorIntegracao.Chave, CursorIntegracao> lidos = cursores.findAll().stream()
                .collect(Collectors.toMap(CursorIntegracao::getChave, Function.identity()));
        List<Cursor> lista = Arrays.stream(RecursoLido.values())
                .map(r -> {
                    CursorIntegracao c = lidos.get(r.chave());
                    return c == null
                            ? new Cursor(r.origem().valor(), r.nome(), null, null, null, null)
                            : new Cursor(r.origem().valor(), r.nome(), c.getDesde(), c.getUltimaLeituraEm(),
                                    c.getUltimaLeituraOkEm(), c.getUltimoErro());
                })
                .toList();
        Map<EstadoEventoRecebido, Long> contagem = Arrays.stream(EstadoEventoRecebido.values())
                .collect(Collectors.toMap(Function.identity(), eventos::countByEstado));
        return new Estado(modo.isExterna() ? "externa" : "propria", leitor.getIfAvailable() != null, lista, contagem);
    }

    /** RF-78 "ler agora". Fora de transação: a rodada abre as dela. */
    public LeitorDaIntegracao.ResumoDaRodada lerAgora() {
        LeitorDaIntegracao disponivel = leitor.getIfAvailable();
        if (disponivel == null) {
            throw new ConflitoException("A leitura só existe no modo integrado (origem dos cadastros = externa).");
        }
        return disponivel.rodar();
    }

    private EventoRecebido buscar(UUID id) {
        return eventos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado."));
    }

    private Pendencia pendencia(EventoRecebido e) {
        List<ServicoDaPendencia> daOs = e.getTipo() == TipoEventoRecebido.ORDEM_SERVICO
                ? servicos.findByJirayaOsId(e.getIdExterno()).stream().map(IntegracaoService::servico).toList()
                : List.of();
        return new Pendencia(e.getId(), e.getOrigem(), e.getTipo(), e.getIdExterno(), e.getVersao(), e.getEstado(),
                e.getMotivo(), e.getUltimoErro(), e.getTentativas(), e.getRecebidoEm(), resumo(e), daOs);
    }

    private static ServicoDaPendencia servico(Servico s) {
        String codigo = s.isDaBomba() ? s.getBomba().getCodigoReferencia() : s.getValvula().getCodigoReferencia();
        return new ServicoDaPendencia(s.getId(), codigo, s.getDataRealizada(), s.isAtivo(),
                s.getValvula() == null ? null : s.getValvula().getId(), s.isDaBomba() ? s.getBomba().getId() : null);
    }

    /** "Bomba de recalque BR-01 · Cond. X", "OS 1234 · Cond. X · concluida", "Cond. X". Sem contato (LGPD). */
    private String resumo(EventoRecebido e) {
        JsonNode corpo = json.readTree(e.getCorpo());
        String cliente = corpo.path("cliente").path("nome").asString("");
        return switch (e.getTipo()) {
            case CLIENTE -> corpo.path("nome_fantasia").asString(corpo.path("nome").asString(""));
            case EQUIPAMENTO -> String.join(" · ", List.of(
                    (corpo.path("tipo").path("nome").asString("") + " " + corpo.path("identificacao").asString("")).strip(),
                    cliente)).strip();
            case ORDEM_SERVICO -> "OS %d · %s · %s".formatted(e.getIdExterno(), cliente,
                    corpo.path("situacao").asString(""));
        };
    }
}
