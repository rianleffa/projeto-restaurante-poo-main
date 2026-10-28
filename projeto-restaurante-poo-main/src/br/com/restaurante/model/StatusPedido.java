package br.com.restaurante.model;

public enum StatusPedido {
    RECEBIDO("Recebido"),
    EM_PREPARO("Em Preparo"),
    SAIU_PARA_ENTREGA("Saiu para Entrega"),
    ENTREGUE("Entregue");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
