const demoCustomers = {
  "1001": { id: 1001, name: "Mercado Bom Vizinho", profile: "Mercado de bairro", city: "Lajeado, RS" },
  "1002": { id: 1002, name: "Mini Mercado Central", profile: "Minimercado", city: "Estrela, RS" },
  "1003": { id: 1003, name: "Supermercado Estação", profile: "Supermercado", city: "Venâncio Aires, RS" }
};

const demoRecommendations = {
  "1001": [
    { id: 201, name: "Fruki Guaraná 2L", category: "Refrigerante", score: 94, reason: "Boa aderência ao perfil de mercado de bairro e à categoria de maior giro.", factor: "Perfil da loja", emoji: "🥤", price: "Preço ilustrativo" },
    { id: 204, name: "Água Mineral 1,5L", category: "Água", score: 87, reason: "Produto complementar que pode ampliar as opções disponíveis ao consumidor.", factor: "Diversificação", emoji: "💧", price: "Preço ilustrativo" },
    { id: 202, name: "Fruki Laranja 2L", category: "Refrigerante", score: 81, reason: "Alternativa de sabor para diversificar a oferta dentro da categoria.", factor: "Portfólio", emoji: "🍊", price: "Preço ilustrativo" },
    { id: 203, name: "Fruki Limão 2L", category: "Refrigerante", score: 76, reason: "Sugestão complementar para testar variedade no próximo pedido.", factor: "Diversificação", emoji: "🍋", price: "Preço ilustrativo" }
  ],
  "1002": [
    { id: 204, name: "Água Mineral 1,5L", category: "Água", score: 92, reason: "Pode complementar a variedade de bebidas de compra recorrente.", factor: "Perfil da loja", emoji: "💧", price: "Preço ilustrativo" },
    { id: 202, name: "Fruki Laranja 2L", category: "Refrigerante", score: 86, reason: "Amplia as opções de sabor sem exigir uma mudança grande no sortimento.", factor: "Diversificação", emoji: "🍊", price: "Preço ilustrativo" },
    { id: 201, name: "Fruki Guaraná 2L", category: "Refrigerante", score: 82, reason: "Item de categoria relevante para o perfil de minimercado.", factor: "Perfil da loja", emoji: "🥤", price: "Preço ilustrativo" },
    { id: 203, name: "Fruki Limão 2L", category: "Refrigerante", score: 71, reason: "Pode ajudar a testar uma opção adicional no portfólio.", factor: "Portfólio", emoji: "🍋", price: "Preço ilustrativo" }
  ],
  "1003": [
    { id: 202, name: "Fruki Laranja 2L", category: "Refrigerante", score: 95, reason: "Boa oportunidade de ampliar a variedade em uma operação com sortimento maior.", factor: "Portfólio", emoji: "🍊", price: "Preço ilustrativo" },
    { id: 203, name: "Fruki Limão 2L", category: "Refrigerante", score: 89, reason: "Alternativa para complementar a categoria com variedade de sabores.", factor: "Diversificação", emoji: "🍋", price: "Preço ilustrativo" },
    { id: 204, name: "Água Mineral 1,5L", category: "Água", score: 85, reason: "Categoria complementar que pode atender diferentes ocasiões de consumo.", factor: "Perfil da loja", emoji: "💧", price: "Preço ilustrativo" },
    { id: 201, name: "Fruki Guaraná 2L", category: "Refrigerante", score: 79, reason: "Ajuda a manter uma opção conhecida no conjunto de recomendações.", factor: "Histórico simulado", emoji: "🥤", price: "Preço ilustrativo" }
  ]
};

let apiBase = localStorage.getItem("frukiApiBase") || "http://localhost:8080";
let currentCustomerId = sessionStorage.getItem("frukiCustomerId") || "1001";
let currentRecommendations = demoRecommendations[currentCustomerId] || demoRecommendations["1001"];
let apiConnected = false;
let catalogIsFromApi = false;
let sessionToken = sessionStorage.getItem("frukiSimToken") || null;
let identifiedCustomer = null;
try { identifiedCustomer = JSON.parse(sessionStorage.getItem("frukiIdentifiedCustomer") || "null"); } catch { identifiedCustomer = null; }
if (sessionToken && identifiedCustomer) currentRecommendations = [];
let toastTimeout;
let sellerAppClients = [];
let sellerCustomerDetails = {};
let sellerAppCustomerId = currentCustomerId;
let sellerAppRecommendations = [];
let sellerAppScreen = "home";

const $ = (id) => document.getElementById(id);

function showToast(message) {
  const toast = $("toast");
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => toast.classList.remove("show"), 3000);
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, char => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[char]));
}

function setTab(tab) {
  const seller = tab === "seller";
  const sellerApp = tab === "seller-app";
  $("seller-view").classList.toggle("hidden", !seller);
  $("seller-app-view").classList.toggle("hidden", !sellerApp);
  $("whatsapp-view").classList.toggle("hidden", tab !== "whatsapp");
  document.querySelectorAll(".nav-item").forEach(button => button.classList.toggle("active", button.dataset.tab === tab));
  if (seller) $("page-title").textContent = "Visão do vendedor";
  else if (sellerApp) {
    $("page-title").textContent = "App do vendedor";
    loadSellerAppData();
  } else $("page-title").textContent = "Pede Fruki / WhatsApp";
}

function selectedCustomer() {
  if (identifiedCustomer && String(identifiedCustomer.idCliente) === String(currentCustomerId)) {
    const profileParts = [identifiedCustomer.segmento, identifiedCustomer.subsegmento].filter(Boolean);
    return {
      id: identifiedCustomer.idCliente,
      name: identifiedCustomer.nome,
      profile: profileParts.join(" · ") || "Perfil não informado",
      city: [identifiedCustomer.cidade, identifiedCustomer.uf].filter(Boolean).join(", ") || "Cidade não informada"
    };
  }
  const sellerCustomer = sellerAppClients.find(client => String(client.idCliente ?? client.id) === String(currentCustomerId));
  if (sellerCustomer) {
    const profileParts = [sellerCustomer.segmento ?? sellerCustomer.profile, sellerCustomer.subsegmento].filter(Boolean);
    return {
      id: sellerCustomer.idCliente ?? sellerCustomer.id,
      name: sellerCustomer.nome ?? sellerCustomer.name ?? "Cliente selecionado",
      profile: profileParts.join(" · ") || "Perfil não informado",
      city: [sellerCustomer.cidade, sellerCustomer.uf].filter(Boolean).join(", ") || "Cidade não informada"
    };
  }
  return demoCustomers[currentCustomerId] || demoCustomers["1001"];
}

function setConnectionStatus(connected, message) {
  apiConnected = connected;
  const banner = $("connection-banner");
  const statusText = banner?.querySelector("span:last-of-type");
  const pill = document.querySelector(".topbar-right .demo-pill");
  const sidebarTitle = document.querySelector(".sidebar-bottom strong");
  const sidebarDetail = document.querySelector(".sidebar-bottom small");

  if (connected) {
    banner?.classList.add("hidden");
    if (pill) pill.innerHTML = "<span></span> API conectada";
    if (sidebarTitle) sidebarTitle.textContent = "API conectada";
    if (sidebarDetail) sidebarDetail.textContent = "Dados reais do Supabase";
    return;
  }

  banner?.classList.remove("hidden");
  if (statusText) statusText.innerHTML = message;
  if (pill) pill.innerHTML = "<span></span> Modo demonstração";
  if (sidebarTitle) sidebarTitle.textContent = "Modo demonstração";
  if (sidebarDetail) sidebarDetail.textContent = "Confira a conexão com a API";
}

function renderDashboardCustomers(rows) {
  const select = $("customer-select");
  if (!select || !Array.isArray(rows) || rows.length === 0) return;

  select.innerHTML = rows.map(client => {
    const id = client.idCliente ?? client.id;
    const name = client.nome ?? client.name ?? `Cliente ${id}`;
    return `<option value="${escapeHtml(id)}">${escapeHtml(name)} · #${escapeHtml(id)}</option>`;
  }).join("");

  if (identifiedCustomer && rows.some(client => String(client.idCliente ?? client.id) === String(identifiedCustomer.idCliente))) {
    currentCustomerId = String(identifiedCustomer.idCliente);
  } else if (!rows.some(client => String(client.idCliente ?? client.id) === String(currentCustomerId))) {
    currentCustomerId = String(rows[0].idCliente ?? rows[0].id);
  }
  select.value = String(currentCustomerId);
  renderCustomer();
}

