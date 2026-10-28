package br.com.restaurante.repository;

import br.com.restaurante.model.Produto;
import java.util.ArrayList;
import java.util.List;

public class ProdutoRepositorio {
    private List<Produto> produtos;

    public ProdutoRepositorio() {
        this.produtos = new ArrayList<>();
    }

    public void salvar(Produto produto) {
        produtos.add(produto);
    }

    public List<Produto> obterTodos() {
        return new ArrayList<>(produtos);
    }

    public Produto buscarPorCodigo(int codigo) {
        for (Produto p : produtos) {
            if (p.getCodigo() == codigo) {
                return p;
            }
        }
        return null;
    }
}
