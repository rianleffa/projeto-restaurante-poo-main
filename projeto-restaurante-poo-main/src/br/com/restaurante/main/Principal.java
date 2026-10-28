package br.com.restaurante.main;

import br.com.restaurante.model.*;
import br.com.restaurante.factory.*;
import br.com.restaurante.service.*;
import br.com.restaurante.repository.*;
import br.com.restaurante.web.RestauranteWebServer;

import java.util.Scanner;

public class Principal {
    public static void main(String[] args) {
        ClienteRepositorio clienteRepo = new ClienteRepositorio();
        ProdutoRepositorio produtoRepo = new ProdutoRepositorio();
        PedidoRepositorio pedidoRepo = new PedidoRepositorio();

        RestauranteServico servico = new RestauranteServico(clienteRepo, produtoRepo, pedidoRepo);
        RelatorioServico relatorioServico = new RelatorioServico(servico);

        // Adiciona dados iniciais para testar o sistema mais rápido
        inicializarDados(servico);

        // A interface web é a entrada padrão; o modo de console segue disponível com --console.
        if (args.length == 0 || !"--console".equalsIgnoreCase(args[0])) {
            try {
                new RestauranteWebServer(servico).iniciar();
            } catch (java.io.IOException e) {
                System.err.println("Não foi possível iniciar o servidor web: " + e.getMessage());
                System.err.println("Verifique se a porta 8080 está livre ou defina outra porta na variável PORT.");
            }
            return;
        }

        Scanner leitor = new Scanner(System.in);
        int opcao = 0;

        do {
            exibirMenu();
            System.out.print("Escolha uma opção: ");
            try {
                opcao = Integer.parseInt(leitor.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Por favor, insira um número válido!");
                continue;
            }

            switch (opcao) {
                case 1:
                    cadastrarCliente(servico, leitor);
                    break;
                case 2:
                    cadastrarProduto(servico, leitor);
                    break;
                case 3:
                    criarPedido(servico, leitor);
                    break;
                case 4:
                    alterarStatus(servico, leitor);
                    break;
                case 5:
                    relatorioServico.gerarRelatorio();
                    break;
                case 6:
                    System.out.println("\nFechando o sistema. Até mais!");
                    break;
                default:
                    System.out.println("Opção inválida! Escolha entre 1 e 6.");
            }
        } while (opcao != 6);

        leitor.close();
    }

    private static void exibirMenu() {
        System.out.println("\n=== MENU PRINCIPAL ===");
        System.out.println("1 - Cadastrar Cliente");
        System.out.println("2 - Cadastrar Produto");
        System.out.println("3 - Criar Pedido");
        System.out.println("4 - Alterar Status");
        System.out.println("5 - Relatórios");
        System.out.println("6 - Sair");
        System.out.println("======================");
    }

    private static void inicializarDados(RestauranteServico servico) {
        servico.cadastrarCliente("Carlos Silva", "(11) 99999-1234", "Rua das Flores, 100", "carlos@email.com");
        servico.cadastrarCliente("Ana Souza", "(11) 98888-5678", "Av. Paulista, 1500", "ana@email.com");

        servico.cadastrarProduto(new Pizza(10, "Pizza de Calabresa", "Molho de tomate, muçarela, calabresa e cebola", 40.00));
        servico.cadastrarProduto(new Pizza(20, "Pizza Margherita", "Molho de tomate, muçarela, tomate e manjericão", 38.00));
        servico.cadastrarProduto(new ProdutoComum(30, "Coca-Cola Lata", "Refrigerante de cola 350ml", 6.00));
        servico.cadastrarProduto(new ProdutoComum(40, "Suco de Laranja", "Copo de suco natural 400ml", 8.00));
    }

    private static void cadastrarCliente(RestauranteServico servico, Scanner leitor) {
        System.out.println("\n--- CADASTRAR NOVO CLIENTE ---");
        System.out.print("Nome: ");
        String nome = leitor.nextLine();
        System.out.print("Telefone: ");
        String telefone = leitor.nextLine();
        System.out.print("Endereço: ");
        String endereco = leitor.nextLine();
        System.out.print("E-mail: ");
        String email = leitor.nextLine();

        try {
            servico.cadastrarCliente(nome, telefone, endereco, email);
            System.out.println("Cliente cadastrado com sucesso!");
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void cadastrarProduto(RestauranteServico servico, Scanner leitor) {
        System.out.println("\n--- CADASTRAR NOVO PRODUTO ---");
        System.out.print("Código do Produto: ");
        int codigo;
        try {
            codigo = Integer.parseInt(leitor.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Código inválido! Digite apenas números.");
            return;
        }

        System.out.print("Nome do Produto: ");
        String nome = leitor.nextLine();
        System.out.print("Descrição: ");
        String descricao = leitor.nextLine();
        System.out.print("Preço unitário: ");
        double preco;
        try {
            preco = Double.parseDouble(leitor.nextLine().replace(",", "."));
        } catch (NumberFormatException e) {
            System.out.println("Preço inválido!");
            return;
        }

        System.out.print("É uma Pizza? (S/N): ");
        String ehPizza = leitor.nextLine().trim().toUpperCase();

        Produto novoProduto;
        if (ehPizza.equals("S")) {
            novoProduto = new Pizza(codigo, nome, descricao, preco);
        } else {
            novoProduto = new ProdutoComum(codigo, nome, descricao, preco);
        }

        try {
            servico.cadastrarProduto(novoProduto);
            System.out.println("Produto cadastrado com sucesso!");
        } catch (IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void criarPedido(RestauranteServico servico, Scanner leitor) {
        System.out.println("\n--- CRIAR NOVO PEDIDO ---");
        System.out.print("Insira o e-mail do cliente: ");
        String email = leitor.nextLine().trim();

        Pedido pedido = servico.iniciarPedido(email);
        if (pedido == null) {
            System.out.println("Cliente não localizado! Cadastre-o primeiro.");
            return;
        }

        boolean adicionando = true;
        while (adicionando) {
            System.out.println("\nCardápio:");
            for (Produto p : servico.obterProdutos()) {
                System.out.println("  [" + p.getCodigo() + "] " + p.getNome() + " - R$ " + String.format("%.2f", p.getPreco()) + " (" + p.getDescricao() + ")");
            }

            System.out.print("Código do produto para adicionar: ");
            int codProd;
            try {
                codProd = Integer.parseInt(leitor.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Código inválido!");
                continue;
            }

            Produto produto = servico.buscarProduto(codProd);
            if (produto == null) {
                System.out.println("Produto não encontrado no cardápio.");
                continue;
            }

            // Decorator aplicado apenas se for Pizza
            if (produto instanceof Pizza) {
                produto = aplicarDecoradores(produto, leitor);
            }

            System.out.print("Quantidade desejada: ");
            int qtd;
            try {
                qtd = Integer.parseInt(leitor.nextLine());
                if (qtd <= 0) {
                    System.out.println("A quantidade deve ser de no mínimo 1.");
                    continue;
                }
            } catch (NumberFormatException e) {
                System.out.println("Quantidade inválida!");
                continue;
            }

            ItemPedido item = new ItemPedido(produto, qtd);
            pedido.adicionarItem(item);
            System.out.println("Item adicionado!");

            System.out.print("Deseja adicionar mais algum produto? (S/N): ");
            String resposta = leitor.nextLine().trim().toUpperCase();
            if (!resposta.equals("S")) {
                adicionando = false;
            }
        }

        if (pedido.getItens().isEmpty()) {
            System.out.println("Nenhum item inserido. Pedido cancelado.");
            return;
        }

        // Aplicação do desconto de forma interativa durante a criação do pedido
        System.out.print("Deseja aplicar um desconto percentual neste pedido? (S/N): ");
        String querDesconto = leitor.nextLine().trim().toUpperCase();
        if (querDesconto.equals("S")) {
            System.out.print("Digite a porcentagem de desconto (0 a 100): ");
            try {
                double desconto = Double.parseDouble(leitor.nextLine().replace(",", "."));
                if (desconto >= 0 && desconto <= 100) {
                    pedido.setDescontoPercentual(desconto);
                    System.out.println("Desconto de " + desconto + "% aplicado com sucesso!");
                } else {
                    System.out.println("Desconto inválido! Valor deve ser entre 0 e 100.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Desconto inválido! Operação ignorada.");
            }
        }

        // Factory Method para selecionar forma de pagamento
        System.out.println("\nSelecione a Forma de Pagamento:");
        System.out.println("1 - PIX");
        System.out.println("2 - Cartão");
        System.out.println("3 - Dinheiro");
        System.out.print("Opção: ");
        int optPgto;
        try {
            optPgto = Integer.parseInt(leitor.nextLine());
        } catch (NumberFormatException e) {
            optPgto = 3;
        }

        String tipoPagamento;
        if (optPgto == 1) {
            tipoPagamento = "PIX";
        } else if (optPgto == 2) {
            tipoPagamento = "CARTAO";
        } else {
            tipoPagamento = "DINHEIRO";
        }

        try {
            Pagamento pgto = FabricaPagamento.criarPagamento(tipoPagamento);
            pedido.setPagamento(pgto);
            pgto.processar(pedido.calcularTotal());

            servico.finalizarPedido(pedido);
            System.out.println("\nPedido finalizado com sucesso!");
            System.out.println(pedido.obterResumo());
        } catch (Exception e) {
            System.out.println("Erro ao fechar pedido: " + e.getMessage());
        }
    }

    private static Produto aplicarDecoradores(Produto pizza, Scanner leitor) {
        Produto pizzaDecorada = pizza;
        boolean customizando = true;

        while (customizando) {
            System.out.println("\nDeseja personalizar a pizza [" + pizzaDecorada.getNome() + "]?");
            System.out.println("1 - Borda Recheada (+ R$ 10,00)");
            System.out.println("2 - Queijo Extra (+ R$ 5,00)");
            System.out.println("3 - Bacon (+ R$ 6,00)");
            System.out.println("4 - Concluir personalização");
            System.out.print("Opção: ");

            int escolha;
            try {
                escolha = Integer.parseInt(leitor.nextLine());
            } catch (NumberFormatException e) {
                escolha = 4;
            }

            switch (escolha) {
                case 1:
                    pizzaDecorada = new BordaRecheada(pizzaDecorada);
                    System.out.println("Borda Recheada adicionada!");
                    break;
                case 2:
                    pizzaDecorada = new QueijoExtra(pizzaDecorada);
                    System.out.println("Queijo Extra adicionado!");
                    break;
                case 3:
                    pizzaDecorada = new Bacon(pizzaDecorada);
                    System.out.println("Bacon adicionado!");
                    break;
                case 4:
                default:
                    customizando = false;
                    break;
            }
        }
        return pizzaDecorada;
    }

    private static void alterarStatus(RestauranteServico servico, Scanner leitor) {
        System.out.println("\n--- ALTERAR STATUS DO PEDIDO ---");
        System.out.print("Número do pedido: ");
        int numero;
        try {
            numero = Integer.parseInt(leitor.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Número de pedido inválido.");
            return;
        }

        Pedido pedido = servico.buscarPedido(numero);
        if (pedido == null) {
            System.out.println("Pedido não localizado.");
            return;
        }

        System.out.println("Status atual: " + pedido.getStatus().getDescricao());
        System.out.println("Selecione o novo status:");
        System.out.println("1 - RECEBIDO");
        System.out.println("2 - EM PREPARO");
        System.out.println("3 - SAIU PARA ENTREGA");
        System.out.println("4 - ENTREGUE");
        System.out.print("Opção: ");
        int optStatus;
        try {
            optStatus = Integer.parseInt(leitor.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Opção inválida.");
            return;
        }

        StatusPedido novoStatus;
        switch (optStatus) {
            case 1:
                novoStatus = StatusPedido.RECEBIDO;
                break;
            case 2:
                novoStatus = StatusPedido.EM_PREPARO;
                break;
            case 3:
                novoStatus = StatusPedido.SAIU_PARA_ENTREGA;
                break;
            case 4:
                novoStatus = StatusPedido.ENTREGUE;
                break;
            default:
                System.out.println("Opção inválida!");
                return;
        }

        servico.alterarStatusPedido(numero, novoStatus);
        System.out.println("Status atualizado com sucesso!");
    }
}
