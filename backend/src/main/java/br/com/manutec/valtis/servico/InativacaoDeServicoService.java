package br.com.manutec.valtis.servico;

import br.com.manutec.valtis.comum.RecursoNaoEncontradoException;
import br.com.manutec.valtis.comum.RegraDeNegocioException;
import br.com.manutec.valtis.comum.Textos;
import br.com.manutec.valtis.oportunidade.OportunidadeService;
import br.com.manutec.valtis.usuario.Usuario;
import br.com.manutec.valtis.usuario.UsuarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * RN-26: o admin inativa um serviço, com motivo. O caso que motivou a regra é a
 * OS do Jiraya reaberta ou cancelada depois de aplicada (P-27), mas vale para
 * qualquer serviço lançado por engano.
 *
 * O serviço inativo sai do cálculo do ciclo: se era ele quem contava, o ciclo
 * volta a contar do último serviço ativo anterior.
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
        servico.inativar(quem, Textos.limpar(motivo), OffsetDateTime.now(relogio));
        servicoRepository.flush(); // a consulta abaixo precisa já ignorar este serviço

        if (!contavaParaOCiclo) {
            return; // serviço antigo ou já superado: o ciclo atual não depende dele
        }
        Optional<LocalDate> anterior = servico.isDaBomba()
                ? servicoRepository.ultimaDataConcluidaDaBomba(servico.getBomba().getId())
                : servicoRepository.ultimaDataConcluida(servico.getValvula().getId());
        if (anterior.isEmpty()) {
            // Sem serviço anterior o equipamento ficaria sem ciclo, e não há desfecho
            // previsto para uma oportunidade que perde a origem. Decisão pendente (P-32).
            throw new RegraDeNegocioException("Este é o único serviço ativo do equipamento e o ciclo atual conta "
                    + "dele. Inativá-lo deixaria o equipamento sem ciclo; essa situação aguarda decisão (P-32).");
        }
        oportunidades.recontarCicloSemServico(servico, anterior.get());
    }
}
