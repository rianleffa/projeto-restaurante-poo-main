package br.com.restaurante.model;

public class BordaRecheada extends DecoradorPizza {
    public BordaRecheada(Produto produtoDecorado) {
        super(produtoDecorado);
    }

    @Override
    public String getNome() {
        return produtoDecorado.getNome() + " + Borda Recheada";
    }

    @Override
    public String getDescricao() {
        return produtoDecorado.getDescricao() + " (com borda recheada de catupiry)";
    }

    @Override
    public double getPreco() {
        return produtoDecorado.getPreco() + 10.0;
    }
}
