package br.com.manutec.valtis.oportunidade;

import br.com.manutec.valtis.agenda.FilaDaAgenda;
import br.com.manutec.valtis.bomba.Bomba;
import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.recorrencia.RegrasRecorrencia;
import br.com.manutec.valtis.servico.Servico;
import br.com.manutec.valtis.usuario.Usuario;
import br.com.manutec.valtis.usuario.UsuarioService;
import br.com.manutec.valtis.valvula.Valvula;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ciclo de vida das oportunidades (módulos M5 e M10).
 *
 * Toda mudança que afeta a agenda passa pela FilaDaAgenda, na mesma transação.
 * Nenhum método aqui chama o Google (D-13).
 */
@Service
public class OportunidadeService {

    public enum Situacao { ABERTAS, ENCERRADAS, TODAS }

    public enum Responsavel { EU, NINGUEM, QUALQUER }

    public record ResultadoDesfecho(OportunidadeResposta oportunidade, OportunidadeResposta retorno) {}

    /** RN-01: sem válvula cadastrada, vale o padrão de 12 meses. */
    private static final int PERIODICIDADE_PADRAO_MESES = 12;

    private final OportunidadeRepository repository;
    private final FilaDaAgenda filaDaAgenda;
    private final RegrasRecorrencia regras;
    private final UsuarioService usuarios;
    private final Clock relogio;