async function loadSellerCustomerDetails(customerId) {
    const requestedCustomerId = String(customerId);

    // Verifica se os detalhes já foram carregados.
    if (sellerCustomerDetails[requestedCustomerId]) {
        return sellerCustomerDetails[requestedCustomerId];
    }

    const url = `${apiBase.replace(/\/+$/, "")}/api/app-vendedor/clientes/${encodeURIComponent(requestedCustomerId)}`;

    const response = await fetch(url, {
        headers: {
            "Accept": "application/json"
        }
    });

    if (!response.ok) {
        throw new Error(`Erro ao buscar cliente: HTTP ${response.status}`);
    }

    const customerDetails = await response.json();

    // Guarda os detalhes recebidos.
    sellerCustomerDetails[requestedCustomerId] = customerDetails;

    // Atualiza o cliente na lista já carregada.
    const clientIndex = sellerAppClients.findIndex(
        client => String(client.idCliente ?? client.id) === requestedCustomerId
    );

    if (clientIndex !== -1) {
        sellerAppClients[clientIndex] = {
            ...sellerAppClients[clientIndex],
            ...customerDetails
        };
    }

    // Atualiza a tela somente se esse ainda for o cliente selecionado.
    if (String(currentCustomerId) === requestedCustomerId) {
        renderCustomer();
    }

    return customerDetails;
}

async function loadSellerDashboardRecommendations(customerId) {
  const requestedCustomerId = String(customerId);
  const url = `${apiBase.replace(/\/+$/, "")}/api/app-vendedor/clientes/${encodeURIComponent(requestedCustomerId)}/recomendacoes`;
  $("api-status").textContent = `Consultando ${url} ...`;

  try {
    const response = await fetch(url, { headers: { "Accept": "application/json" } });
    if (!response.ok) throw new Error(`API respondeu HTTP ${response.status}`);
    const payload = await response.json();
    if (!Array.isArray(payload)) throw new Error("Formato de recomendações inesperado.");
    if (String(currentCustomerId) !== requestedCustomerId) return;

    currentRecommendations = mapApiRecommendations(payload);
    refreshCatalogProductsForCurrentRecommendations();
    setConnectionStatus(true);
    $("api-status").textContent = `Conectado com sucesso. ${payload.length} recomendação(ões) recebida(s).`;
    renderRecommendations();
  } catch (error) {
    if (String(currentCustomerId) !== requestedCustomerId) return;
    const demoRows = demoRecommendations[requestedCustomerId];
    currentRecommendations = demoRows || [];
    renderRecommendations();

    if (demoRows) {
      setConnectionStatus(false, `<strong>Modo demonstração ativo.</strong> Não foi possível acessar a API; exibindo dados fictícios.`);
    } else {
      setConnectionStatus(false, `<strong>API indisponível.</strong> ${escapeHtml(error.message)}. As recomendações reais não puderam ser carregadas.`);
    }
    $("api-status").textContent = `Não foi possível conectar: ${error.message}. Confira backend, CORS e Supabase.`;
  }
}

function renderRecommendations() {
  const recommendations = currentRecommendations || [];
  $("recommendation-list").innerHTML = recommendations.map((item, index) => `
    <article class="recommendation-card">
      <div class="rank">${String(index + 1).padStart(2, "0")}</div>
      <div class="recommendation-main">
        ${productVisualMarkup(item, "recommendation-photo")}
        <div>
          <div class="product-title-row">
            <span class="product-title">${escapeHtml(item.name)}</span>
            <span class="category-tag">${escapeHtml(item.category || "Produto")}</span>
          </div>
          <p class="product-reason">${escapeHtml(item.reason || "Recomendação calculada pelo motor de recomendação.")}</p>
          <div class="product-sub">Fator em destaque: ${escapeHtml(item.factor || "Score combinado")} · ${escapeHtml(item.price || "Consulte a tabela de preços")}</div>
        </div>
      </div>
      <div class="score-wrap">
        <div class="score">${Math.round(Number(item.score ?? item.pontuacao ?? 0))}<small>/100</small></div>
        <div class="score-bar"><span style="width:${Math.max(0, Math.min(100, Number(item.score ?? item.pontuacao ?? 0)))}%"></span></div>
      </div>
    </article>
  `).join("") || `<div class="panel" style="padding:22px;color:#718078">Nenhuma recomendação retornada para este cliente.</div>`;
  bindProductImages($("recommendation-list"));

  $("metric-products").textContent = recommendations.length;
  const newCount = Math.max(0, recommendations.filter(item => /diversifica|portfólio|complement/i.test((item.factor || "") + " " + (item.reason || ""))).length);
  $("metric-new").innerHTML = `${newCount} <small>produtos</small>`;
  $("metric-signal").textContent = recommendations[0]?.factor || "Score combinado";
  renderChatProducts();
}

function renderCustomer() {
  const customer = selectedCustomer();
  $("customer-profile").textContent = customer.profile || customer.tipoEstabelecimento || "Perfil não informado";
  $("customer-city").textContent = customer.city || customer.cidade || "Cidade não informada";
  $("chat-customer-name").textContent = customer.name || customer.nome || "Cliente não identificado";
  const select = $("customer-select");
  if (select && !Array.from(select.options).some(option => option.value === String(currentCustomerId))) {
    const option = document.createElement("option");
    option.value = String(currentCustomerId);
    option.textContent = `Cliente identificado: ${customer.name || currentCustomerId}`;
    select.appendChild(option);
  }
  if (select && identifiedCustomer && String(identifiedCustomer.idCliente) === String(currentCustomerId)) {
    select.value = String(currentCustomerId);
  }
}

function emojiFor(name = "") {
  const text = name.toLowerCase();
  if (text.includes("água") || text.includes("agua")) return "💧";
  if (text.includes("elev") || text.includes("energ")) return "⚡";
  if (text.includes("laranja")) return "🍊";
  if (text.includes("limão") || text.includes("limao") || text.includes("frukito")) return "🍋";
  return "🥤";
}

function normalizeCatalogText(value) {
  return String(value ?? "").normalize("NFD").replace(/[\u0300-\u036f]/g, "")
    .toLowerCase().replace(/[^a-z0-9]+/g, " ").trim().replace(/\s+/g, " ");
}

function catalogPresentationKey(product) {
  const basePrice = Number.isFinite(Number(product.precoBase)) ? Number(product.precoBase).toFixed(2) : "";
  const minimumPrice = Number.isFinite(Number(product.precoMinimo)) ? Number(product.precoMinimo).toFixed(2) : "";
  return [normalizeCatalogText(product.name), normalizeCatalogText(product.category), basePrice, minimumPrice].join("|");
}

function uniqueCatalogProducts(products) {
  const recommendedIds = new Set((currentRecommendations || []).map(item => String(item.id ?? item.produtoId ?? "")));
  const groups = new Map();
  products.forEach(product => {
    const key = catalogPresentationKey(product);
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(product);
  });

  return Array.from(groups.values(), group => group.slice().sort((first, second) => {
    const firstRecommended = recommendedIds.has(String(first.id)) ? 0 : 1;
    const secondRecommended = recommendedIds.has(String(second.id)) ? 0 : 1;
    if (firstRecommended !== secondRecommended) return firstRecommended - secondRecommended;
    return Number(first.id) - Number(second.id);
  })[0]);
}

function refreshCatalogProductsForCurrentRecommendations() {
  if (catalogIsFromApi && catalogProductRows.length) {
    catalogProducts = uniqueCatalogProducts(catalogProductRows);
  }
}

function productVisualMarkup(item, className = "product-visual") {
  const imageUrl = item.imageUrl ?? item.imagemUrl ?? item.fotoUrl ?? item.urlImagem;
  const safeImageUrl = typeof imageUrl === "string" && /^(https?:\/\/|\/)/i.test(imageUrl.trim())
    ? imageUrl.trim()
    : "";
  const productName = item.name || item.nomeProduto || "";
  const normalizedName = normalizeCatalogText(productName);
  const tone = normalizedName.includes("agua") ? "tone-water"
    : normalizedName.includes("elev") || normalizedName.includes("energy") ? "tone-energy"
    : normalizedName.includes("frukito") || normalizedName.includes("suco") || normalizedName.includes("com-tem") ? "tone-juice"
    : normalizedName.includes("citrus") || normalizedName.includes("laranja") || normalizedName.includes("limao") ? "tone-citrus"
    : "tone-fruki";
  const emoji = emojiFor(productName);
  const visual = safeImageUrl
    ? `<img data-product-image src="${escapeHtml(safeImageUrl)}" alt="${escapeHtml(productName || "Produto Fruki")}" loading="lazy"><span class="product-visual-fallback" hidden>${emoji}</span>`
    : `<span>${emoji}</span>`;
  return `<div class="${className} ${tone}${safeImageUrl ? " has-image" : ""}">${visual}</div>`;
}

