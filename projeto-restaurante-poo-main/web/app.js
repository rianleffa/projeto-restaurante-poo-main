(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));
  const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
  const statusNames = {
    RECEBIDO: 'Recebido',
    EM_PREPARO: 'Em preparo',
    SAIU_PARA_ENTREGA: 'Saiu para entrega',
    ENTREGUE: 'Entregue'
  };
  const activeStatuses = new Set(['RECEBIDO', 'EM_PREPARO', 'SAIU_PARA_ENTREGA']);
  let data = { clientes: [], produtos: [], pedidos: [], notificacoes: [], relatorio: {} };
  let toastTimeout;

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, char => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[char]);
  }

  function money(value) { return currency.format(Number(value) || 0); }

  async function request(url, options = {}) {
    const response = await fetch(url, options);
    let result;
    try { result = await response.json(); } catch { result = {}; }
    if (!response.ok) throw new Error(result.erro || 'Não foi possível concluir a operação.');
    return result;
  }

  function formOptions(values) {
    return { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' }, body: new URLSearchParams(values) };
  }

  async function refresh() {
    data = await request('/api/dashboard');
    $('#loading').hidden = true;
    render();
  }

  function render() {
    const report = data.relatorio || {};
    const activeCount = data.pedidos.filter(order => activeStatuses.has(order.status)).length;
    $('#metric-orders').textContent = data.pedidos.length;
    $('#metric-orders-note').textContent = data.pedidos.length === 1 ? '1 pedido no sistema' : 'Pedidos no sistema';
    $('#metric-active').textContent = activeCount;
    $('#metric-sales').textContent = money(report.faturamento);
    $('#metric-clients').textContent = data.clientes.length;
    $('#report-sales').textContent = money(report.faturamento);
    $('#report-orders').textContent = report.pedidos ?? data.pedidos.length;
    $('#report-products').textContent = report.produtos ?? data.produtos.length;
    $('#report-clients').textContent = report.clientes ?? data.clientes.length;
    $('#nav-order-count').textContent = data.pedidos.length;
    const notificationCount = data.notificacoes.length;
    const badge = $('#nav-notification-count');
    badge.textContent = notificationCount > 99 ? '99+' : notificationCount;
    badge.hidden = notificationCount === 0;
    $('#header-notification-dot').hidden = notificationCount === 0;
    $('#notification-summary').textContent = notificationCount === 1 ? '1 atualização' : `${notificationCount} atualizações`;
    $('#today-label').textContent = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date()).toLocaleUpperCase('pt-BR');
    renderRecentOrders();
    renderOrders($('#order-search').value);
    renderProducts($('#product-search').value);
    renderClients($('#client-search').value);
    renderNotifications();
    renderReport(report);
    populateOrderClientSelect();
  }

  function orderStatus(order) {
    return `<span class="status-pill status-${escapeHtml(order.status)}">${escapeHtml(order.statusDescricao || statusNames[order.status] || order.status)}</span>`;
  }

  function orderItems(order) {
    return order.itens.map(item => `${item.quantidade} × ${escapeHtml(item.nome)}`).join(' · ');
  }

  function renderRecentOrders() {
    const target = $('#recent-orders');
    const orders = [...data.pedidos].sort((a, b) => b.numero - a.numero).slice(0, 4);
    target.innerHTML = orders.length ? orders.map(order => `<div class="recent-order">
      <span class="recent-number"><b class="order-number">#${order.numero}</b><small>${escapeHtml(order.cliente)}</small></span>
      <span class="recent-name"><b>${escapeHtml(order.itens[0]?.nome || 'Pedido')}</b><small>${order.itens.length} ${order.itens.length === 1 ? 'item' : 'itens'}</small></span>
      <span>${orderStatus(order)}</span><b class="recent-total">${money(order.total)}</b>
    </div>`).join('') : '<div class="empty-state"><b>A mesa está pronta.</b>Os próximos pedidos aparecem aqui.</div>';
  }

  function renderOrders(search = '') {
    const target = $('#all-orders');
    const term = search.trim().toLocaleLowerCase('pt-BR');
    const orders = [...data.pedidos].sort((a, b) => b.numero - a.numero).filter(order => `${order.numero} ${order.cliente} ${order.email} ${order.itens.map(item => item.nome).join(' ')}`.toLocaleLowerCase('pt-BR').includes(term));
    $('#order-summary').textContent = `${data.pedidos.length} ${data.pedidos.length === 1 ? 'pedido registrado' : 'pedidos registrados'}`;
    target.innerHTML = orders.length ? orders.map(order => `<article class="order-card">
      <div class="order-card-main"><span><b>Pedido #${order.numero}</b><small>${escapeHtml(order.cliente)}</small></span><span><b>${escapeHtml(order.email)}</b><small>${order.itens.length} ${order.itens.length === 1 ? 'item' : 'itens'}${order.desconto ? ` · ${escapeHtml(order.desconto)}% de desconto` : ''}</small></span>${orderStatus(order)}<strong class="order-total">${money(order.total)}</strong></div>
      <div class="order-items"><span>${orderItems(order)}</span></div>
      <div class="order-controls"><label for="status-${order.numero}">Atualizar status</label><select id="status-${order.numero}" data-order-status="${order.numero}">${Object.entries(statusNames).map(([key, label]) => `<option value="${key}" ${order.status === key ? 'selected' : ''}>${label}</option>`).join('')}</select><small>O cliente recebe uma notificação de atualização.</small></div>
    </article>`).join('') : `<div class="panel empty-state"><b>${term ? 'Nenhum resultado por aqui.' : 'Ainda não há pedidos.'}</b>${term ? 'Tente buscar pelo nome do cliente ou número do pedido.' : 'Registre um pedido para começar a acompanhar o movimento.'}</div>`;
  }

  function renderProducts(search = '') {
    const term = search.trim().toLocaleLowerCase('pt-BR');
    const products = data.produtos.filter(product => `${product.nome} ${product.descricao} ${product.codigo}`.toLocaleLowerCase('pt-BR').includes(term));
    $('#product-summary').textContent = `${data.produtos.length} ${data.produtos.length === 1 ? 'produto cadastrado' : 'produtos cadastrados'}`;
    $('#product-grid').innerHTML = products.length ? products.map(product => `<article class="product-card"><span class="product-code">#${product.codigo}</span><span class="product-symbol">${product.tipo === 'pizza' ? '◉' : '✳'}</span><h3>${escapeHtml(product.nome)}</h3><p>${escapeHtml(product.descricao)}</p><strong>${money(product.preco)}</strong></article>`).join('') : `<div class="panel empty-state"><b>${term ? 'Nenhum produto encontrado.' : 'Seu cardápio começa aqui.'}</b>${term ? 'Experimente buscar por outro nome.' : 'Cadastre seus produtos para montar pedidos.'}</div>`;
  }

  function initials(name) {
    return String(name).trim().split(/\s+/).slice(0, 2).map(part => part.charAt(0)).join('');
  }

  function renderClients(search = '') {
    const term = search.trim().toLocaleLowerCase('pt-BR');
    const clients = data.clientes.filter(client => `${client.nome} ${client.email} ${client.telefone} ${client.endereco}`.toLocaleLowerCase('pt-BR').includes(term));
    $('#client-summary').textContent = `${data.clientes.length} ${data.clientes.length === 1 ? 'cliente cadastrado' : 'clientes cadastrados'}`;
    $('#client-list').innerHTML = clients.length ? clients.map(client => `<article class="client-card"><span class="client-avatar">${escapeHtml(initials(client.nome))}</span><span class="client-info"><b>${escapeHtml(client.nome)}</b><small>${escapeHtml(client.email)}</small></span><span class="client-contact"><small>${escapeHtml(client.telefone)}</small><small>${escapeHtml(client.endereco)}</small></span></article>`).join('') : `<div class="panel empty-state"><b>${term ? 'Nenhum cliente encontrado.' : 'Seus clientes ficam por aqui.'}</b>${term ? 'Tente buscar por outro contato.' : 'Cadastre alguém para iniciar um pedido.'}</div>`;
  }

  function notificationMarkup(message, index) {
    return `<article class="notification-entry"><span class="notification-icon">♢</span><div><p>${escapeHtml(message)}</p><small>${index === 0 ? 'Última atualização' : `Atualização ${index + 1}`}</small></div></article>`;
  }

  function renderNotifications() {
    const list = data.notificacoes || [];
    $('#all-notifications').innerHTML = list.length ? list.map(notificationMarkup).join('') : '<div class="empty-state"><b>Nenhuma atualização por enquanto.</b>Quando um pedido mudar de status, a notificação aparece aqui.</div>';
    $('#recent-notifications').innerHTML = list.length ? list.slice(0, 3).map(notificationMarkup).join('') : '<div class="empty-state"><b>Tudo tranquilo por aqui.</b>As mudanças de status dos pedidos aparecem nesta lista.</div>';
  }

  function renderReport(report) {
    const counts = report.porStatus || {};
    const total = Math.max(1, Number(report.pedidos) || 0);
    $('#status-breakdown').innerHTML = Object.entries(statusNames).map(([key, label]) => {
      const count = Number(counts[key]) || 0;
      const width = Math.round(count / total * 100);
      return `<div class="status-row"><span>${label}</span><div class="status-track"><div class="status-fill" style="width:${width}%"></div></div><b>${count}</b></div>`;
    }).join('');
  }

  function populateOrderClientSelect() {
    const select = $('#order-client');
    const previous = select.value;
    select.innerHTML = '<option value="">Selecione um cliente</option>' + data.clientes.map(client => `<option value="${escapeHtml(client.email)}">${escapeHtml(client.nome)} · ${escapeHtml(client.email)}</option>`).join('');
    if (data.clientes.some(client => client.email === previous)) select.value = previous;
  }

  function addOrderItem() {
    const template = $('#order-item-template');
    const row = template.content.firstElementChild.cloneNode(true);
    const productSelect = $('.item-product', row);
    productSelect.innerHTML = '<option value="">Selecione um produto</option>' + data.produtos.map(product => `<option value="${product.codigo}" data-type="${product.tipo}">${escapeHtml(product.nome)} · ${money(product.preco)}</option>`).join('');
    productSelect.addEventListener('change', () => {
      const selected = productSelect.options[productSelect.selectedIndex];
      $('.toppings', row).hidden = selected?.dataset.type !== 'pizza';
    });
    $('#order-items').append(row);
  }

  function openDialog(kind) {
    const dialog = $(`#dialog-${kind}`);
    if (kind === 'pedido') {
      populateOrderClientSelect();
      $('#order-items').innerHTML = '';
      addOrderItem();
    }
    if (typeof dialog.showModal === 'function') dialog.showModal();
    else dialog.setAttribute('open', '');
  }

  function closeDialog(button) {
    const dialog = button.closest('dialog');
    if (dialog?.close) dialog.close(); else dialog?.removeAttribute('open');
  }

  async function submitForm(form, endpoint, successMessage) {
    const values = Object.fromEntries(new FormData(form).entries());
    await request(endpoint, formOptions(values));
    form.reset();
    form.closest('dialog')?.close();
    await refresh();
    showToast(successMessage);
  }

  async function submitOrder(form) {
    const rows = $$('.order-item-row', $('#order-items'));
    if (!rows.length) throw new Error('Adicione ao menos um item ao pedido.');
    const values = new URLSearchParams(new FormData(form));
    values.set('itemCount', rows.length);
    rows.forEach((row, index) => {
      const prefix = `item${index}`;
      values.set(`${prefix}Produto`, $('.item-product', row).value);
      values.set(`${prefix}Quantidade`, $('.item-quantity', row).value);
      if ($('.item-borda', row).checked) values.set(`${prefix}Borda`, 'true');
      if ($('.item-queijo', row).checked) values.set(`${prefix}Queijo`, 'true');
      if ($('.item-bacon', row).checked) values.set(`${prefix}Bacon`, 'true');
    });
    await request('/api/pedidos', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' }, body: values });
    form.reset();
    form.closest('dialog')?.close();
    await refresh();
    showToast('Pedido registrado e enviado para a cozinha.');
  }

  function showToast(message, error = false) {
    const toast = $('#toast');
    toast.textContent = message;
    toast.classList.toggle('error', error);
    toast.classList.add('visible');
    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => toast.classList.remove('visible'), 3600);
  }

  function showError(error) {
    const banner = $('#error-banner');
    banner.textContent = error.message || 'Não foi possível concluir a operação.';
    banner.hidden = false;
    window.scrollTo({ top: 0, behavior: 'smooth' });
    showToast(banner.textContent, true);
    setTimeout(() => { banner.hidden = true; }, 6000);
  }

  function switchView(name) {
    $$('.view').forEach(view => {
      const active = view.id === `view-${name}`;
      view.classList.toggle('active', active);
      view.hidden = !active;
    });
    $$('.nav-item').forEach(button => button.classList.toggle('active', button.dataset.view === name));
    const labels = { inicio: 'Visão geral', pedidos: 'Pedidos', produtos: 'Produtos', clientes: 'Clientes', relatorios: 'Relatórios', notificacoes: 'Notificações' };
    $('#breadcrumb-current').textContent = labels[name] || 'Visão geral';
    $('.sidebar').classList.remove('open');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  document.addEventListener('click', event => {
    const viewButton = event.target.closest('[data-view]');
    if (viewButton) { switchView(viewButton.dataset.view); return; }
    const openButton = event.target.closest('[data-open]');
    if (openButton) { openDialog(openButton.dataset.open); return; }
    const closeButton = event.target.closest('[data-close]');
    if (closeButton) { closeDialog(closeButton); return; }
    if (event.target.id === 'add-order-item') { addOrderItem(); return; }
    if (event.target.classList.contains('remove-item')) {
      const row = event.target.closest('.order-item-row');
      if ($$('.order-item-row', $('#order-items')).length > 1) row.remove();
      else showToast('O pedido precisa ter pelo menos um item.', true);
    }
  });

  document.addEventListener('change', async event => {
    const select = event.target.closest('[data-order-status]');
    if (!select) return;
    try {
      await request(`/api/pedidos/${select.dataset.orderStatus}/status`, {
        method: 'PATCH', headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' }, body: new URLSearchParams({ status: select.value })
      });
      await refresh();
      showToast('Status atualizado. O cliente recebeu uma notificação.');
    } catch (error) { showError(error); await refresh().catch(() => {}); }
  });

  $('#customer-form').addEventListener('submit', event => { event.preventDefault(); submitForm(event.currentTarget, '/api/clientes', 'Cliente cadastrado com sucesso.').catch(showError); });
  $('#product-form').addEventListener('submit', event => { event.preventDefault(); submitForm(event.currentTarget, '/api/produtos', 'Produto adicionado ao cardápio.').catch(showError); });
  $('#order-form').addEventListener('submit', event => { event.preventDefault(); submitOrder(event.currentTarget).catch(showError); });
  $('#order-search').addEventListener('input', event => renderOrders(event.target.value));
  $('#product-search').addEventListener('input', event => renderProducts(event.target.value));
  $('#client-search').addEventListener('input', event => renderClients(event.target.value));
  $('#mobile-menu').addEventListener('click', () => $('.sidebar').classList.toggle('open'));
  document.addEventListener('keydown', event => { if (event.key === 'Escape') $('.sidebar').classList.remove('open'); });

  refresh().catch(error => {
    $('#loading').hidden = true;
    showError(error);
    $('#view-inicio').hidden = false;
    $('#view-inicio').classList.add('active');
  });
})();
