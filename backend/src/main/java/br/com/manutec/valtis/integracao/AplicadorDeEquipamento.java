package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.bomba.Bomba;
import br.com.manutec.valtis.bomba.BombaRepository;
import br.com.manutec.valtis.bomba.BombaService;
import br.com.manutec.valtis.bomba.ConjuntoBombeamento;
import br.com.manutec.valtis.bomba.ConjuntoRepository;
import br.com.manutec.valtis.bomba.OcupacaoBomba;
import br.com.manutec.valtis.bomba.OcupacaoRepository;
import br.com.manutec.valtis.bomba.TipoBomba;
import br.com.manutec.valtis.bomba.TipoBombaRepository;
import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.cliente.ClienteService;
import br.com.manutec.valtis.comum.ValtisPropriedades;
import br.com.manutec.valtis.estacao.Estacao;
import br.com.manutec.valtis.estacao.EstacaoRepository;
import br.com.manutec.valtis.valvula.Valvula;
import br.com.manutec.valtis.valvula.ValvulaRepository;
import br.com.manutec.valtis.valvula.ValvulaService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * RF-72, RF-73, RF-79: o equipamento do Jiraya vira (ou atualiza) a VRP ou a
 * bomba espelhada, na estação ou no conjunto derivado da localização interna.
 *
 * Só os campos que vêm da fonte são escritos; o que é do Valtis (especificação,
 * andares, periodicidade, dados elétricos) nunca é tocado (RN-24 por campo).
 */
@Component
public class AplicadorDeEquipamento {

    /** RF-73: equipamento sem localização interna. */
    static final String SEM_LOCALIZACAO = "Sem localização";

    private final ObjectMapper json;
    private final Clock relogio;
    private final ClienteService clientes;
    private final ValvulaRepository valvulas;
    private final ValvulaService valvulaService;
    private final EstacaoRepository estacoes;
    private final BombaRepository bombas;
    private final BombaService bombaService;
    private final ConjuntoRepository conjuntos;
    private final OcupacaoRepository ocupacoes;
    private final TipoBombaRepository tiposDeBomba;
    private final TiposDoJiraya tiposDoJiraya;
    private final List<String> tiposValvula;
    private final List<String> tiposBomba;

    public AplicadorDeEquipamento(ObjectMapper json, Clock relogio, ClienteService clientes,
                                  ValvulaRepository valvulas, ValvulaService valvulaService,
                                  EstacaoRepository estacoes, BombaRepository bombas, BombaService bombaService,
                                  ConjuntoRepository conjuntos, OcupacaoRepository ocupacoes,
                                  TipoBombaRepository tiposDeBomba, TiposDoJiraya tiposDoJiraya,
                                  ValtisPropriedades propriedades) {
        this.json = json;
        this.relogio = relogio;
        this.clientes = clientes;
        this.valvulas = valvulas;
        this.valvulaService = valvulaService;
        this.estacoes = estacoes;
        this.bombas = bombas;
        this.bombaService = bombaService;
        this.conjuntos = conjuntos;
        this.ocupacoes = ocupacoes;
        this.tiposDeBomba = tiposDeBomba;
        this.tiposDoJiraya = tiposDoJiraya;
        ValtisPropriedades.Integracao config = propriedades.integracao();
        this.tiposValvula = normalizados(config == null ? null : config.tiposValvula(), "vrp");
        this.tiposBomba = normalizados(config == null ? null : config.tiposBomba(), "bomba");
    }

    enum Classe { VALVULA, BOMBA, OUTRO }

    @Transactional(propagation = Propagation.MANDATORY)
    public void aplicar(EventoRecebido evento) {
        Contratos.EquipamentoDoJiraya e = json.readValue(evento.getCorpo(), Contratos.EquipamentoDoJiraya.class);
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        Classe classe = classificar(e);
        exigirMesmaClasseDoEspelho(e, classe);
        switch (classe) {
            case VALVULA -> aplicarValvula(e, evento.getVersao(), agora);
            case BOMBA -> aplicarBomba(e, evento.getVersao(), agora);
            case OUTRO -> throw new ResultadoDaAplicacao.Pendencia(ResultadoDaAplicacao.TIPO_NAO_MAPEADO + ": "
                    + (e.tipo() == null ? "sem tipo" : tiposDoJiraya.nomeAtual(e.tipo())) + ".");
        }
    }

    /** Classe do equipamento de um corpo guardado, com o nome ATUAL do tipo (reclassificação). */
    Classe classificarCorpo(String corpo) {
        return classificar(json.readValue(corpo, Contratos.EquipamentoDoJiraya.class));
    }

    /**
     * RF-72, P-21: pelo nome do tipo, sem diferenciar caixa nem acento. VRP antes de bomba.
     * O nome é o atual do Jiraya (TiposDoJiraya), não o que veio no corpo.
     */
    Classe classificar(Contratos.EquipamentoDoJiraya e) {
        String tipo = ResultadoDaAplicacao.normalizar(e.tipo() == null ? null : tiposDoJiraya.nomeAtual(e.tipo()));
        if (tiposValvula.stream().anyMatch(tipo::contains)) return Classe.VALVULA;
        if (tiposBomba.stream().anyMatch(tipo::contains)) return Classe.BOMBA;
        return Classe.OUTRO;
    }