function bindProductImages(container) {
  if (!container) return;
  container.querySelectorAll("[data-product-image]").forEach(image => {
    const showFallback = () => {
      image.hidden = true;
      if (image.nextElementSibling) image.nextElementSibling.hidden = false;
    };
    image.addEventListener("error", showFallback, { once: true });
    if (image.complete && !image.naturalWidth) showFallback();
  });
}

function recommendationBundleAmounts(count) {
  if (count >= 3) return [50, 30, 20];
  if (count === 2) return [60, 40];
  if (count === 1) return [100];
  return [];
}

function renderChatProducts() {
  const recommendations = (currentRecommendations || []).slice(0, 3);
  const amounts = recommendationBundleAmounts(recommendations.length);
  const products = recommendations.map((item, index) => ({
    name: item.name || item.nomeProduto || "Produto recomendado",
    note: item.reason || item.motivo || "Recomendado para este perfil",
    amount: amounts[index] || 0,
    tag: item.factor || item.fatorPrincipal || "Recomendação",
    emoji: emojiFor(item.name || item.nomeProduto),
    imageUrl: item.imageUrl ?? item.imagemUrl ?? item.fotoUrl ?? item.urlImagem ?? portfolioImageForProduct(item.name || item.nomeProduto)
  }));
  const target = $("weekly-products");
  if (target) target.innerHTML = products.map(item => `
    <div class="weekly-product">${productVisualMarkup(item)}<div><strong>${escapeHtml(item.name)}</strong><small>${escapeHtml(item.note)} · ${item.amount} fardos</small></div><span class="product-reason-tag">${escapeHtml(item.tag)}</span></div>
  `).join("") || `<p>Identifique o cliente e conecte o backend para ver sugestões reais.</p>`;
  bindProductImages(target);
  const hiddenTarget = $("chat-products");
  if (hiddenTarget) hiddenTarget.innerHTML = "";
}

function portfolioImageForProduct(name = "") {
  const normalized = normalizeCatalogText(name);
  const hasVolume = volume => normalized.includes(volume);
  const has500Ml = hasVolume("500ml") || hasVolume("500 ml");
  const has350Ml = hasVolume("350ml") || hasVolume("350 ml");
  const has2L = hasVolume("2l");

  if (normalized.includes("agua da pedra") && normalized.includes("sem gas")) {
    if (hasVolume("20l")) return "https://fruki.com.br/static/40d0e73872ac3aa328f5a79467a183d5/f537b/fruki_pet_2l_regular_frontal.webp";
    if (hasVolume("1 5l") || hasVolume("1 5 l")) return "https://fruki.com.br/static/ff27adbda81184149dcc16ddebe7194c/a785b/fruki_pet_1-5l_regular_frontal.webp";
    if (hasVolume("5l")) return "https://fruki.com.br/static/b101b555aaf30dfaa73fdce19069e0ed/a785b/fruki-5l-agua-s-gas-gotas.webp";
    if (has500Ml) return "https://fruki.com.br/static/c4b5fd7ee7d5f90d32e4b858153506c5/f537b/fruki-500ml-agua-s-gas-gotas.webp";
  }
  if (normalized.includes("agua da pedra") && normalized.includes("com gas")) {
    if (has500Ml) return "https://fruki.com.br/static/27bf5daa2a9731ece513fc07904bd200/f537b/fruki-500ml-agua-c-gas-gotas.webp";
    if (has350Ml) return "https://fruki.com.br/static/6d1641ce86d4fc4011b17d4ac58329d9/f537b/fruki-350ml-agua-c-gas-gotas.webp";
  }
  if (normalized.includes("elev")) {
    if (normalized.includes("zero") && has2L) return "https://fruki.com.br/static/60c87b70f0e41d1172fd7def3add6cea/a785b/elev_zero-a%C3%A7%C3%BAcar.webp";
    if (normalized.includes("original") && has2L) return "https://fruki.com.br/static/8ca2e728d0bf453a09375e820bf0397a/a785b/elev_sabor-original.webp";
    if (normalized.includes("maca verde") && hasVolume("473")) return "https://fruki.com.br/static/8b96568d93d1ff375803067d708a3db3/c361c/eleve_473_maca_verde.webp";
    if (normalized.includes("maca verde") && has2L) return "https://fruki.com.br/static/ff129726e8a72dede934727b19e479d6/a785b/pet_maca_verde_frontal.webp";
    if (normalized.includes("original") && hasVolume("473")) return "https://fruki.com.br/static/b92955462f4378923141512db531854c/c361c/eleve_473_original.webp";
    if (normalized.includes("zero") && hasVolume("473")) return "https://fruki.com.br/static/0a6968df7ec5b1403c5c7c5f28e5f638/c361c/eleve_473_zero_ac%CC%A7ucar.webp";
    if (normalized.includes("frutas tropicais") && has2L) return "https://fruki.com.br/static/0f8443b72a3dcf40313a8fad72786f58/a785b/elev_frutas-tropicais.webp";
    if (normalized.includes("morango e melancia") && has2L) return "https://fruki.com.br/static/3cfc7e4bbedb1b51d46354413bbaf727/a785b/elev_pet_morango_melancia.webp";
    if (normalized.includes("guarana e maracuja") && has2L) return "https://fruki.com.br/static/afe8ce7b1d000cbae8698035d18fb842/a785b/pet_guarana_frontal.webp";
    if (normalized.includes("coco") && normalized.includes("acai") && has2L) return "https://fruki.com.br/static/68ef8d584fc9dd7cbca42694c5cf8816/a785b/fruki_elev_coco_acai_2l.webp";
  }
  if (normalized.includes("bella vista") && normalized.includes("ipa") && normalized.includes("lata")) return "https://fruki.com.br/static/bc608b6936580e38a552d0f64356bb93/f537b/bellavista-ipa-350.webp";
  if (normalized.includes("bella vista") && normalized.includes("premium lager") && normalized.includes("long neck")) return "https://fruki.com.br/static/fc06d0a0e6de5c2ab0580019534063a2/a785b/frontais_suadas_long_neck_f01.webp";
  if (normalized.includes("fruki citrus") && has350Ml) return "https://fruki.com.br/static/d5be827bf87fe6d2d57b53565979eea5/ce8be/citrus_350ml.png2.webp";
  if (normalized.includes("fruki cola")) {
    if (has2L) return "https://fruki.com.br/static/724e5a7fadc809365cee654146d2ad9e/f537b/cola-2l.webp";
    if (has350Ml) return "https://fruki.com.br/static/f5db864f1dd77fb888b5db1d7483ccc7/f537b/cola_lata350_wet.webp";
    if (hasVolume("600ml") || hasVolume("600 ml")) return "https://fruki.com.br/static/ee97a2a0a23c9e25112c22236edffe60/f537b/cola-600.webp";
  }
  if (normalized.includes("fruki uva") && has2L) return "https://fruki.com.br/static/e9c99adf5d0a26fbecbd33c61ad54148/beecb/mockup-2l-uva-final.webp";
  if (normalized.includes("fruki guarana") && (hasVolume("600ml") || hasVolume("600 ml")) && !normalized.includes("zero")) return "https://fruki.com.br/static/fe3966b9b9fe49a66a68e89045b7b94c/f537b/fruki_pet_600ml__frontal.webp";
  if (normalized.includes("laranjinha") && has2L) return "https://fruki.com.br/static/10b10561f32ed9e6cd68ddae8959d7a5/f537b/laranjinha-2l-sem-_novo.webp";
  if (normalized.includes("frukito") && normalized.includes("citricas") && has500Ml) return "https://fruki.com.br/static/d179ee653572fdca4adc376d73f74336/a785b/alta_frukito_frutas_citricas_500ml_gotas.webp";
  if (normalized.includes("suco com tem") && normalized.includes("laranja") && hasVolume("1l") && !normalized.includes("vidro")) return "https://fruki.com.br/static/f165e5daf0fa2e8b9dd0b100600b98c3/a785b/_suco_tetrapack_1l_frente_laranja.webp";
  if (normalized.includes("suco com tem") && normalized.includes("uva") && hasVolume("1l") && !normalized.includes("vidro")) return "https://fruki.com.br/static/51ec67c153be6c18fafb67e403c1c9e1/a785b/_suco_tetrapack_1l_frente_uva.webp";
  return null;
}

