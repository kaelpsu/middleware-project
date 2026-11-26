# Roteiro de Apresentação — Middleware Distribuído em Java
---

## 1. Visão Geral e Arquitetura

O projeto é dividido em dois módulos Maven desacoplados:

- **`middleware-core`**: biblioteca com toda a infraestrutura de comunicação remota, despacho reflexivo, serialização, registro de serviços, balanceamento, ciclo de vida e interceptação.
- **`app-demo`**: aplicação de demonstração composta por um **API Gateway** (`GatewayService`) e dois microsserviços de negócio (**`OrderService`** e **`TaxService`**) iniciados a partir da mesma base de código via argumentos CLI (`MainApp`).


### Flags CLI suportadas pela `MainApp`

| Flag | Padrão | Descrição |
| :--- | :--- | :--- |
| `--node` | `gateway` | Papel do nó: `gateway`, `order` ou `tax` |
| `--host` | `127.0.0.1` | IP anunciado pelo nó backend nos heartbeats |
| `--tcp-port` | `8080` | Porta TCP do nó backend |
| `--udp-port` | `9090` | Porta UDP do nó backend |
| `--gateway-host` | `127.0.0.1` | Host do Gateway para registro e chamadas internas |
| `--gateway-tcp-port` | `8080` | Porta TCP do Gateway |
| `--gateway-udp-port` | `9090` | Porta UDP do Gateway (recepção de heartbeats) |

---

## Demonstração

```bash
curl -s -X POST http://3.137.106.188:8080/gateway/order \
  -H "Content-Type: application/json" \
  -d '{"item":"Notebook","quantity":2,"unitPrice":3500.0,"stateCode":"SP"}'
```

