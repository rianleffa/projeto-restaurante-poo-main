package br.com.restaurante.model;

public abstract class DecoradorPizza extends Produto {
    protected Produto produtoDecorado;

    public DecoradorPizza(Produto produtoDecorado) {
        super(produtoDecorado.getCodigo(), produtoDecorado.getNome(), 
              produtoDecorado.getDescricao(), produtoDecorado.getPreco());
        this.produtoDecorado = produtoDecorado;
    }

    @Override
    public int getCodigo() {
        return produtoDecorado.getCodigo();
    }

    @Override
    public abstract String getNome();

    @Override
    public abstract String getDescricao();

    @Override
    public abstract double getPreco();
}