function dedupeRecommendationsByProduct(recommendations) {
  const seenProducts = new Set();
  return recommendations.filter(item => {
    const productKey = normalizeCatalogText(item.name || item.nomeProduto || "") || `id:${item.id ?? item.produtoId ?? ""}`;
    if (seenProducts.has(productKey)) return false;
    seenProducts.add(productKey);
    return true;
  });
}

function mapApiRecommendations(rows) {
  const mapped = rows.map(item => ({
    id: item.produtoId ?? item.id ?? item.idProduto,
    name: item.nomeProduto ?? item.nome ?? item.name ?? "Produto sem nome",
    category: item.categoria ?? item.categoriaProduto ?? "Produto",
    score: item.score ?? item.pontuacao ?? 0,
    reason: item.motivo ?? item.justificativa ?? item.reason ?? "Recomendação retornada pela API.",
    factor: item.fatorPrincipal ?? item.fator ?? "Score combinado",
    price: item.preco != null ? Number(item.preco).toLocaleString("pt-BR", { style: "currency", currency: "BRL" }) : "Preço conforme cadastro",
    imageUrl: item.imageUrl ?? item.imagemUrl ?? item.fotoUrl ?? item.urlImagem ?? portfolioImageForProduct(item.nomeProduto ?? item.nome ?? item.name),
    rawPrice: item.preco
  }));
  return dedupeRecommendationsByProduct(mapped);
}

function sellerAppCustomer() {
  const found = sellerAppClients.find(client => String(client.idCliente ?? client.id) === String(sellerAppCustomerId));
  if (found) {
    const nome = found.nome ?? found.name ?? "Cliente selecionado";
    const segmento = found.segmento ?? found.profile ?? "Segmento não informado";
    const subsegmento = found.subsegmento ?? "";
    return { id: found.idCliente ?? found.id, name: nome, profile: [segmento, subsegmento].filter(Boolean).join(" · "), segment: segmento };
  }
  const demo = demoCustomers[sellerAppCustomerId] || demoCustomers["1001"];
  return { id: demo.id, name: demo.name, profile: demo.profile, segment: demo.profile };
}

function renderSellerApp() {
  const customer = sellerAppCustomer();
  if ($("seller-app-customer-name")) $("seller-app-customer-name").textContent = customer.name || "Cliente não selecionado";
  if ($("seller-app-customer-profile")) $("seller-app-customer-profile").textContent = customer.profile || "Perfil não informado";
  if ($("seller-app-segment")) $("seller-app-segment").textContent = customer.segment || "Não informado";
  if ($("seller-app-customer-id")) $("seller-app-customer-id").textContent = customer.id ?? "—";
  const select = $("seller-app-client-select");
  if (select && select.value !== String(sellerAppCustomerId)) select.value = String(sellerAppCustomerId);

  const list = $("seller-app-mix-list");
  if (list) {
    const rows = sellerAppRecommendations || [];
    list.innerHTML = rows.map((item, index) => `
      <article class="seller-mix-item">
        <span class="seller-mix-rank">${String(index + 1).padStart(2, "0")}</span>
        <div class="seller-mix-product">${productVisualMarkup(item, "seller-mix-photo")}<div class="seller-mix-copy"><strong>${escapeHtml(item.name || item.nomeProduto || "Produto")}</strong><small>${escapeHtml(item.category || item.categoria || "Produto")} · ${escapeHtml(item.factor || item.fatorPrincipal || "Score combinado")}</small><p>${escapeHtml(item.reason || item.motivo || "Recomendado para este perfil.")}</p></div></div>
        <div class="seller-mix-score"><strong>${Math.round(Number(item.score ?? 0))}</strong><small>/100</small><span>${escapeHtml(item.price || "Preço conforme cadastro")}</span></div>
      </article>`).join("") || `<p class="seller-app-empty">Não há recomendações disponíveis. Confira se a API e o banco estão conectados.</p>`;
    bindProductImages(list);
    $("seller-app-mix-subtitle").textContent = `${rows.length} produto(s) priorizado(s) para ${customer.name}.`;
  }
  const profileScreen = $("seller-app-home");
  if (profileScreen) profileScreen.classList.toggle("hidden", sellerAppScreen !== "home");
  if ($("seller-app-mix-screen")) $("seller-app-mix-screen").classList.toggle("hidden", sellerAppScreen !== "mix");
  if ($("seller-app-notifications-screen")) $("seller-app-notifications-screen").classList.toggle("hidden", sellerAppScreen !== "notifications");
}

async function loadSellerAppClients() {
  const select = $("seller-app-client-select");
  if (!select) return;
  try {
    const response = await fetch(`${apiBase.replace(/\/+$/, "")}/api/app-vendedor/clientes`, { headers: { "Accept": "application/json" } });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const rows = await response.json();
    if (!Array.isArray(rows) || rows.length === 0) throw new Error("A API não retornou clientes.");
    sellerAppClients = rows;
    select.innerHTML = rows.map(client => `<option value="${escapeHtml(client.idCliente)}">${escapeHtml(client.nome)} · #${escapeHtml(client.idCliente)}</option>`).join("");
    if (!rows.some(client => String(client.idCliente) === String(sellerAppCustomerId))) sellerAppCustomerId = String(rows[0].idCliente);
    select.value = String(sellerAppCustomerId);
    renderDashboardCustomers(rows);

// Busca os detalhes do cliente selecionado,
// incluindo cidade e UF.
try {
    await loadSellerCustomerDetails(currentCustomerId);
} catch (error) {
    console.error("Erro ao carregar detalhes do cliente:", error);
}

renderSellerApp();

await loadSellerDashboardRecommendations(currentCustomerId);
    await loadSellerAppData();
  } catch (error) {
    sellerAppClients = Object.keys(demoCustomers).map(id => ({
      idCliente: Number(id), nome: demoCustomers[id].name, segmento: demoCustomers[id].profile, subsegmento: ""
    }));
    select.innerHTML = sellerAppClients.map(client => `<option value="${client.idCliente}">${escapeHtml(client.nome)}</option>`).join("");
    if (!sellerAppClients.some(client => String(client.idCliente) === String(sellerAppCustomerId))) sellerAppCustomerId = "1001";
    select.value = String(sellerAppCustomerId);
    sellerAppRecommendations = demoRecommendations[sellerAppCustomerId] || demoRecommendations["1001"];
    renderDashboardCustomers(sellerAppClients);
    currentRecommendations = demoRecommendations[currentCustomerId] || demoRecommendations["1001"];
    renderRecommendations();
    setConnectionStatus(false, `<strong>Modo demonstração ativo.</strong> Não foi possível acessar a API; exibindo dados fictícios.`);
    renderSellerApp();
  }
}

async function loadSellerAppData() {
  if (!$("seller-app-view")) return;
  const customerId = sellerAppCustomerId;
  try {
    const url = `${apiBase.replace(/\/+$/, "")}/api/app-vendedor/clientes/${encodeURIComponent(customerId)}/recomendacoes`;
    const response = await fetch(url, { headers: { "Accept": "application/json" } });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const payload = await response.json();
    if (!Array.isArray(payload)) throw new Error("Formato de recomendações inesperado.");
    sellerAppRecommendations = mapApiRecommendations(payload);
    apiConnected = true;
    renderSellerApp();
  } catch (error) {
    if (demoRecommendations[customerId]) {
      sellerAppRecommendations = demoRecommendations[customerId];
    } else {
      sellerAppRecommendations = [];
    }
    renderSellerApp();
  }
}

async function loadSellerAppNotifications() {
  const target = $("seller-app-notification-list");
  if (!target) return;
  target.innerHTML = `<p class="seller-app-empty">Consultando pedidos recentes…</p>`;
  try {
    const url = `${apiBase.replace(/\/+$/, "")}/api/app-vendedor/clientes/${encodeURIComponent(sellerAppCustomerId)}/pedidos-recentes`;
    const response = await fetch(url, { headers: { "Accept": "application/json" } });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const rows = await response.json();
    target.innerHTML = rows.map(item => `
      <article class="seller-notification-item"><strong>Pedido #${escapeHtml(item.idPedido)}</strong><p>Pedido registrado no Supabase</p><small>${escapeHtml(new Date(item.dataPedido).toLocaleString("pt-BR"))}</small></article>
    `).join("") || `<p class="seller-app-empty">Nenhum pedido recente deste cliente. O histórico de notificações não é armazenado.</p>`;
  } catch (error) {
    target.innerHTML = `<p class="seller-app-empty">Não foi possível carregar pedidos: ${escapeHtml(error.message)}.</p>`;
  }
}

