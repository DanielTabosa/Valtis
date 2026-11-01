package br.com.manutec.valtis.integracao;

import br.com.manutec.valtis.cliente.Cliente;
import br.com.manutec.valtis.cliente.ClienteService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RF-71: o cliente do Manutec Clientes vira (ou atualiza) o espelho no Valtis.
 *
 * Como o detalhe da fonte vira os campos do Valtis (suposição S-12, a confirmar):
 * - nome do condomínio: o nome fantasia; sem ele, o nome (razão social);
 * - endereço, bairro e cidade: do endereço principal ativo; sem ele, do primeiro ativo;
 * - síndico, telefone e e-mail: do contato com função "síndico"; sem ele, do primeiro contato.
 */
@Component
public class AplicadorDeCliente {

    private final ClienteService clientes;
    private final ObjectMapper json;
    private final Clock relogio;

    public AplicadorDeCliente(ClienteService clientes, ObjectMapper json, Clock relogio) {
        this.clientes = clientes;
        this.json = json;
        this.relogio = relogio;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void aplicar(EventoRecebido evento) {
        Contratos.ClienteDoManutec fonte = json.readValue(evento.getCorpo(), Contratos.ClienteDoManutec.class);
        Cliente.DadosDoManutec dados = dados(fonte, evento.getVersao());
        OffsetDateTime agora = OffsetDateTime.now(relogio);
        clientes.buscarPorManutecId(fonte.id()).ifPresentOrElse(
                c -> c.atualizarDoManutec(dados, agora),
                () -> clientes.criarEspelho(fonte.id(), dados, agora));
    }

    static Cliente.DadosDoManutec dados(Contratos.ClienteDoManutec fonte, String versao) {
        String nome = fonte.nomeFantasia() != null && !fonte.nomeFantasia().isBlank()
                ? fonte.nomeFantasia() : fonte.nome();
        Optional<Contratos.ClienteDoManutec.Endereco> endereco = endereco(fonte.enderecos());
        Optional<Contratos.ClienteDoManutec.Contato> contato = sindico(fonte.contatos());
        return new Cliente.DadosDoManutec(nome,
                endereco.map(AplicadorDeCliente::logradouro).orElse(null),
                endereco.map(Contratos.ClienteDoManutec.Endereco::bairro).orElse(null),
                endereco.map(Contratos.ClienteDoManutec.Endereco::cidade).orElse(null),
                contato.map(Contratos.ClienteDoManutec.Contato::nome).orElse(null),
                contato.map(Contratos.ClienteDoManutec.Contato::telefone).orElse(null),
                contato.map(Contratos.ClienteDoManutec.Contato::email).orElse(null),
                fonte.ativo(), versao);
    }

    private static Optional<Contratos.ClienteDoManutec.Endereco> endereco(
            List<Contratos.ClienteDoManutec.Endereco> enderecos) {
        if (enderecos == null) {
            return Optional.empty();
        }
        return enderecos.stream().filter(e -> e.ativo() && e.principal()).findFirst()
                .or(() -> enderecos.stream().filter(Contratos.ClienteDoManutec.Endereco::ativo).findFirst());
    }

    private static Optional<Contratos.ClienteDoManutec.Contato> sindico(
            List<Contratos.ClienteDoManutec.Contato> contatos) {
        if (contatos == null) {
            return Optional.empty();
        }
        return contatos.stream()
                .filter(c -> ResultadoDaAplicacao.normalizar(c.funcao()).contains("sindic"))
                .findFirst()
                .or(() -> contatos.stream().findFirst());
    }

    private static String logradouro(Contratos.ClienteDoManutec.Endereco e) {
        String texto = Stream.of(e.logradouro(), e.numero(), e.complemento())
                .filter(Objects::nonNull)
                .filter(parte -> !parte.isBlank())
                .collect(Collectors.joining(", "));
        return texto.isEmpty() ? null : texto;
    }
}
