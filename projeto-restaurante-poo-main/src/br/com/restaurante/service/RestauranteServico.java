package br.com.restaurante.service;

import br.com.restaurante.model.*;
import br.com.restaurante.repository.*;

import java.util.List;

public class RestauranteServico {
    private ClienteRepositorio clienteRepositorio;
    private ProdutoRepositorio produtoRepositorio;
    private PedidoRepositorio pedidoRepositorio;
    private int proximoNumeroPedido;

    public RestauranteServico(ClienteRepositorio clienteRepositorio, 
                              ProdutoRepositorio produtoRepositorio, 
                              PedidoRepositorio pedidoRepositorio) {
        this.clienteRepositorio = clienteRepositorio;
        this.produtoRepositorio = produtoRepositorio;
        this.pedidoRepositorio = pedidoRepositorio;
        this.proximoNumeroPedido = 1;
    }

    public void cadastrarCliente(String nome, String telefone, String endereco, String email) {
        if (clienteRepositorio.buscarPorEmail(email) != null) {
            throw new IllegalArgumentException("Cliente com este e-mail já está cadastrado.");
        }
        Cliente novo = new Cliente(nome, telefone, endereco, email);
        clienteRepositorio.salvar(novo);
    }

    public void cadastrarProduto(Produto produto) {
        if (produtoRepositorio.buscarPorCodigo(produto.getCodigo()) != null) {
            throw new IllegalArgumentException("Produto com este código já está cadastrado.");
        }
        produtoRepositorio.salvar(produto);
    }

    public Pedido iniciarPedido(String emailCliente) {
        Cliente cliente = clienteRepositorio.buscarPorEmail(emailCliente);
        if (cliente == null) {
            return null;
        }
        return new Pedido(proximoNumeroPedido++, cliente);
    }

    public void finalizarPedido(Pedido pedido) {
        if (pedido.getItens().isEmpty()) {
            throw new IllegalStateException("Não é possível fechar um pedido sem itens.");
        }
        pedidoRepositorio.salvar(pedido);
    }

    public List<Cliente> obterClientes() {
        return clienteRepositorio.obterTodos();
    }

    public List<Produto> obterProdutos() {
        return produtoRepositorio.obterTodos();
    }

    public List<Pedido> obterPedidos() {
        return pedidoRepositorio.obterTodos();
    }

    public Produto buscarProduto(int codigo) {
        return produtoRepositorio.buscarPorCodigo(codigo);
    }

    public Pedido buscarPedido(int numero) {
        return pedidoRepositorio.buscarPorNumero(numero);
    }

    public boolean alterarStatusPedido(int numero, StatusPedido novoStatus) {
        Pedido p = pedidoRepositorio.buscarPorNumero(numero);
        if (p != null) {
            p.setStatus(novoStatus);
            return true;
        }
        return false;
    }
}