function showSellerAppScreen(screen) {
  sellerAppScreen = screen;
  renderSellerApp();
  if (screen === "notifications") loadSellerAppNotifications();
}

function setDemoData(customerId) {
  currentCustomerId = customerId;
  currentRecommendations = demoRecommendations[customerId] || demoRecommendations["1001"];
  renderCustomer();
  renderRecommendations();
}

async function loadFromApi(customerId) {
  const url = `${apiBase.replace(/\/+$/, "")}/api/clientes/${encodeURIComponent(customerId)}/recomendacoes`;
  $("api-status").textContent = `Consultando ${url} ...`;
  try {
    const headers = { "Accept": "application/json" };
    if (sessionToken) headers.Authorization = `Bearer ${sessionToken}`;
    const response = await fetch(url, { headers });
    if (response.status === 401 && sessionToken) {
      resetSimulation();
      $("chat-feedback").textContent = "A sessão da simulação expirou ou o backend foi reiniciado. Identifique o cliente novamente.";
      showToast("Sessão expirada. Identifique o cliente novamente.");
      return;
    }
    if (!response.ok) throw new Error(`API respondeu HTTP ${response.status}`);
    const payload = await response.json();
    const rows = Array.isArray(payload) ? payload : (payload.recomendacoes || payload.produtos || payload.items || []);
    if (!Array.isArray(rows)) throw new Error("Formato JSON não reconhecido. Esperava uma lista de recomendações.");
    currentRecommendations = mapApiRecommendations(rows);
    refreshCatalogProductsForCurrentRecommendations();
    apiConnected = true;
    $("connection-banner").classList.add("hidden");
    $("api-status").textContent = `Conectado com sucesso. ${rows.length} recomendação(ões) recebida(s).`;
    renderRecommendations();
    showToast("Recomendações carregadas da API Java.");
  } catch (error) {
    apiConnected = false;
    $("api-status").textContent = `Não foi possível conectar: ${error.message}. Confira backend, CORS e Supabase.`;
    if (sessionToken && identifiedCustomer) {
      currentRecommendations = [];
      renderRecommendations();
      $("connection-banner").classList.remove("hidden");
      $("connection-banner").querySelector("span:last-of-type").innerHTML = `<strong>API indisponível.</strong> Não vamos substituir dados reais por recomendações fictícias nesta conversa.`;
    } else {
      currentRecommendations = demoRecommendations[customerId] || demoRecommendations["1001"];
      renderRecommendations();
      $("connection-banner").classList.remove("hidden");
      $("connection-banner").querySelector("span:last-of-type").innerHTML = `<strong>Modo demonstração ativo.</strong> Não foi possível acessar a API; exibindo dados fictícios.`;
    }
    showToast("Não foi possível conectar à API.");
  }
}

async function loadCatalogFromApi() {
  try {
    const baseUrl = apiBase.replace(/\/+$/, "");

    const response = await fetch(`${baseUrl}/api/produtos`, {
      headers: {
        "Accept": "application/json"
      }
    });

    if (!response.ok) {
      throw new Error(`API respondeu HTTP ${response.status}`);
    }

    const rows = await response.json();

    if (!Array.isArray(rows)) {
      throw new Error("A API não retornou uma lista de produtos.");
    }

    catalogProductRows = rows.map(item => ({
      id: String(item.idItem ?? item.produtoId ?? item.id),
      name: item.nome ?? item.name ?? item.nomeProduto ?? "Produto sem nome",
      brand: item.codFamilia ? `Família ${item.codFamilia}` : "Catálogo Fruki",
      category: item.codFamilia ? String(item.codFamilia) : "Produtos",
      emoji: emojiFor(item.nome ?? item.name),
      imageUrl: item.imageUrl ?? item.imagemUrl ?? item.fotoUrl ?? item.urlImagem ?? portfolioImageForProduct(item.nome ?? item.name ?? item.nomeProduto),
      precoBase: item.precoBase,
      precoMinimo: item.precoMinimo
    }));

    catalogIsFromApi = rows.length > 0;
    catalogProducts = uniqueCatalogProducts(catalogProductRows);

    if (!catalogIsFromApi) {
      throw new Error("O catálogo retornou vazio.");
    }

    $("chat-feedback").textContent =
      `Catálogo conectado: ${catalogProducts.length} apresentações únicas em ${rows.length} registros do Supabase.`;

    return true;

  } catch (error) {
    catalogIsFromApi = false;
    catalogProductRows = demoCatalogProducts.slice();
    catalogProducts = demoCatalogProducts.slice();

    $("chat-feedback").textContent =
      `Não foi possível carregar o catálogo real: ${error.message}`;

    showToast(
      "Catálogo real indisponível. Não será possível gravar pedidos com produtos fictícios."
    );

    return false;
  }
}

function renderIdentificationScreen() {
  $("chat-screen-body").innerHTML = `<div class="chat-messages phone-messages" id="chat-messages">
    <div class="chat-date">HOJE</div>
    <div class="message received"><p>Olá! 👋 Bem-vindo ao Pede Fruki. Para consultar recomendações e montar um pedido, envie seu <strong>CNPJ cadastrado</strong> ou o <strong>código do cliente</strong>.</p><time>Agora</time></div>
    <div class="message received"><p>Esta é uma simulação: a identificação associa esta conversa ao cadastro encontrado no Supabase; ainda não comprova a identidade legal do usuário.</p></div>
  </div>`;
  const input = $("chat-input");
  if (input) input.placeholder = "Digite CNPJ ou código do cliente";
  $("chat-customer-name").textContent = "Cliente não identificado";
  $("chat-feedback").textContent = "A conversa só poderá montar e salvar um pedido após identificar um cadastro real.";
}

function renderVerifiedChatHome(message) {
  $("chat-screen-body").innerHTML = homeScreenHTML;
  renderChatProducts();
  if (message) chatReply(message);
  const input = $("chat-input");
  if (input) input.placeholder = "Digite uma mensagem…";
}

function resetSimulation() {
  sessionToken = null;
  identifiedCustomer = null;
  sessionStorage.removeItem("frukiSimToken");
  sessionStorage.removeItem("frukiCustomerId");
  sessionStorage.removeItem("frukiIdentifiedCustomer");
  Object.keys(cart).forEach(key => delete cart[key]);
  updateCartCount();
  catalogIsFromApi = false;
  catalogProductRows = demoCatalogProducts.slice();
  catalogProducts = demoCatalogProducts.slice();
  currentCustomerId = "1001";
  currentRecommendations = demoRecommendations[currentCustomerId];
  renderCustomer();
  renderRecommendations();
  renderIdentificationScreen();
}

function refreshRecommendations() {
  loadSellerDashboardRecommendations(currentCustomerId);
  if (sessionToken && identifiedCustomer) {
    loadFromApi(currentCustomerId);
    loadCatalogFromApi();
  }
}

$("seller-app-client-select").addEventListener("change", async event => {
  sellerAppCustomerId = String(event.target.value);
  sellerAppScreen = "home";
  renderSellerApp();
  await loadSellerAppData();
});
$("seller-app-start").addEventListener("click", async () => {
  await loadSellerAppData();
  showSellerAppScreen("mix");
});
$("seller-app-refresh").addEventListener("click", async () => {
  await loadSellerAppClients();
  await loadSellerAppData();
  showToast("Dados do atendimento atualizados.");
});
$("seller-app-back").addEventListener("click", () => showSellerAppScreen("home"));
$("seller-app-back-bottom").addEventListener("click", () => showSellerAppScreen("home"));
$("seller-app-open-whatsapp").addEventListener("click", () => {
  setTab("whatsapp");
  if (!sessionToken) {
    const input = $("chat-input");
    input.value = String(sellerAppCustomerId);
    input.placeholder = "Confirme o ID/CNPJ para iniciar a conversa";
    $("chat-feedback").textContent = `PDV selecionado no app do vendedor: ${sellerAppCustomer().name}. Envie o código para vincular a simulação do Pede Fruki.`;
  }
});
$("seller-app-notifications-back").addEventListener("click", () => showSellerAppScreen("home"));
$("seller-app-reload-notifications").addEventListener("click", loadSellerAppNotifications);
document.querySelectorAll("[data-seller-action]").forEach(button => button.addEventListener("click", async () => {
  const action = button.dataset.sellerAction;
  if (action === "mix") {
    await loadSellerAppData();
    showSellerAppScreen("mix");
  } else if (action === "notifications") {
    showSellerAppScreen("notifications");
  } else if (action === "history") {
    showToast("A consulta detalhada do histórico de itens ainda não está exposta como tela no protótipo.");
  } else if (action === "client") {
    const customer = sellerAppCustomer();
    showToast(`${customer.name} · ${customer.profile}`);
  }
}));

