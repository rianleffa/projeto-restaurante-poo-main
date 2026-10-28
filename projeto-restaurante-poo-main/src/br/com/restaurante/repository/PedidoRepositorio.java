package br.com.restaurante.repository;

import br.com.restaurante.model.Pedido;
import java.util.ArrayList;
import java.util.List;

public class PedidoRepositorio {
    private List<Pedido> pedidos;

    public PedidoRepositorio() {
        this.pedidos = new ArrayList<>();
    }

    public void salvar(Pedido pedido) {
        pedidos.add(pedido);
    }

    public List<Pedido> obterTodos() {
        return new ArrayList<>(pedidos);
    }

    public Pedido buscarPorNumero(int numero) {
        for (Pedido p : pedidos) {
            if (p.getNumeroPedido() == numero) {
                return p;
            }
        }
        return null;
    }
}
