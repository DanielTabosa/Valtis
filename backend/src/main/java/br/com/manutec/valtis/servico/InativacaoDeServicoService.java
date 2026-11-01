package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.oportunidade.OportunidadeService;
import br.com.manutec.valtis.usuario.Usuario;
import br.com.manutec.valtis.usuario.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * RN-26: o admin inativa um serviço, com motivo. O caso que motivou a regra é a
 * OS do Jiraya reaberta ou cancelada depois de aplicada (P-27), mas vale para
 * qualquer serviço lançado por engano.
 *
 * O serviço inativo sai do cálculo do ciclo: se era ele quem contava, o ciclo
 * volta a contar da data-base que sobrou (o último serviço ativo anterior; na
 * bomba, também a data de cadastro, RF-80). Sem data-base, o ciclo é cancelado (P-32).
 */
@Service
public class InativacaoDeServicoService {

    private final ServicoRepository servicoRepository;
    private final OportunidadeService oportunidades;
    private final UsuarioService usuarios;
    private final Clock relogio;

    public InativacaoDeServicoService(ServicoRepository servicoRepository, OportunidadeService oportunidades,
                                      UsuarioService usuarios, Clock relogio) {
        this.servicoRepository = servicoRepository;
        this.oportunidades = oportunidades;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    @Transactional
    public void inativar(UUID servicoId, String motivo, UUID usuarioId) {
        Usuario quem = usuarios.buscarAtivo(usuarioId);
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado."));

        boolean contavaParaOCiclo = oportunidades.cicloAbertoNasceuDe(servico);
        String motivoLimpo = Textos.limpar(motivo);
        servico.inativar(quem, motivoLimpo, OffsetDateTime.now(relogio));
        servicoRepository.flush(); // a consulta abaixo precisa já ignorar este serviço

        if (!contavaParaOCiclo) {
            return; // serviço antigo ou já superado: o ciclo atual não depende dele
        }
        Optional<LocalDate> dataBase = servico.isDaBomba() ? dataBaseDaBomba(servico) : dataBaseDaValvula(servico);
        if (dataBase.isEmpty()) {
            oportunidades.cancelarCicloDoServico(servico, motivoLimpo);
            return;
        }
        oportunidades.recontarCicloSemServico(servico, dataBase.get());
    }

    private Optional<LocalDate> dataBaseDaValvula(Servico servico) {
        return servicoRepository.ultimaDataConcluida(servico.getValvula().getId());
    }

    /** RF-80: na bomba, a data-base é a mais recente entre o cadastro e o último serviço ativo. */
    private Optional<LocalDate> dataBaseDaBomba(Servico servico) {
        Optional<LocalDate> ultimoServico = servicoRepository.ultimaDataConcluidaDaBomba(servico.getBomba().getId());
        return Stream.of(ultimoServico.orElse(null), servico.getBomba().getUltimaManutencao())
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder());
    }
}