document.querySelectorAll(".nav-item").forEach(button => button.addEventListener("click", () => setTab(button.dataset.tab)));
$("customer-select").addEventListener("change", async event => {
    if (sessionToken && identifiedCustomer) {
        event.target.value = String(identifiedCustomer.idCliente);

        showToast("Esta conversa está vinculada ao cliente identificado. Reinicie a conversa para identificar outro cliente.");

        return;
    }

    currentCustomerId = String(event.target.value);

    // Atualiza os dados básicos imediatamente.
    renderCustomer();

    // Busca cidade, UF e demais detalhes.
    try {
        await loadSellerCustomerDetails(currentCustomerId);
    } catch (error) {
        console.error("Erro ao carregar cidade do cliente:", error);
    }

    // Atualiza as recomendações.
    await loadSellerDashboardRecommendations(currentCustomerId);
});
$("refresh-recommendations").addEventListener("click", refreshRecommendations);
$("configure-api").addEventListener("click", () => $("api-settings").classList.toggle("hidden"));
$("save-api").addEventListener("click", () => {
  const value = $("api-url").value.trim();
  if (!value) {
    $("api-status").textContent = "Informe a URL base da API.";
    return;
  }
  apiBase = value.replace(/\/+$/, "");
  localStorage.setItem("frukiApiBase", apiBase);
  loadSellerAppClients();
  if (sessionToken && identifiedCustomer) loadCatalogFromApi();
});
const demoCatalogProducts = [
  { id: "agua500", name: "Água da Pedra Sem gás 500ml", brand: "Água da Pedra · PET 500ml · fardo c/ 12 un.", category: "Água Mineral", emoji: "💧" },
  { id: "aguaGas", name: "Água da Pedra Com gás 500ml", brand: "Água da Pedra · PET 500ml · fardo c/ 12 un.", category: "Água Mineral", emoji: "🫧" },
  { id: "agua5l", name: "Água da Pedra Sem gás 5L", brand: "Água da Pedra · galão 5L", category: "Água Mineral", emoji: "🚰" },
  { id: "guarana2l", name: "Fruki Guaraná 2L", brand: "Fruki · PET 2L · fardo c/ 6 un.", category: "Refrigerantes", emoji: "🥤" },
  { id: "laranja2l", name: "Fruki Laranja 2L", brand: "Fruki · PET 2L · fardo c/ 6 un.", category: "Refrigerantes", emoji: "🍊" },
  { id: "elev473", name: "Elev Sabor Original 473ml", brand: "Elev · lata 473ml · fardo c/ 6 un.", category: "Energéticos", emoji: "⚡" },
  { id: "elevZero", name: "Elev Zero Açúcar 269ml", brand: "Elev · lata 269ml · fardo c/ 12 un.", category: "Energéticos", emoji: "🟣" },
  { id: "frukito", name: "Frukito Frutas Cítricas 500ml", brand: "Frukito · PET 500ml · fardo c/ 12 un.", category: "Sucos", emoji: "🍋" }
];
let catalogProducts = demoCatalogProducts.slice();
let catalogProductRows = [];
const cart = {};
const MINIMUM_ORDER_QUANTITY = 100;
let activeCatalogCategory = "Todos";
let catalogSearchTerm = "";
const homeScreenHTML = $("chat-screen-body").innerHTML;

