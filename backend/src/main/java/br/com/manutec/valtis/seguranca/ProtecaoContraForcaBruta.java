package br.com.manutec.valtis.seguranca;

import br.com.manutec.valtis.comum.ValtisPropriedades;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueia o login de um e-mail por alguns minutos depois de N senhas erradas.
 *
 * Sem isso, um robô testaria milhares de senhas por minuto. Fica em memória:
 * com uma instância só e 3 usuários, não justifica tabela nem cache externo.
 * Reiniciar a aplicação zera os contadores, o que é aceitável aqui.
 */
@Component
public class ProtecaoContraForcaBruta {

    private record Tentativas(int falhas, Instant bloqueadoAte) {}

    private final Map<String, Tentativas> porEmail = new ConcurrentHashMap<>();
    private final int maxTentativas;
    private final Duration bloqueio;
    private final Clock relogio;

    public ProtecaoContraForcaBruta(ValtisPropriedades propriedades, Clock relogio) {
        this.maxTentativas = propriedades.seguranca().maxTentativasLogin();
        this.bloqueio = Duration.ofMinutes(propriedades.seguranca().bloqueioLoginMinutos());
        this.relogio = relogio;
    }

    /** Minutos restantes de bloqueio, ou 0 se pode tentar. */
    public long minutosBloqueado(String email) {
        Tentativas t = porEmail.get(email);
        if (t == null || t.bloqueadoAte() == null) {
            return 0;
        }
        Duration restante = Duration.between(relogio.instant(), t.bloqueadoAte());
        if (restante.isNegative() || restante.isZero()) {
            porEmail.remove(email);
            return 0;
        }
        return Math.max(1, restante.toMinutes() + (restante.toSecondsPart() > 0 ? 1 : 0));
    }

    public void registrarFalha(String email) {
        porEmail.compute(email, (chave, atual) -> {
            int falhas = (atual == null ? 0 : atual.falhas()) + 1;
            Instant bloqueadoAte = falhas >= maxTentativas ? relogio.instant().plus(bloqueio) : null;
            return new Tentativas(falhas, bloqueadoAte);
        });
    }

    public void registrarSucesso(String email) {
        porEmail.remove(email);
    }
}
