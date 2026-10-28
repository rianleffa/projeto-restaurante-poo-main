package br.com.restaurante.model;

import br.com.restaurante.observer.ObservadorPedido;
import br.com.restaurante.factory.Pagamento;
import java.util.ArrayList;
import java.util.List;

public class Pedido {
    private int numeroPedido;
    private Cliente cliente;
    private List<ItemPedido> itens;
    private StatusPedido status;
    private Pagamento pagamento;
    private double descontoPercentual;
    private List<ObservadorPedido> observadores;

    public Pedido(int numeroPedido, Cliente cliente) {
        this.numeroPedido = numeroPedido;
        this.cliente = cliente;
        this.itens = new ArrayList<>();
        this.status = StatusPedido.RECEBIDO;
        this.descontoPercentual = 0.0;
        this.observadores = new ArrayList<>();
        adicionarObservador(cliente);
    }

    public void adicionarItem(ItemPedido item) {
        this.itens.add(item);
    }

    public void removerItem(ItemPedido item) {
        this.itens.remove(item);
    }

    public void adicionarObservador(ObservadorPedido obs) {
        this.observadores.add(obs);
    }

    public void removerObservador(ObservadorPedido obs) {
        this.observadores.remove(obs);
    }

    public void notificarObservadores() {
        String mensagem = "O status do seu pedido #" + numeroPedido + " mudou para: [" + status.getDescricao() + "]";
        for (ObservadorPedido obs : observadores) {
            obs.atualizar(mensagem);
        }
    }

    public double calcularTotal() {
        double subtotal = 0.0;
        for (ItemPedido item : itens) {
            subtotal += item.getSubtotal();
        }
        double desconto = subtotal * (descontoPercentual / 100.0);
        return subtotal - desconto;
    }

    public int getNumeroPedido() {
        return numeroPedido;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
        notificarObservadores();
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    public void setPagamento(Pagamento pagamento) {
        this.pagamento = pagamento;
    }

    public double getDescontoPercentual() {
        return descontoPercentual;
    }

    public void setDescontoPercentual(double descontoPercentual) {
        this.descontoPercentual = descontoPercentual;
    }

    public String obterResumo() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n==========================================");
        sb.append("\n          RESUMO DO PEDIDO #").append(numeroPedido);
        sb.append("\n==========================================");
        sb.append("\nCliente:   ").append(cliente.getNome());
        sb.append("\nStatus:    ").append(status.getDescricao());
        sb.append("\nItens:");
        for (ItemPedido item : itens) {
            sb.append("\n  - ").append(item.toString());
        }
        if (descontoPercentual > 0) {
            sb.append("\nDesconto:  ").append(descontoPercentual).append("%");
        }
        if (pagamento != null) {
            String nomePagamento = pagamento.getClass().getSimpleName().replace("Pagamento", "");
            sb.append("\nPagamento: ").append(nomePagamento);
        }
        sb.append(String.format("\nTOTAL:     R$ %.2f", calcularTotal()));
        sb.append("\n==========================================");
        return sb.toString();
    }
}