function cartTotal() { return Object.values(cart).reduce((sum, quantity) => sum + quantity, 0); }
function updateCartCount() { const count = $("cart-count"); if (count) count.textContent = cartTotal(); }
function showPhoneScreen(screen) {
  if (!sessionToken) { renderIdentificationScreen(); return; }
  const body = $("chat-screen-body");
  if (screen === "home") { body.innerHTML = homeScreenHTML; renderChatProducts(); return; }
  if (screen === "catalog") { renderCatalogScreen(); return; }
  if (screen === "cart") { renderCartScreen(); return; }
  if (screen === "reminders") { renderRemindersScreen(); return; }
}
function renderCatalogScreen() {
  if (!sessionToken) { renderIdentificationScreen(); return; }
  if (!catalogIsFromApi) {
    $("chat-screen-body").innerHTML = `<div class="catalog-screen"><h3 class="mobile-screen-title">Catálogo</h3><p class="mobile-screen-subtitle">Para salvar um pedido precisamos dos IDs reais dos produtos do Supabase.</p><button id="catalog-retry" class="primary-mobile-btn">Tentar carregar catálogo</button><button id="home-back" class="chat-cta">Voltar à conversa</button></div>`;
    return;
  }
  const categories = ["Todos", ...new Set(catalogProducts.map(product => product.category).filter(Boolean))].slice(0, 7);
  const filtered = catalogProducts.filter(product =>
    (activeCatalogCategory === "Todos" || product.category === activeCatalogCategory) &&
    `${product.name} ${product.brand}`.toLowerCase().includes(catalogSearchTerm.toLowerCase())
  );
  const categoryButtons = categories.map(category => `
    <button class="category-chip ${activeCatalogCategory === category ? "active" : ""}" data-category="${escapeHtml(category)}">${escapeHtml(category)}</button>
  `).join("");
  const productRows = filtered.map(product => `
    <article class="catalog-item">
      ${productVisualMarkup(product)}
      <div><strong>${escapeHtml(product.name)}</strong><small>${escapeHtml(product.brand)}<br>Preço calculado pelo backend conforme segmento</small></div>
      <div class="quantity-control">${cart[product.id]
        ? `<button data-qty="-" data-product="${escapeHtml(product.id)}" aria-label="Diminuir quantidade">−</button><span>${cart[product.id]}</span><button data-qty="+" data-product="${escapeHtml(product.id)}" aria-label="Aumentar quantidade">+</button>`
        : `<button data-qty="+" data-product="${escapeHtml(product.id)}" aria-label="Adicionar quantidade">+10</button>`}</div>
    </article>
  `).join("");
  $("chat-screen-body").innerHTML = `<div class="catalog-screen">
    <h3 class="mobile-screen-title">Catálogo do Supabase</h3>
    <p class="mobile-screen-subtitle">${catalogProducts.length} produtos ativos · pedido mínimo de ${MINIMUM_ORDER_QUANTITY} fardos</p>
    <input id="catalog-search" class="catalog-search" placeholder="Buscar produto" value="${escapeHtml(catalogSearchTerm)}">
    <div class="category-chips">${categoryButtons}</div>
    ${productRows || `<p>Nenhum produto encontrado.</p>`}
    <div class="catalog-bottom"><button id="catalog-cart-button" class="primary-mobile-btn">Ver carrinho · ${cartTotal()}/${MINIMUM_ORDER_QUANTITY} fardos</button></div>
  </div>`;
  bindProductImages($("chat-screen-body"));
}
function renderCartScreen() {
  if (!sessionToken) { renderIdentificationScreen(); return; }
  const items = catalogProducts.filter(p => cart[p.id]);
  const checkoutDisabled = cartTotal() < MINIMUM_ORDER_QUANTITY || !catalogIsFromApi;
  $("chat-screen-body").innerHTML = `<div class="cart-screen">
    <h3 class="mobile-screen-title">Seu carrinho</h3>
    <p class="mobile-screen-subtitle">Confira os itens e as quantidades antes de enviar o pedido.</p>
    <div class="cart-total"><strong>${cartTotal()} fardos no carrinho</strong><br><span>${cartTotal() >= MINIMUM_ORDER_QUANTITY ? "Pedido mínimo atingido ✓" : `Faltam ${MINIMUM_ORDER_QUANTITY - cartTotal()} fardos para atingir o pedido mínimo de ${MINIMUM_ORDER_QUANTITY}.`}</span></div>
    ${items.length ? items.map(product => `<div class="cart-line">
      <div><strong>${escapeHtml(product.name)}</strong><small>${escapeHtml(product.brand)}</small></div>
      <div class="cart-quantity-control">
        <button data-qty="-" data-product="${escapeHtml(product.id)}" aria-label="Diminuir ${escapeHtml(product.name)}">−</button>
        <strong>${cart[product.id]}</strong>
        <button data-qty="+" data-product="${escapeHtml(product.id)}" aria-label="Aumentar ${escapeHtml(product.name)}">+</button>
      </div>
    </div>`).join("") : `<p>Seu carrinho está vazio. Explore o catálogo ou adicione as sugestões inteligentes.</p>`}
    <div class="cart-actions">
      <button id="cart-add-suggestions" class="chat-cta">Adicionar sugestões inteligentes</button>
      <button id="checkout-button" class="primary-mobile-btn" ${checkoutDisabled ? "disabled" : ""}>Finalizar e salvar pedido · ${cartTotal()} fardos</button>
      <button id="catalog-back" class="chat-cta">Voltar ao catálogo</button>
      ${!catalogIsFromApi ? `<p class="mobile-screen-subtitle">Carregue o catálogo real do Supabase para salvar o pedido.</p>` : ""}
    </div>
  </div>`;
}
async function renderRemindersScreen() {
  if (!sessionToken || !identifiedCustomer) { renderIdentificationScreen(); return; }
  const targetId = String(currentCustomerId);
  const base = apiBase.replace(/\/+$/, "");
  const headers = { "Accept": "application/json", "Authorization": `Bearer ${sessionToken}` };
  $("chat-screen-body").innerHTML = `<div class="reminders-screen"><h3 class="mobile-screen-title">Clima e calendário</h3><p class="mobile-screen-subtitle">Carregando informações reais do cliente identificado…</p><button id="home-back" class="chat-cta">Voltar à conversa</button></div>`;
  try {
    const contextResponse = await fetch(`${base}/api/clientes/${encodeURIComponent(targetId)}/contexto`, { headers });
    if (contextResponse.status === 401) {
      resetSimulation();
      chatReply("A sessão expirou. Identifique o cliente novamente para consultar o contexto.");
      return;
    }
    const context = contextResponse.ok ? await contextResponse.json() : null;
    const weatherRoot = context?.clima;
    const currentWeather = weatherRoot?.current || null;
    const temperature = currentWeather?.temperature_2m;
    const precipitation = currentWeather?.precipitation;
    const weatherSummary = temperature != null
      ? `${Number(temperature).toLocaleString("pt-BR")} °C · precipitação atual ${Number(precipitation ?? 0).toLocaleString("pt-BR")} mm`
      : "Dados do clima indisponíveis no momento";
    const holidays = Array.isArray(context?.feriados) ? context.feriados.filter(item => item && item.date) : [];
    const holidayHtml = holidays.length ? holidays.map(item => `
      <div class="reminder-card"><strong>${escapeHtml(item.name || "Feriado nacional")}</strong>
      <p>${escapeHtml(new Date(`${item.date}T12:00:00`).toLocaleDateString("pt-BR", {day:"2-digit",month:"long",year:"numeric"}))}</p></div>`).join("")
      : `<p class="mobile-screen-subtitle">Nenhum feriado próximo encontrado ou o calendário está indisponível.</p>`;
    const contextMessage = context?.observacao || "O clima e os feriados são sinais heurísticos do motor de recomendação, não garantia de venda.";
    $("chat-screen-body").innerHTML = `<div class="reminders-screen">
      <h3 class="mobile-screen-title">Clima e calendário</h3>
      <p class="mobile-screen-subtitle">Cliente: ${escapeHtml(identifiedCustomer.nome || targetId)} · ${escapeHtml([context?.cidade, context?.uf].filter(Boolean).join(", "))}</p>
      <div class="context-chips"><span>${escapeHtml(weatherSummary)}</span></div>
      <p style="font-size:12px;line-height:1.5;color:#4d665b">${escapeHtml(contextMessage)}<br><strong style="color:#19836b">Regra inicial: produtos reconhecidos como bebidas recebem até 10 pontos em dias quentes e até 10 pontos perto de feriados.</strong></p>
      <h4 style="color:#267762;font-size:11px;letter-spacing:.7px;margin-top:20px">PRÓXIMOS FERIADOS NACIONAIS</h4>${holidayHtml}
      ${!contextResponse.ok ? `<p class="mobile-screen-subtitle">O contexto não pôde ser carregado (HTTP ${contextResponse.status}). Confira se o cliente possui cidade cadastrada.</p>` : ""}
      <button id="refresh-context" class="primary-mobile-btn">Atualizar informações</button><button id="home-back" class="chat-cta" style="margin-top:8px">Voltar à conversa</button>
    </div>`;
  } catch (error) {
    $("chat-screen-body").innerHTML = `<div class="reminders-screen"><h3 class="mobile-screen-title">Contexto indisponível</h3><p class="mobile-screen-subtitle">Não foi possível consultar o backend: ${escapeHtml(error.message)}</p><button id="refresh-context" class="primary-mobile-btn">Tentar novamente</button><button id="home-back" class="chat-cta">Voltar à conversa</button></div>`;
  }
}
function addCartProduct(id, amount=10) { cart[id] = Math.max(0, (cart[id] || 0) + amount); if (cart[id] === 0) delete cart[id]; updateCartCount(); }
function addWeeklySuggestions() {
  if (!sessionToken || !catalogIsFromApi) { chatReply("Antes de adicionar sugestões, identifique o cliente e carregue o catálogo real do Supabase."); return; }
  const available = (currentRecommendations || []).filter(r => catalogProducts.some(p => String(p.id) === String(r.id))).slice(0, 3);
  if (!available.length) { chatReply("Não há recomendações vinculadas ao catálogo carregado. Abra o catálogo para escolher produtos."); return; }
  const amounts = recommendationBundleAmounts(available.length);
  available.forEach((recommendation, index) => addCartProduct(String(recommendation.id), amounts[index]));
  chatReply(`Adicionei ${available.length} produto(s) recomendado(s), totalizando ${amounts.slice(0, available.length).reduce((a, b) => a + b, 0)} fardos. Confira o carrinho antes de finalizar.`);
  $("chat-feedback").textContent = "Sugestões baseadas em recomendações reais da API.";
  showPhoneScreen("cart");
}
function chatReply(message) {
  const messages = $("chat-messages");
  if (!messages) return;
  const bubble = document.createElement("div"); bubble.className = "message received";
  const p = document.createElement("p"); p.textContent = message;
  const time = document.createElement("time"); time.textContent = new Date().toLocaleTimeString("pt-BR", {hour:"2-digit",minute:"2-digit"});
  bubble.append(p,time); messages.appendChild(bubble); messages.scrollTop = messages.scrollHeight;
}
function chatUserMessage(message) {
  const messages = $("chat-messages");
  if (!messages) return;
  const bubble = document.createElement("div"); bubble.className = "message sent";
  const p = document.createElement("p"); p.textContent = message;
  const time = document.createElement("time"); time.textContent = new Date().toLocaleTimeString("pt-BR", {hour:"2-digit",minute:"2-digit"});
  bubble.append(p, time); messages.appendChild(bubble); messages.scrollTop = messages.scrollHeight;
}

