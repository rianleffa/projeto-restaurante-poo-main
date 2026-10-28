package br.com.restaurante.service;

import br.com.restaurante.model.*;
import java.util.List;

public class RelatorioServico {
    private RestauranteServico restauranteServico;

    public RelatorioServico(RestauranteServico restauranteServico) {
        this.restauranteServico = restauranteServico;
    }

    public void gerarRelatorio() {
        List<Cliente> clientes = restauranteServico.obterClientes();
        List<Produto> produtos = restauranteServico.obterProdutos();
        List<Pedido> pedidos = restauranteServico.obterPedidos();

        double totalVendido = 0.0;
        for (Pedido p : pedidos) {
            totalVendido += p.calcularTotal();
        }

        System.out.println("\n=== RELATÓRIOS DO SISTEMA ===");
        
        System.out.println("\n1. Clientes Cadastrados:");
        if (clientes.isEmpty()) {
            System.out.println("   Nenhum cliente cadastrado.");
        } else {
            for (Cliente c : clientes) {
                System.out.println("   - " + c.getNome() + " (E-mail: " + c.getEmail() + ")");
            }
        }

        System.out.println("\n2. Produtos Cadastrados:");
        if (produtos.isEmpty()) {
            System.out.println("   Nenhum produto cadastrado.");
        } else {
            for (Produto p : produtos) {
                System.out.println("   - [" + p.getCodigo() + "] " + p.getNome() + " | Preço: R$ " + String.format("%.2f", p.getPreco()));
            }
        }

        System.out.println("\n3. Pedidos Realizados:");
        if (pedidos.isEmpty()) {
            System.out.println("   Nenhum pedido realizado.");
        } else {
            for (Pedido p : pedidos) {
                System.out.println("   - Pedido #" + p.getNumeroPedido() + " | Cliente: " + p.getCliente().getNome() + " | Total: R$ " + String.format("%.2f", p.calcularTotal()) + " | Status: " + p.getStatus().getDescricao());
            }
        }

        System.out.println("\n4. Total Vendido:");
        System.out.println("   Faturamento total das vendas: R$ " + String.format("%.2f", totalVendido));
        System.out.println("=============================");
    }
}
