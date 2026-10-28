package br.com.restaurante.repository;

import br.com.restaurante.model.Cliente;
import java.util.ArrayList;
import java.util.List;

public class ClienteRepositorio {
    private List<Cliente> clientes;

    public ClienteRepositorio() {
        this.clientes = new ArrayList<>();
    }

    public void salvar(Cliente cliente) {
        clientes.add(cliente);
    }

    public List<Cliente> obterTodos() {
        return new ArrayList<>(clientes);
    }

    public Cliente buscarPorEmail(String email) {
        for (Cliente c : clientes) {
            if (c.getEmail().equalsIgnoreCase(email)) {
                return c;
            }
        }
        return null;
    }
}
