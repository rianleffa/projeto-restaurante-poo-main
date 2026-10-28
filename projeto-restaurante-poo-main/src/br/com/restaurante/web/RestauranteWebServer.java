package br.com.restaurante.web;

import br.com.restaurante.factory.FabricaPagamento;
import br.com.restaurante.factory.Pagamento;
import br.com.restaurante.model.Bacon;
import br.com.restaurante.model.BordaRecheada;
import br.com.restaurante.model.ItemPedido;
import br.com.restaurante.model.Pedido;
import br.com.restaurante.model.Pizza;
import br.com.restaurante.model.Produto;
import br.com.restaurante.model.ProdutoComum;
import br.com.restaurante.model.QueijoExtra;
import br.com.restaurante.model.StatusPedido;
import br.com.restaurante.service.RestauranteServico;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/** Servidor HTTP leve para apresentar a interface web mantendo os serviços Java existentes. */
public class RestauranteWebServer {
    private final RestauranteServico restaurante;
    private final List<String> notificacoes = new ArrayList<>();
    private final Path pastaWeb = Path.of("web").toAbsolutePath().normalize();
    private HttpServer servidor;

    public RestauranteWebServer(RestauranteServico restaurante) {
        this.restaurante = restaurante;
    }

    public void iniciar() throws IOException {
        int porta = obterPorta();
        String host = System.getenv("HOST");
        if (host == null || host.isBlank()) host = "127.0.0.1";
        servidor = HttpServer.create(new InetSocketAddress(host, porta), 0);
        servidor.createContext("/", this::atender);
        servidor.setExecutor(Executors.newSingleThreadExecutor());
        servidor.start();
        System.out.println("Sistema Restaurante disponível em http://localhost:" + porta);
        if (!"127.0.0.1".equals(host) && !"localhost".equalsIgnoreCase(host)) {
            System.out.println("Para abrir em outro dispositivo da rede, use o IPv4 deste computador e a porta " + porta + ".");
        }
        System.out.println("Pressione Ctrl+C para encerrar.");
    }

    private int obterPorta() {
        String configurada = System.getenv("PORT");
        if (configurada == null || configurada.isBlank()) return 8080;
        try {
            int porta = Integer.parseInt(configurada);
            if (porta < 1 || porta > 65535) throw new NumberFormatException();
            return porta;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("A variável PORT deve conter uma porta entre 1 e 65535.");
        }
    }