    /**
     * Um equipamento já espelhado não muda de classe sozinho: a VRP e a bomba têm
     * histórico, ciclo e oportunidades separados. Sem esta trava, uma VRP cujo tipo
     * virasse "bomba" no Jiraya ganharia uma bomba duplicada ao lado (S-16).
     */
    private void exigirMesmaClasseDoEspelho(Contratos.EquipamentoDoJiraya e, Classe classe) {
        String tipo = e.tipo() == null ? "sem tipo" : tiposDoJiraya.nomeAtual(e.tipo());
        Optional<Valvula> valvula = valvulas.findByJirayaEquipamentoId(e.id());
        if (valvula.isPresent() && classe != Classe.VALVULA) {
            throw reclassificado(e, "a VRP " + valvula.get().getCodigoReferencia(), tipo);
        }
        Optional<Bomba> bomba = bombas.findByJirayaEquipamentoId(e.id());
        if (bomba.isPresent() && classe != Classe.BOMBA) {
            throw reclassificado(e, "a bomba " + bomba.get().getCodigoReferencia(), tipo);
        }
    }

    private static ResultadoDaAplicacao.Pendencia reclassificado(Contratos.EquipamentoDoJiraya e, String espelho,
                                                                 String tipo) {
        return new ResultadoDaAplicacao.Pendencia("O equipamento " + e.id() + " do Jiraya está no Valtis como "
                + espelho + ", mas agora tem o tipo \"" + tipo + "\". Mudar de classe não é automático: confira o "
                + "tipo no Jiraya ou trate o equipamento à mão.");
    }

    private void aplicarValvula(Contratos.EquipamentoDoJiraya e, String versao, OffsetDateTime agora) {
        Valvula.DadosDoJiraya dados = new Valvula.DadosDoJiraya(e.fabricante(), e.modelo(), e.numeroSerie(),
                e.identificacao(), versao);
        Optional<Valvula> existente = valvulas.findByJirayaEquipamentoId(e.id());
        if (existente.isPresent()) {
            Valvula v = existente.get();
            exigirMesmoCliente(e, v.getEstacao().getCliente());
            if (!chave(e.localizacaoInterna()).equals(v.getEstacao().getChaveIntegracao())) {
                // A válvula não troca de estação (estacao_id não muda): a história técnica é da estação.
                throw new ResultadoDaAplicacao.Pendencia("A VRP " + v.getCodigoReferencia()
                        + " mudou de localização no Jiraya (agora: " + localizacao(e) + "). Mover entre estações "
                        + "não é automático.");
            }
            v.atualizarDoJiraya(dados, agora);
            if (saiuDeOperacao(e)) {
                v.inativar(); // RF-79: substituída ou excluída no Jiraya
            }
            return;
        }
        if (saiuDeOperacao(e)) {
            return; // nunca espelhada e já fora de uso: nada a fazer
        }
        Cliente cliente = clienteDoValtis(e);
        Estacao estacao = estacoes.findByClienteIdAndChaveIntegracao(cliente.getId(), chave(e.localizacaoInterna()))
                .orElseGet(() -> estacoes.saveAndFlush(
                        Estacao.derivadaDoJiraya(cliente, localizacao(e), chave(e.localizacaoInterna()))));
        valvulaService.criarEspelho(estacao, e.id(), dados, agora);
    }

    private void aplicarBomba(Contratos.EquipamentoDoJiraya e, String versao, OffsetDateTime agora) {
        Bomba.DadosDoJiraya dados = new Bomba.DadosDoJiraya(e.fabricante(), e.modelo(), e.identificacao(), versao);
        Optional<Bomba> existente = bombas.findByJirayaEquipamentoId(e.id());
        if (existente.isPresent()) {
            Bomba b = existente.get();
            exigirMesmoCliente(e, b.getCliente());
            b.atualizarDoJiraya(dados, agora);
            if (saiuDeOperacao(e)) {
                b.inativar(); // RF-79
                encerrarOcupacoes(b, dataDeSaida(e));
            } else {
                moverSeMudouDeLugar(b, e);
            }
            return;
        }
        if (saiuDeOperacao(e)) {
            return;
        }
        Cliente cliente = clienteDoValtis(e);
        TipoBomba tipo = tipoDeBomba(e);
        Bomba nova = bombaService.criarEspelho(cliente, e.id(), dados, agora);
        ocupar(nova, conjuntoDerivado(cliente, tipo, e));
    }

    /** A bomba circula; o histórico de posição é a ocupação (D-27). Mudou de lugar no Jiraya, muda aqui. */
    private void moverSeMudouDeLugar(Bomba bomba, Contratos.EquipamentoDoJiraya e) {
        List<OcupacaoBomba> abertas = ocupacoes.abertasDaBomba(bomba.getId());
        String chave = chave(e.localizacaoInterna());
        boolean noLugar = abertas.stream().anyMatch(o -> chave.equals(o.getConjunto().getChaveIntegracao()));
        if (noLugar) {
            return;
        }
        TipoBomba tipo = tipoDeBomba(e);
        encerrarOcupacoes(bomba, LocalDate.now(relogio));
        ocupar(bomba, conjuntoDerivado(bomba.getCliente(), tipo, e));
    }

