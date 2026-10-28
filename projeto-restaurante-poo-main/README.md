# Mesa Viva — gestão do restaurante

O Mesa Viva é o sistema Java de gerenciamento deste projeto, com uma apresentação web antes do painel de operação. O painel usa os mesmos serviços e modelos do sistema original para clientes, cardápio, pedidos, pagamentos, relatórios e notificações de mudança de status.

O projeto não usa Node/npm, então não há `package.json` nem `npm run dev`; o servidor HTTP e a interface são executados com o JDK existente.

## Requisitos

- JDK 11 ou superior (`java` e `javac` disponíveis no `PATH`)
- PowerShell no Windows

## Executar localmente

Na pasta deste projeto, rode:

```powershell
.\executar.ps1
```

Se estiver na pasta externa `projeto-restaurante-poo-main`, o atalho `executar.ps1` também inicia o projeto Java que está dentro dela.

O script compila as classes Java em `out` e inicia o servidor local. Abra [http://localhost:8080](http://localhost:8080) para ver a landing page e use **Acessar sistema** para entrar no painel. Para escolher outra porta, defina a variável de ambiente `PORT` antes de executar o script.

As páginas se adaptam a telas de computador e celular. Para acessar pelo celular na mesma rede Wi-Fi, inicie o servidor com `HOST=0.0.0.0` (no PowerShell: `$env:HOST = '0.0.0.0'`) e use no telefone o IPv4 do computador com a mesma porta. Sem essa opção, o servidor aceita conexões apenas do próprio computador.

O sistema também pode ser iniciado no console existente, após a compilação, com `java -cp out br.com.restaurante.main.Principal --console`.

## Organização

- `src/br/com/restaurante`: modelos, repositórios, serviços e padrões Factory Method, Decorator e Observer existentes, além do servidor HTTP Java.
- `web/index.html`: landing page.
- `web/sistema.html`: painel de operação.
- `web/site.css`, `web/system.css` e `web/app.js`: estilos responsivos e interações do navegador.
- `executar.ps1`: compilação e inicialização local.

Os dados são mantidos em memória, como na implementação original, e são reiniciados ao iniciar o processo.
