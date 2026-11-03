# Frontend Pede Fruki

Interface estática HTML/CSS/JavaScript com três áreas: visão geral do vendedor, protótipo do app do vendedor (Atendimento PDV / Mix do PDV) e conversa Pede Fruki/WhatsApp simulada.

## Executar

Sirva a pasta em servidor estático local, por exemplo usando a extensão Live Server do VS Code na porta `5500`. A API padrão é `http://localhost:8080`; também pode alterá-la no botão **Configurar API**.

## Integrações

- A visão geral consome a API Java para recomendações.
- O app do vendedor usa `/api/app-vendedor/clientes`, `/api/app-vendedor/clientes/{id}/recomendacoes` e `/api/app-vendedor/clientes/{id}/pedidos-recentes`.
- O canal Pede Fruki usa sessão simulada por CNPJ/ID e só permite checkout após identificar um cliente real.
- Ao finalizar pedido, o frontend envia apenas produtos e quantidades; o backend escolhe o preço, grava pedido/itens e cria notificação.
- Quando configurado, o backend solicita um e-mail ao Resend após confirmar o pedido no banco. A conversa mostrada no navegador continua sendo uma simulação visual, não uma sessão real do WhatsApp. O Resend não envia WhatsApp ou push ao app do vendedor.

Os dados fictícios só aparecem quando a API estiver indisponível e são identificados no modo de demonstração.