    public OportunidadeService(OportunidadeRepository repository, FilaDaAgenda filaDaAgenda,
                               RegrasRecorrencia regras, UsuarioService usuarios, Clock relogio) {
        this.repository = repository;
        this.filaDaAgenda = filaDaAgenda;
        this.regras = regras;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    /**
     * Chamado pelo lançamento de serviço, dentro da transação dele. Vale para válvula
     * e para bomba (D-31, RF-80).
     * RN-10: fecha o ciclo aberto do equipamento. RN-07: abre o próximo, sem responsável.
     * RF-59: o primeiro serviço real de um cliente importado fecha a oportunidade de escopo cliente.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Oportunidade abrirNovoCiclo(Servico servico) {
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        Cliente cliente = servico.getCliente();

        abertasDoEquipamento(servico).forEach(o -> fechar(o, servico, agora));
        repository.abertasDeEscopoCliente(cliente.getId(), StatusOportunidade.ABERTOS)
                .forEach(o -> fechar(o, servico, agora));
        // O Hibernate grava INSERTs antes de UPDATEs. Sem este flush, a nova
        // oportunidade chegaria ao banco antes de a antiga ser fechada, e o
        // índice "um ciclo aberto por equipamento" recusaria a gravação.
        repository.flush();

        LocalDate prevista = regras.proximaManutencao(servico.getDataRealizada(), servico.getPeriodicidadeMeses());
        Oportunidade nova = repository.save(Oportunidade.doServico(servico, prevista));
        filaDaAgenda.enfileirarAlertas(nova);
        return nova;
    }

    /**
     * RF-54: o cliente importado entra na fila com a periodicidade padrão (RN-01), já
     * vencido quando for o caso. Alertas no passado não vão para a agenda (RN-13).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Oportunidade abrirCicloDeImportado(Cliente cliente, LocalDate ultimaManutencao) {
        LocalDate prevista = regras.proximaManutencao(ultimaManutencao, PERIODICIDADE_PADRAO_MESES);
        Oportunidade nova = repository.save(Oportunidade.daImportacao(cliente, prevista));
        filaDaAgenda.enfileirarAlertas(nova);
        return nova;
    }

    /** A periodicidade da válvula mudou (RN-02): o ciclo aberto acompanha a nova data. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reprogramarCicloDaValvula(Valvula valvula, LocalDate ultimaManutencao) {
        LocalDate prevista = regras.proximaManutencao(ultimaManutencao, valvula.getPeriodicidadeMeses());
        for (Oportunidade aberta : repository.abertasDaValvula(valvula.getId(), StatusOportunidade.ABERTOS)) {
            if (!aberta.getDataPrevista().equals(prevista) && aberta.getOportunidadeAnterior() == null) {
                aberta.reprogramar(prevista);
                filaDaAgenda.reprogramar(aberta);
            }
        }
    }

    /** A periodicidade da bomba mudou (P-22): o ciclo aberto acompanha, como na válvula. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reprogramarCicloDaBomba(Bomba bomba, LocalDate ultimaManutencao) {
        LocalDate prevista = regras.proximaManutencao(ultimaManutencao, bomba.getPeriodicidadeMeses());
        for (Oportunidade aberta : repository.abertasDaBomba(bomba.getId(), StatusOportunidade.ABERTOS)) {
            if (!aberta.getDataPrevista().equals(prevista) && aberta.getOportunidadeAnterior() == null) {
                aberta.reprogramar(prevista);
                filaDaAgenda.reprogramar(aberta);
            }
        }
    }

    /**
     * RN-26: o serviço que abriu o ciclo atual foi inativado. O ciclo volta a contar
     * do último serviço ATIVO do equipamento. O retorno pedido numa recusa (RN-09)
     * fica como está: a data foi escolhida por uma pessoa, como em S-07.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recontarCicloSemServico(Servico inativado, LocalDate ultimoServicoAtivo) {
        LocalDate prevista = regras.proximaManutencao(ultimoServicoAtivo, inativado.getPeriodicidadeMeses());
        for (Oportunidade aberta : abertasDoEquipamento(inativado)) {
            boolean nasceuDoInativado = aberta.getServicoOrigem() != null
                    && aberta.getServicoOrigem().getId().equals(inativado.getId());
            if (nasceuDoInativado && !aberta.getDataPrevista().equals(prevista)) {
                aberta.reprogramar(prevista);
                filaDaAgenda.reprogramar(aberta);
            }
        }
    }

    /** O ciclo aberto do equipamento nasceu deste serviço? */
    @Transactional(readOnly = true)
    public boolean cicloAbertoNasceuDe(Servico servico) {
        return abertasDoEquipamento(servico).stream()
                .anyMatch(o -> o.getServicoOrigem() != null && o.getServicoOrigem().getId().equals(servico.getId()));
    }

    private List<Oportunidade> abertasDoEquipamento(Servico servico) {
        return servico.isDaBomba()
                ? repository.abertasDaBomba(servico.getBomba().getId(), StatusOportunidade.ABERTOS)
                : repository.abertasDaValvula(servico.getValvula().getId(), StatusOportunidade.ABERTOS);
    }

    /** A válvula já entrou em algum ciclo? Se sim, tem histórico e não pode ser excluída. */
    @Transactional(readOnly = true)
    public boolean existeParaValvula(UUID valvulaId) {
        return repository.existsByValvulaId(valvulaId);
    }

    @Transactional(readOnly = true)
    public boolean existeParaBomba(UUID bombaId) {
        return repository.existsByBombaId(bombaId);
    }

    @Transactional(readOnly = true)
    public List<OportunidadeResposta> fila(Situacao situacao, Responsavel responsavel, UUID usuarioId) {
        LocalDate hoje = LocalDate.now(relogio);
        Set<UUID> comLembrete = filaDaAgenda.oportunidadesComLembrete();
        return repository.fila(statusDa(situacao))
                .stream()
                // RN-12: válvula ou cliente inativo não aparece para contato.
                .filter(Oportunidade::isDeEquipamentoOperante)
                .filter(o -> aceitaResponsavel(o, responsavel, usuarioId))
                .map(o -> resposta(o, hoje, comLembrete.contains(o.getId())))
                .toList();
    }

    @Transactional
    public OportunidadeResposta assumir(UUID id, UUID usuarioId) {
        Usuario usuario = usuarios.buscarAtivo(usuarioId);
        Oportunidade oportunidade = buscar(id);
        oportunidade.assumir(usuario, OffsetDateTime.now(relogio));
        return resposta(oportunidade, LocalDate.now(relogio));
    }

    @Transactional
    public OportunidadeResposta liberar(UUID id, UUID usuarioId) {
        Usuario usuario = usuarios.buscarAtivo(usuarioId);
        Oportunidade oportunidade = buscar(id);
        oportunidade.liberar(usuario);
        return resposta(oportunidade, LocalDate.now(relogio));
    }

    /**
     * RF-41, RF-44, RN-09.
     * Quem registra o desfecho de uma oportunidade sem dono passa a ser o dono dela.
     */
    @Transactional
    public ResultadoDesfecho registrarDesfecho(UUID id, DesfechoRequisicao pedido, UUID usuarioId) {
        Usuario usuario = usuarios.buscarAtivo(usuarioId);
        Oportunidade oportunidade = buscar(id);
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        LocalDate hoje = LocalDate.now(relogio);

        validarRecusa(pedido, hoje);

        if (oportunidade.getResponsavel() == null && oportunidade.isAberta()) {
            oportunidade.assumir(usuario, agora);
        }
        oportunidade.registrarDesfecho(pedido.status(), Textos.limpar(pedido.notas()),
                Textos.limpar(pedido.motivoRecusa()), agora);

        if (!oportunidade.isAberta()) {
            filaDaAgenda.descartarPendentes(oportunidade, "Oportunidade encerrada como " + pedido.status().valor());
        }

        OportunidadeResposta retorno = null;
        if (pedido.status() == StatusOportunidade.RECUSADO && Boolean.TRUE.equals(pedido.lembrarNovamente())) {
            repository.flush(); // fecha a recusada no banco antes de abrir a nova (ver abrirNovoCiclo)
            Oportunidade nova = repository.save(Oportunidade.retornoDe(oportunidade, pedido.lembrarEm()));
            filaDaAgenda.enfileirarAlertas(nova);
            retorno = resposta(nova, hoje);
        }
        return new ResultadoDesfecho(resposta(oportunidade, hoje), retorno);
    }

    private void validarRecusa(DesfechoRequisicao pedido, LocalDate hoje) {
        if (pedido.status() != StatusOportunidade.RECUSADO) {
            return;
        }
        if (pedido.lembrarNovamente() == null) {
            throw new RegraDeNegocioException("Na recusa, informe se o sistema deve voltar a lembrar deste cliente.");
        }
        if (pedido.lembrarNovamente()) {
            if (pedido.lembrarEm() == null) {
                throw new RegraDeNegocioException("Informe a data em que o sistema deve voltar a lembrar.");
            }
            if (!pedido.lembrarEm().isAfter(hoje)) {
                throw new RegraDeNegocioException("A data para voltar a lembrar precisa estar no futuro.");
            }
        }
    }

    private void fechar(Oportunidade oportunidade, Servico servico, OffsetDateTime agora) {
        oportunidade.fecharComServico(servico, agora);
        filaDaAgenda.descartarPendentes(oportunidade, "Ciclo fechado por novo serviço");
    }

    private Oportunidade buscar(UUID id) {
        return repository.buscarCompleta(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Oportunidade não encontrada."));
    }

    private static EnumSet<StatusOportunidade> statusDa(Situacao situacao) {
        return switch (situacao) {
            case ABERTAS -> EnumSet.copyOf(StatusOportunidade.ABERTOS);
            case ENCERRADAS -> EnumSet.complementOf(EnumSet.copyOf(StatusOportunidade.ABERTOS));
            case TODAS -> EnumSet.allOf(StatusOportunidade.class);
        };
    }

    private static boolean aceitaResponsavel(Oportunidade o, Responsavel filtro, UUID usuarioId) {
        return switch (filtro) {
            case QUALQUER -> true;
            case NINGUEM -> o.getResponsavel() == null;
            case EU -> o.getResponsavel() != null && o.getResponsavel().getId().equals(usuarioId);
        };
    }

    private OportunidadeResposta resposta(Oportunidade o, LocalDate hoje) {
        return resposta(o, hoje, filaDaAgenda.temLembrete(o));
    }

    private OportunidadeResposta resposta(Oportunidade o, LocalDate hoje, boolean temLembrete) {
        LocalDate alertaComercial = regras.dataAlertaComercial(o.getDataPrevista());
        boolean naJanelaDeContato = o.isAberta() && !hoje.isBefore(alertaComercial);
        // RF-43: já entrou na janela do alerta comercial e ninguém assumiu.
        boolean semDonoUrgente = naJanelaDeContato && o.getResponsavel() == null;
        // A agenda só recebe alertas futuros (RN-13). Quem chegou atrasado (importado,
        // lançamento antigo) nunca terá lembrete: a fila é o único lugar que avisa.
        boolean semLembreteNaAgenda = naJanelaDeContato && !temLembrete;

        Cliente c = o.getCliente();
        Valvula v = o.getValvula();
        Bomba b = o.getBomba();
        Usuario r = o.getResponsavel();
        return new OportunidadeResposta(
                o.getId(), o.getEscopo(), o.getStatus(), o.isAberta(), o.getOrigem(),
                o.getDataPrevista(), alertaComercial, regras.dataLembreteFinal(o.getDataPrevista()),
                o.isAberta() ? ChronoUnit.DAYS.between(hoje, o.getDataPrevista()) : null,
                semDonoUrgente,
                semLembreteNaAgenda,
                c.getId(), c.getCodigoReferencia(), c.getNomeCondominio(), c.getBairro(),
                c.getSindicoResponsavel(), c.getTelefoneContato(),
                v == null ? null : v.getId(),
                v == null ? null : v.getCodigoReferencia(),
                v == null ? null : v.getEstacao().getLocalizacaoInstalacao(),
                v == null ? null : v.getEstacao().descreverAndares(),
                v == null ? null : v.getEstacao().isRequerFechamentoGeral(),
                r == null ? null : r.getId(),
                r == null ? null : r.getNome(),
                o.getAssumidaEm(), o.getNotas(), o.getMotivoRecusa(), o.getEncerradaEm(),
                b == null ? null : b.getId(),
                b == null ? null : b.getCodigoReferencia(),
                b == null ? null : b.descricao());
    }
}