    private void ocupar(Bomba bomba, ConjuntoBombeamento conjunto) {
        String posicao = "%02d".formatted(ocupacoes.maiorPosicaoNoConjunto(conjunto.getId()) + 1);
        ocupacoes.save(OcupacaoBomba.daIntegracao(bomba, conjunto, posicao, LocalDate.now(relogio)));
    }

    private void encerrarOcupacoes(Bomba bomba, LocalDate data) {
        for (OcupacaoBomba aberta : ocupacoes.abertasDaBomba(bomba.getId())) {
            LocalDate fim = data.isBefore(aberta.getInicio()) ? aberta.getInicio() : data;
            aberta.encerrar(fim, "Encerrada pelo Jiraya");
        }
    }

    private ConjuntoBombeamento conjuntoDerivado(Cliente cliente, TipoBomba tipo, Contratos.EquipamentoDoJiraya e) {
        String chave = chave(e.localizacaoInterna());
        return conjuntos.findByClienteIdAndTipoIdAndChaveIntegracao(cliente.getId(), tipo.getId(), chave)
                .orElseGet(() -> conjuntos.saveAndFlush(
                        ConjuntoBombeamento.derivadoDoJiraya(cliente, tipo, localizacao(e), chave)));
    }

    /**
     * P-21: um tipo por função no Jiraya ("Bomba de recalque"). O tipo do Valtis é o do
     * catálogo cujo nome aparece no nome do Jiraya; havendo mais de um, o mais longo.
     */
    private TipoBomba tipoDeBomba(Contratos.EquipamentoDoJiraya e) {
        String doJiraya = ResultadoDaAplicacao.normalizar(e.tipo().nome());
        return tiposDeBomba.findByAtivoTrueOrderByNome().stream()
                .filter(t -> doJiraya.contains(ResultadoDaAplicacao.normalizar(t.getNome())))
                .max(Comparator.comparingInt(t -> t.getNome().length()))
                .orElseThrow(() -> new ResultadoDaAplicacao.Pendencia("O tipo \"" + e.tipo().nome()
                        + "\" do Jiraya não corresponde a nenhum tipo de bomba do catálogo do Valtis."));
    }

    private Cliente clienteDoValtis(Contratos.EquipamentoDoJiraya e) {
        if (e.cliente() == null || e.cliente().manutecId() == null) {
            throw new ResultadoDaAplicacao.Pendencia("O cliente do equipamento ("
                    + (e.cliente() == null ? "?" : e.cliente().nome())
                    + ") não está vinculado ao Manutec Clientes no Jiraya.");
        }
        // O Manutec Clientes pode só não ter sido lido ainda: espera.
        return clientes.buscarPorManutecId(e.cliente().manutecId())
                .orElseThrow(() -> new ResultadoDaAplicacao.Aguardar("O cliente " + e.cliente().nome()
                        + " (Manutec Clientes " + e.cliente().manutecId() + ") ainda não chegou ao Valtis."));
    }

    private static void exigirMesmoCliente(Contratos.EquipamentoDoJiraya e, Cliente atual) {
        Long manutecIdNovo = e.cliente() == null ? null : e.cliente().manutecId();
        if (atual != null && atual.getManutecId() != null && !atual.getManutecId().equals(manutecIdNovo)) {
            throw new ResultadoDaAplicacao.Pendencia("O equipamento mudou de cliente no Jiraya. Trocar o dono de um "
                    + "equipamento com histórico não é automático.");
        }
    }

    private static boolean saiuDeOperacao(Contratos.EquipamentoDoJiraya e) {
        return e.excluido() || e.substituidoPor() != null;
    }

    private LocalDate dataDeSaida(Contratos.EquipamentoDoJiraya e) {
        if (e.substituidoPor() != null && e.substituidoPor().em() != null) {
            return OffsetDateTime.parse(e.substituidoPor().em()).atZoneSameInstant(relogio.getZone()).toLocalDate();
        }
        return LocalDate.now(relogio);
    }

    private static String localizacao(Contratos.EquipamentoDoJiraya e) {
        return e.localizacaoInterna() == null || e.localizacaoInterna().isBlank()
                ? SEM_LOCALIZACAO : e.localizacaoInterna().strip();
    }

    /** RF-73: uma estação/conjunto por localização normalizada; sem localização, "sem-localizacao". */
    static String chave(String localizacaoInterna) {
        String chave = ResultadoDaAplicacao.normalizar(localizacaoInterna);
        return chave.isEmpty() ? "sem-localizacao" : chave;
    }

    private static List<String> normalizados(List<String> termos, String padrao) {
        if (termos == null || termos.isEmpty()) {
            return List.of(padrao);
        }
        return termos.stream().map(ResultadoDaAplicacao::normalizar).filter(t -> !t.isEmpty()).toList();
    }
}