async function identifyChatCustomer(identifier) {
  const response = await fetch(`${apiBase.replace(/\/+$/, "")}/api/simulacao/identificar`, {
    method: "POST",
    headers: { "Accept": "application/json", "Content-Type": "application/json" },
    body: JSON.stringify({ identificador: identifier })
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.mensagem || payload.message || `Não foi possível identificar (HTTP ${response.status}).`);
  sessionToken = payload.tokenSessao;
  identifiedCustomer = payload.cliente;
  currentCustomerId = String(payload.cliente.idCliente);
  currentRecommendations = [];
  sessionStorage.setItem("frukiSimToken", sessionToken);
  sessionStorage.setItem("frukiCustomerId", currentCustomerId);
  sessionStorage.setItem("frukiIdentifiedCustomer", JSON.stringify(identifiedCustomer));
  Object.keys(cart).forEach(key => delete cart[key]);
  updateCartCount();
  renderCustomer();
  renderVerifiedChatHome(payload.mensagem || `Cliente ${identifiedCustomer.nome} identificado.`);
  await Promise.all([loadFromApi(currentCustomerId), loadCatalogFromApi()]);
  if (catalogIsFromApi) chatReply("Tudo pronto! Escreva 'catálogo' para escolher produtos ou 'carrinho' para revisar seu pedido.");
  $("chat-feedback").textContent = `Conversa associada ao cliente ${identifiedCustomer.nome}. Sessão demonstrativa válida por até 2 horas.`;
}

async function sendChatMessage() {
  const input = $("chat-input");
  if (!input) return;
  const message = input.value.trim();
  if (!message) return;
  input.value = "";
  chatUserMessage(message);
  if (!sessionToken) {
    try {
      await identifyChatCustomer(message);
    } catch (error) {
      chatReply(`${error.message} Envie o CNPJ cadastrado (com ou sem pontuação) ou o código numérico do cliente.`);
      $("chat-feedback").textContent = "Cliente ainda não identificado.";
    }
    return;
  }
  const lower = message.toLowerCase();
  if (lower.includes("trocar cliente") || lower.includes("sair") || lower.includes("reiniciar")) {
    resetSimulation();
    chatReply("Conversa reiniciada. Informe o CNPJ ou o código do próximo cliente.");
  } else if (lower.includes("catálogo") || lower.includes("catalogo") || lower.includes("produto")) {
    renderCatalogScreen();
  } else if (lower.includes("carrinho") || lower.includes("finalizar") || lower.includes("pedido")) {
    renderCartScreen();
  } else if (lower.includes("sugest")) {
    addWeeklySuggestions();
  } else {
    chatReply("Posso ajudar com: 'catálogo', 'sugestões', 'carrinho' ou 'finalizar pedido'. Para simular outro cliente, clique no ícone de voltar ao início e informe outro CNPJ/código.");
  }
}

document.addEventListener("click", async event => {
  const el = event.target.closest("button"); if (!el) return;
  if (el.closest("#whatsapp-view") && !sessionToken && !["reset-chat", "configure-api", "save-api"].includes(el.id)) {
    if (["catalog-retry"].includes(el.id)) { showToast("Identifique o cliente primeiro."); return; }
    chatReply("Para continuar, envie primeiro o CNPJ cadastrado ou o código do cliente na caixa de mensagem abaixo.");
    return;
  }
  if (el.id === "catalog-open" || el.id === "catalog-open-inline") { activeCatalogCategory="Todos"; catalogSearchTerm=""; renderCatalogScreen(); }
  else if (el.id === "cart-open" || el.id === "catalog-cart-button") renderCartScreen();
  else if (el.id === "reminders-open") renderRemindersScreen();
  else if (el.id === "refresh-context") renderRemindersScreen();
  else if (el.id === "chat-add" || el.id === "cart-add-suggestions") addWeeklySuggestions();
  else if (el.id === "chat-more") { showPhoneScreen("home"); const target=$("weekly-products"); if(target) target.scrollIntoView({behavior:"smooth",block:"center"}); }
  else if (el.id === "chat-finish") renderCartScreen();
  else if (el.id === "reset-chat") resetSimulation();
  else if (el.id === "home-back") showPhoneScreen("home");
  else if (el.id === "catalog-retry") { await loadCatalogFromApi(); renderCatalogScreen(); }
  else if (el.id === "talk-seller") { chatReply("Tudo certo! Um vendedor Fruki entraria em contato para ajudar com valores e entrega."); $("chat-feedback").textContent="Contato com vendedor simulado — nenhuma mensagem foi enviada."; }
  else if (el.id === "phone-menu") showToast("Mais opções: demonstração do Pede Fruki.");
  else if (el.dataset.category) { activeCatalogCategory=el.dataset.category; renderCatalogScreen(); }
  else if (el.dataset.qty) {
    addCartProduct(el.dataset.product, el.dataset.qty === "+" ? 10 : -10);
    el.closest(".cart-screen") ? renderCartScreen() : renderCatalogScreen();
  }
  else if (el.dataset.reminderQty) {
    const reminderProduct = catalogProducts[0];
    if (reminderProduct) addCartProduct(String(reminderProduct.id), Number(el.dataset.reminderQty));
    showToast(`Sugestão de ${el.dataset.reminderQty} fardos adicionada ao primeiro produto do catálogo atual.`);
    renderRemindersScreen();
  }
  else if (el.id === "catalog-back") renderCatalogScreen();
  else if (el.id === "checkout-button") {
    if (cartTotal() < MINIMUM_ORDER_QUANTITY || !sessionToken || !catalogIsFromApi) return;
    const itens = Object.entries(cart).filter(([, quantidade]) => quantidade > 0).map(([produtoId, quantidade]) => ({ produtoId: Number(produtoId), quantidade }));
    if (itens.some(item => !Number.isInteger(item.produtoId))) {
      chatReply("Algum produto do carrinho não pertence ao catálogo real. Esvazie o carrinho, abra o catálogo novamente e escolha os produtos do Supabase.");
      return;
    }
    el.disabled = true; el.textContent = "Salvando pedido…";
    try {
      const response = await fetch(`${apiBase.replace(/\/+$/, "")}/api/pedidos`, {
        method: "POST",
        headers: { "Accept": "application/json", "Content-Type": "application/json", "Authorization": `Bearer ${sessionToken}` },
        body: JSON.stringify({ itens })
      });
      const payload = await response.json().catch(() => ({}));
      if (response.status === 401) {
        resetSimulation();
        throw new Error("A sessão expirou ou o backend foi reiniciado. Identifique o cliente novamente; o pedido não foi confirmado como salvo.");
      }
      if (!response.ok) throw new Error(payload.mensagem || payload.message || `Falha ao salvar o pedido (HTTP ${response.status}).`);
      const quantity = payload.quantidadeTotal ?? cartTotal();
      const totalMoney = Number(payload.valorTotal ?? 0).toLocaleString("pt-BR", {style:"currency",currency:"BRL"});
      const orderLines = (payload.itens || []).map(item => {
        return `<div class="cart-line"><div><strong>${escapeHtml(item.nomeProduto)}</strong><small>${item.quantidade} fardos × ${Number(item.valorUnitario).toLocaleString("pt-BR", {style:"currency",currency:"BRL"})}</small></div><strong>${Number(item.subtotal).toLocaleString("pt-BR", {style:"currency",currency:"BRL"})}</strong></div>`;
      }).join("");
      Object.keys(cart).forEach(key => delete cart[key]); updateCartCount();
      const observationsHtml = (payload.observacoes || []).map(item => `<p class="mobile-screen-subtitle">${escapeHtml(item)}</p>`).join("");
      const stockMessage = payload.estoqueAtualizado === true ? "Estoque conhecido baixado com sucesso." : "Confira a situação do estoque nas observações.";
      $("chat-screen-body").innerHTML = `<div class="cart-screen"><div style="text-align:center;padding:28px 12px"><div style="font-size:48px;color:#00a884">✓</div><h3 class="mobile-screen-title">Pedido salvo!</h3><p style="color:#78847d;font-size:12px;line-height:1.6">Pedido <strong>#${escapeHtml(payload.idPedido)}</strong> gravado no banco de dados para <strong>${escapeHtml(payload.cliente?.nome || identifiedCustomer?.nome || "cliente identificado")}</strong>.</p></div>${orderLines}<div class="cart-total"><strong>${quantity} fardos</strong><br><span>Total registrado: ${totalMoney}</span></div><p class="mobile-screen-subtitle">${escapeHtml(payload.mensagem || "Pedido persistido no Supabase.")}</p><p class="mobile-screen-subtitle">${escapeHtml(stockMessage)}</p><p class="mobile-screen-subtitle">O pedido foi salvo no Supabase. Se habilitado, o backend tentará enviar um evento ao Resend após a confirmação da transação. Não existe histórico de notificações no banco.</p>${observationsHtml}<button id="reminders-open" class="primary-mobile-btn">Ver clima e calendário</button><button id="home-back" class="chat-cta" style="margin-top:8px">Voltar à conversa</button></div>`;
      $("chat-feedback").textContent = `Pedido #${payload.idPedido} salvo no Supabase. Se Resend_ENABLED=true, a API também tentou encaminhar a confirmação ao webhook.`;
      showToast(`Pedido #${payload.idPedido} salvo com sucesso.`);
    } catch (error) {
      renderCartScreen();
      $("chat-feedback").textContent = `O pedido não foi confirmado como salvo: ${error.message}`;
      showToast(error.message);
    }
  }
});
document.addEventListener("input", event => { if (event.target.id === "catalog-search") { catalogSearchTerm=event.target.value; const pos=event.target.selectionStart; renderCatalogScreen(); const input=$("catalog-search"); input.focus(); input.setSelectionRange(pos,pos); } });
$("send-chat").addEventListener("click", sendChatMessage);
$("chat-input").addEventListener("keydown", event => {
  if (event.key === "Enter") { event.preventDefault(); sendChatMessage(); }
});

$("api-url").value = apiBase;
setDemoData(currentCustomerId);
loadSellerAppClients();
if (sessionToken && identifiedCustomer) {
  currentCustomerId = String(identifiedCustomer.idCliente);
  renderCustomer();
  renderVerifiedChatHome("Sessão demonstrativa restaurada. Se o backend foi reiniciado, identifique o cliente novamente.");
  loadFromApi(currentCustomerId);
  loadCatalogFromApi();
} else {
  sessionToken = null;
  identifiedCustomer = null;
  renderIdentificationScreen();
}
