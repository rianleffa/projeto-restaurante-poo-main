package br.com.restaurante.model;

public class Bacon extends DecoradorPizza {
    public Bacon(Produto produtoDecorado) {
        super(produtoDecorado);
    }

    @Override
    public String getNome() {
        return produtoDecorado.getNome() + " + Bacon";
    }

    @Override
    public String getDescricao() {
        return produtoDecorado.getDescricao() + " (com porção extra de bacon)";
    }

    @Override
    public double getPreco() {
        return produtoDecorado.getPreco() + 6.0;
    }
}
