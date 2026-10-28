package br.com.restaurante.model;

public class QueijoExtra extends DecoradorPizza {
    public QueijoExtra(Produto produtoDecorado) {
        super(produtoDecorado);
    }

    @Override
    public String getNome() {
        return produtoDecorado.getNome() + " + Queijo Extra";
    }

    @Override
    public String getDescricao() {
        return produtoDecorado.getDescricao() + " (com porção extra de queijo)";
    }

    @Override
    public double getPreco() {
        return produtoDecorado.getPreco() + 5.0;
    }
}
