package br.com.manutec.valtis.integracao;

/** Cada lista de alterações lida, com o cursor próprio (cursor_integracao). */
public enum RecursoLido {

    CLIENTES(OrigemIntegracao.MANUTEC_CLIENTES, "clientes", TipoEventoRecebido.CLIENTE),
    EQUIPAMENTOS(OrigemIntegracao.JIRAYA, "equipamentos", TipoEventoRecebido.EQUIPAMENTO),
    ORDENS_SERVICO(OrigemIntegracao.JIRAYA, "ordens_servico", TipoEventoRecebido.ORDEM_SERVICO);

    private final OrigemIntegracao origem;
    private final String nome;
    private final TipoEventoRecebido tipo;

    RecursoLido(OrigemIntegracao origem, String nome, TipoEventoRecebido tipo) {
        this.origem = origem;
        this.nome = nome;
        this.tipo = tipo;
    }

    public CursorIntegracao.Chave chave() {
        return new CursorIntegracao.Chave(origem.valor(), nome);
    }

    public OrigemIntegracao origem() { return origem; }
    public String nome() { return nome; }
    public TipoEventoRecebido tipo() { return tipo; }
}
