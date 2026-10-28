package br.com.restaurante.factory;

public class FabricaPagamento {
    public static Pagamento criarPagamento(String tipo) {
        if (tipo == null) {
            return null;
        }
        switch (tipo.toUpperCase().trim()) {
            case "PIX":
                return new PagamentoPix();
            case "CARTAO":
            case "CARTÃO":
                return new PagamentoCartao();
            case "DINHEIRO":
                return new PagamentoDinheiro();
            default:
                throw new IllegalArgumentException("Forma de pagamento não suportada: " + tipo);
        }
    }
}
