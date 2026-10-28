package br.com.restaurante.factory;

public class PagamentoDinheiro implements Pagamento {
    @Override
    public void processar(double valor) {
        System.out.println("Pagamento de R$ " + String.format("%.2f", valor) + " registrado em Dinheiro.");
    }
}