    private void atender(HttpExchange troca) throws IOException {
        try {
            String caminho = troca.getRequestURI().getPath();
            String metodo = troca.getRequestMethod();
            if (caminho.startsWith("/api/")) {
                atenderApi(troca, metodo, caminho);
            } else if ("GET".equals(metodo)) {
                servirArquivo(troca, caminho);
            } else {
                responder(troca, 405, "{\"erro\":\"Método não permitido.\"}");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            responder(troca, 400, objetoErro(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            responder(troca, 500, objetoErro("Ocorreu um erro ao processar a solicitação."));
        }
    }

    private void atenderApi(HttpExchange troca, String metodo, String caminho) throws IOException {
        if ("GET".equals(metodo) && "/api/dashboard".equals(caminho)) {
            responder(troca, 200, jsonDashboard());
            return;
        }
        if ("GET".equals(metodo) && "/api/relatorios".equals(caminho)) {
            responder(troca, 200, jsonRelatorio());
            return;
        }
        if ("POST".equals(metodo) && "/api/clientes".equals(caminho)) {
            Map<String, String> dados = lerFormulario(troca);
            restaurante.cadastrarCliente(obrigatorio(dados, "nome"), obrigatorio(dados, "telefone"),
                    obrigatorio(dados, "endereco"), obrigatorio(dados, "email"));
            responder(troca, 201, "{\"mensagem\":\"Cliente cadastrado com sucesso.\"}");
            return;
        }
        if ("POST".equals(metodo) && "/api/produtos".equals(caminho)) {
            Map<String, String> dados = lerFormulario(troca);
            int codigo = inteiro(obrigatorio(dados, "codigo"), "Código");
            double preco = decimal(obrigatorio(dados, "preco"), "Preço");
            String nome = obrigatorio(dados, "nome");
            String descricao = obrigatorio(dados, "descricao");
            Produto produto = "pizza".equalsIgnoreCase(dados.get("tipo"))
                    ? new Pizza(codigo, nome, descricao, preco)
                    : new ProdutoComum(codigo, nome, descricao, preco);
            restaurante.cadastrarProduto(produto);
            responder(troca, 201, "{\"mensagem\":\"Produto cadastrado com sucesso.\"}");
            return;
        }
        if ("POST".equals(metodo) && "/api/pedidos".equals(caminho)) {
            criarPedido(troca, lerFormulario(troca));
            return;
        }
        if ("PATCH".equals(metodo) && caminho.matches("/api/pedidos/\\d+/status")) {
            alterarStatus(troca, caminho);
            return;
        }
        responder(troca, 404, "{\"erro\":\"Rota não encontrada.\"}");
    }

    private void criarPedido(HttpExchange troca, Map<String, String> dados) throws IOException {
        String email = obrigatorio(dados, "clienteEmail");
        Pedido pedido = restaurante.iniciarPedido(email);
        if (pedido == null) throw new IllegalArgumentException("Cliente não encontrado. Cadastre o cliente antes de criar o pedido.");

        int quantidadeItens = inteiro(obrigatorio(dados, "itemCount"), "Quantidade de itens");
        if (quantidadeItens < 1 || quantidadeItens > 20) {
            throw new IllegalArgumentException("O pedido deve conter entre 1 e 20 itens.");
        }
        for (int i = 0; i < quantidadeItens; i++) {
            String prefixo = "item" + i;
            int codigo = inteiro(obrigatorio(dados, prefixo + "Produto"), "Código do produto");
            int quantidade = inteiro(obrigatorio(dados, prefixo + "Quantidade"), "Quantidade");
            if (quantidade < 1) throw new IllegalArgumentException("A quantidade de cada produto deve ser pelo menos 1.");
            Produto produto = restaurante.buscarProduto(codigo);
            if (produto == null) throw new IllegalArgumentException("Produto " + codigo + " não encontrado.");
            if (produto instanceof Pizza) {
                if (marcado(dados, prefixo + "Borda")) produto = new BordaRecheada(produto);
                if (marcado(dados, prefixo + "Queijo")) produto = new QueijoExtra(produto);
                if (marcado(dados, prefixo + "Bacon")) produto = new Bacon(produto);
            }
            pedido.adicionarItem(new ItemPedido(produto, quantidade));
        }

        String descontoInformado = dados.getOrDefault("desconto", "0").trim();
        double desconto = descontoInformado.isEmpty() ? 0 : decimal(descontoInformado, "Desconto");
        if (desconto < 0 || desconto > 100) throw new IllegalArgumentException("O desconto deve estar entre 0 e 100%.");
        pedido.setDescontoPercentual(desconto);

        Pagamento pagamento = FabricaPagamento.criarPagamento(obrigatorio(dados, "pagamento"));
        pedido.setPagamento(pagamento);
        pagamento.processar(pedido.calcularTotal());
        pedido.adicionarObservador(notificacoes::add);
        restaurante.finalizarPedido(pedido);
        responder(troca, 201, jsonPedido(pedido));
    }

    private void alterarStatus(HttpExchange troca, String caminho) throws IOException {
        String[] partes = caminho.split("/");
        int numero = inteiro(partes[3], "Número do pedido");
        Pedido pedido = restaurante.buscarPedido(numero);
        if (pedido == null) throw new IllegalArgumentException("Pedido não encontrado.");
        Map<String, String> dados = lerFormulario(troca);
        StatusPedido status;
        try {
            status = StatusPedido.valueOf(obrigatorio(dados, "status"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status de pedido inválido.");
        }
        restaurante.alterarStatusPedido(numero, status);
        responder(troca, 200, jsonPedido(pedido));
    }

    private String jsonDashboard() {
        StringBuilder json = new StringBuilder("{\"clientes\":[");
        List<br.com.restaurante.model.Cliente> clientes = restaurante.obterClientes();
        for (int i = 0; i < clientes.size(); i++) {
            if (i > 0) json.append(',');
            br.com.restaurante.model.Cliente c = clientes.get(i);
            json.append("{\"nome\":").append(citar(c.getNome()))
                    .append(",\"telefone\":").append(citar(c.getTelefone()))
                    .append(",\"endereco\":").append(citar(c.getEndereco()))
                    .append(",\"email\":").append(citar(c.getEmail())).append('}');
        }
        json.append("],\"produtos\":[");
        List<Produto> produtos = restaurante.obterProdutos();
        for (int i = 0; i < produtos.size(); i++) {
            if (i > 0) json.append(',');
            json.append(jsonProduto(produtos.get(i)));
        }
        json.append("],\"pedidos\":[");
        List<Pedido> pedidos = restaurante.obterPedidos();
        for (int i = 0; i < pedidos.size(); i++) {
            if (i > 0) json.append(',');
            json.append(jsonPedido(pedidos.get(i)));
        }
        json.append("],\"notificacoes\":[");
        int inicio = Math.max(0, notificacoes.size() - 20);
        for (int i = notificacoes.size() - 1; i >= inicio; i--) {
            if (i < notificacoes.size() - 1) json.append(',');
            json.append(citar(notificacoes.get(i)));
        }
        json.append("],\"relatorio\":").append(jsonRelatorio()).append('}');
        return json.toString();
    }

    private String jsonRelatorio() {
        List<Pedido> pedidos = restaurante.obterPedidos();
        double faturamento = 0;
        Map<StatusPedido, Integer> porStatus = new LinkedHashMap<>();
        for (StatusPedido status : StatusPedido.values()) porStatus.put(status, 0);
        for (Pedido pedido : pedidos) {
            faturamento += pedido.calcularTotal();
            porStatus.put(pedido.getStatus(), porStatus.get(pedido.getStatus()) + 1);
        }
        StringBuilder json = new StringBuilder("{\"clientes\":")
                .append(restaurante.obterClientes().size())
                .append(",\"produtos\":").append(restaurante.obterProdutos().size())
                .append(",\"pedidos\":").append(pedidos.size())
                .append(",\"faturamento\":").append(numero(faturamento)).append(",\"porStatus\":{");
        int i = 0;
        for (Map.Entry<StatusPedido, Integer> entrada : porStatus.entrySet()) {
            if (i++ > 0) json.append(',');
            json.append(citar(entrada.getKey().name())).append(':').append(entrada.getValue());
        }
        return json.append("}}").toString();
    }

    private String jsonProduto(Produto produto) {
        return "{\"codigo\":" + produto.getCodigo()
                + ",\"nome\":" + citar(produto.getNome())
                + ",\"descricao\":" + citar(produto.getDescricao())
                + ",\"preco\":" + numero(produto.getPreco())
                + ",\"tipo\":" + citar(produto instanceof Pizza ? "pizza" : "produto") + "}";
    }

    private String jsonPedido(Pedido pedido) {
        StringBuilder json = new StringBuilder("{\"numero\":")
                .append(pedido.getNumeroPedido())
                .append(",\"cliente\":").append(citar(pedido.getCliente().getNome()))
                .append(",\"email\":").append(citar(pedido.getCliente().getEmail()))
                .append(",\"status\":").append(citar(pedido.getStatus().name()))
                .append(",\"statusDescricao\":").append(citar(pedido.getStatus().getDescricao()))
                .append(",\"total\":").append(numero(pedido.calcularTotal()))
                .append(",\"desconto\":").append(numero(pedido.getDescontoPercentual()))
                .append(",\"itens\":[");
        List<ItemPedido> itens = pedido.getItens();
        for (int i = 0; i < itens.size(); i++) {
            if (i > 0) json.append(',');
            ItemPedido item = itens.get(i);
            json.append("{\"nome\":").append(citar(item.getProduto().getNome()))
                    .append(",\"quantidade\":").append(item.getQuantidade())
                    .append(",\"subtotal\":").append(numero(item.getSubtotal())).append('}');
        }
        return json.append("]}").toString();
    }

    private void servirArquivo(HttpExchange troca, String caminho) throws IOException {
        String nome = "/sistema".equals(caminho) || "/sistema/".equals(caminho)
                ? "sistema.html" : ("/".equals(caminho) ? "index.html" : caminho.substring(1));
        Path arquivo = pastaWeb.resolve(nome).normalize();
        if (!arquivo.startsWith(pastaWeb) || !Files.isRegularFile(arquivo)) {
            responder(troca, 404, "Página não encontrada.");
            return;
        }
        String tipo = nome.endsWith(".css") ? "text/css; charset=utf-8"
                : nome.endsWith(".js") ? "text/javascript; charset=utf-8"
                : nome.endsWith(".svg") ? "image/svg+xml"
                : "text/html; charset=utf-8";
        byte[] conteudo = Files.readAllBytes(arquivo);
        troca.getResponseHeaders().set("Content-Type", tipo);
        troca.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        troca.sendResponseHeaders(200, conteudo.length);
        troca.getResponseBody().write(conteudo);
        troca.close();
    }

    private Map<String, String> lerFormulario(HttpExchange troca) throws IOException {
        String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> resultado = new LinkedHashMap<>();
        if (corpo.isBlank()) return resultado;
        for (String par : corpo.split("&")) {
            String[] partes = par.split("=", 2);
            String chave = decodificar(partes[0]);
            String valor = partes.length > 1 ? decodificar(partes[1]) : "";
            resultado.put(chave, valor);
        }
        return resultado;
    }

    private String decodificar(String texto) {
        return URLDecoder.decode(texto, StandardCharsets.UTF_8);
    }

    private String obrigatorio(Map<String, String> dados, String chave) {
        String valor = dados.get(chave);
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Preencha o campo " + chave + ".");
        return valor.trim();
    }

    private int inteiro(String valor, String campo) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " inválido.");
        }
    }

    private double decimal(String valor, String campo) {
        try {
            double resultado = Double.parseDouble(valor.trim().replace(',', '.'));
            if (!Double.isFinite(resultado)) throw new NumberFormatException();
            return resultado;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " inválido.");
        }
    }

    private boolean marcado(Map<String, String> dados, String chave) {
        String valor = dados.get(chave);
        return valor != null && (valor.equalsIgnoreCase("true") || valor.equalsIgnoreCase("on") || valor.equals("1"));
    }

    private String objetoErro(String mensagem) {
        return "{\"erro\":" + citar(mensagem == null ? "Não foi possível concluir a operação." : mensagem) + "}";
    }

    private String citar(String valor) {
        StringBuilder resultado = new StringBuilder("\"");
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"': resultado.append("\\\""); break;
                case '\\': resultado.append("\\\\"); break;
                case '\b': resultado.append("\\b"); break;
                case '\f': resultado.append("\\f"); break;
                case '\n': resultado.append("\\n"); break;
                case '\r': resultado.append("\\r"); break;
                case '\t': resultado.append("\\t"); break;
                default:
                    if (c < 0x20) resultado.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else resultado.append(c);
            }
        }
        return resultado.append('"').toString();
    }

    private String numero(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    private void responder(HttpExchange troca, int status, String resposta) throws IOException {
        byte[] conteudo = resposta.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        troca.getResponseHeaders().set("Cache-Control", "no-store");
        troca.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        troca.sendResponseHeaders(status, conteudo.length);
        troca.getResponseBody().write(conteudo);
        troca.close();
    }
}
