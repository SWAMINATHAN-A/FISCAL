/**
 * FISCAL — Custom Tax Management System
 * Master Application Coordinator & User Experience Logic
 * Brand Tagline: Clarity in every calculation.
 */
(function() {
  'use strict';

  // Application State
  const state = {
    currentUser: null,
    currentView: 'overview',
    taxpayers: [],
    taxTypes: [],
    payments: [],
    dashboard: null,
    cities: [],
    filters: {
      taxpayerSearch: '',
      taxpayerCity: 'ALL',
      taxpayerSort: 'id_asc',
      paymentSearch: '',
      paymentStatus: 'ALL',
      paymentTaxType: 'ALL',
      paymentSort: 'date_desc'
    }
  };

  // Helper: Format Currency in INR
  function formatINR(val) {
    if (val === null || val === undefined || isNaN(val)) return '₹0.00';
    const num = Number(val);
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(num);
  }

  // Toast Notification System
  function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let icon = 'ℹ️';
    if (type === 'success') icon = '✓';
    if (type === 'error') icon = '✕';

    toast.innerHTML = `<span style="font-weight:700;">${icon}</span> <span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(10px)';
      toast.style.transition = 'all 0.2s ease';
      setTimeout(() => toast.remove(), 250);
    }, 4000);
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  // Modal Manager
  function openModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.add('active');
  }

  function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.remove('active');
  }

  // View Switching
  function navigateTo(viewName) {
    state.currentView = viewName;
    
    // Update navigation active state
    document.querySelectorAll('.nav-item').forEach(item => {
      if (item.getAttribute('data-view') === viewName) {
        item.classList.add('active');
      } else {
        item.classList.remove('active');
      }
    });

    // Hide all view sections and show target
    document.querySelectorAll('.view-section').forEach(sec => {
      sec.style.display = 'none';
    });

    const target = document.getElementById(`view-${viewName}`);
    if (target) {
      target.style.display = 'block';
    }

    // Refresh view data
    switch(viewName) {
      case 'overview':
        loadDashboard();
        break;
      case 'taxpayers':
        loadTaxpayers();
        break;
      case 'tax-types':
        loadTaxTypes();
        break;
      case 'assessment':
        loadAssessmentWorkbench();
        break;
      case 'payments':
        loadPayments();
        break;
      case 'users':
        loadUsers();
        break;
      case 'about':
        // Static academic & project presentation view
        break;
    }
  }

  // --------------------------------------------------------------------------
  // Authentication & Session
  // --------------------------------------------------------------------------
  async function checkInitialAuth() {
    try {
      const res = await FiscalAPI.checkStatus();
      if (res && res.userId) {
        setAuthenticatedUser(res);
      } else {
        showLoginView();
      }
    } catch {
      showLoginView();
    }
  }

  function setAuthenticatedUser(user) {
    state.currentUser = user;
    document.getElementById('login-container').style.display = 'none';
    document.getElementById('app-container').style.display = 'flex';

    // Update Header
    const nameEl = document.getElementById('header-user-name');
    const roleEl = document.getElementById('header-user-role');
    const avatarEl = document.getElementById('header-user-avatar');

    if (nameEl) nameEl.textContent = user.fullName || user.username;
    if (roleEl) roleEl.textContent = user.role || 'OFFICER';
    if (avatarEl) avatarEl.textContent = (user.fullName || user.username).substring(0, 2).toUpperCase();

    // Toggle Admin Navigation
    const usersNavItem = document.getElementById('nav-item-users');
    if (usersNavItem) {
      usersNavItem.style.display = (user.role === 'ADMIN') ? 'inline-flex' : 'none';
    }

    navigateTo('overview');
  }

  function showLoginView() {
    state.currentUser = null;
    document.getElementById('app-container').style.display = 'none';
    document.getElementById('login-container').style.display = 'flex';
  }

  async function handleLoginSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('btn-submit-login');
    const usernameInput = document.getElementById('login-username');
    const passwordInput = document.getElementById('login-password');
    const errorBox = document.getElementById('login-error-message');

    if (errorBox) errorBox.style.display = 'none';
    if (btn) {
      btn.disabled = true;
      btn.textContent = 'Verifying credentials...';
    }

    try {
      const user = await FiscalAPI.login(usernameInput.value.trim(), passwordInput.value);
      showToast(`Welcome back, ${user.fullName}`, 'success');
      setAuthenticatedUser(user);
    } catch (err) {
      if (errorBox) {
        errorBox.textContent = err.message || 'Invalid username or password.';
        errorBox.style.display = 'block';
      }
      showToast(err.message, 'error');
    } finally {
      if (btn) {
        btn.disabled = false;
        btn.textContent = 'Sign In to Workspace';
      }
    }
  }

  async function handleLogout() {
    try {
      await FiscalAPI.logout();
      showToast('You have been logged out securely.', 'info');
    } catch (err) {
      console.warn(err);
    } finally {
      showLoginView();
    }
  }

  // --------------------------------------------------------------------------
  // Dashboard Overview
  // --------------------------------------------------------------------------
  async function loadDashboard() {
    try {
      const data = await FiscalAPI.getDashboard();
      state.dashboard = data;
      renderDashboard(data);
    } catch (err) {
      showToast(`Error loading dashboard: ${err.message}`, 'error');
    }
  }

  function renderDashboard(d) {
    if (!d) return;

    // Metrics
    document.getElementById('metric-total-taxpayers').textContent = d.totalTaxpayers || 0;
    document.getElementById('metric-total-payments').textContent = d.totalPayments || 0;
    document.getElementById('metric-total-paid').textContent = formatINR(d.totalPaidRevenue);
    document.getElementById('metric-total-pending').textContent = formatINR(d.totalPendingAmount);
    document.getElementById('metric-gross-income').textContent = formatINR(d.grossTurnoverMonitored);

    // Status breakdown badges
    const dist = d.statusDistribution || {};
    document.getElementById('dist-paid-count').textContent = dist.PAID || 0;
    document.getElementById('dist-pending-count').textContent = dist.PENDING || 0;
    document.getElementById('dist-reversed-count').textContent = dist.REVERSED || 0;

    // Category progress bars
    const catList = document.getElementById('dashboard-category-bars');
    if (catList && d.taxCategoriesBreakdown) {
      catList.innerHTML = '';
      const totalCollectedAll = d.totalPaidRevenue || 1;

      d.taxCategoriesBreakdown.forEach(cat => {
        const collected = Number(cat.totalCollected || 0);
        const percent = Math.min(100, Math.round((collected / (totalCollectedAll || 1)) * 100));

        const item = document.createElement('div');
        item.className = 'category-bar-item';
        item.innerHTML = `
          <div class="category-bar-label-row">
            <span>${escapeHtml(cat.taxName)} <span class="badge badge-category" style="font-size:10px;">${cat.taxRate}%</span></span>
            <span class="mono">${formatINR(collected)}</span>
          </div>
          <div class="category-progress-track">
            <div class="category-progress-fill" style="width: ${percent}%;"></div>
          </div>
        `;
        catList.appendChild(item);
      });
    }

    // Recent Payments Table
    const recentTable = document.getElementById('dashboard-recent-payments-tbody');
    if (recentTable) {
      recentTable.innerHTML = '';
      if (!d.recentPayments || d.recentPayments.length === 0) {
        recentTable.innerHTML = '<tr><td colspan="6" style="text-align:center; padding:32px; color:var(--muted);">No payment transactions recorded yet.</td></tr>';
      } else {
        d.recentPayments.forEach(p => {
          const row = document.createElement('tr');
          row.innerHTML = `
            <td class="mono font-technical" style="font-weight:700;">${escapeHtml(p.paymentRef)}</td>
            <td><strong>${escapeHtml(p.taxpayerName)}</strong></td>
            <td><span class="badge badge-category">${escapeHtml(p.taxName)}</span></td>
            <td class="mono" style="font-weight:700;">${formatINR(p.amount)}</td>
            <td class="mono text-muted">${p.paymentDate || '—'}</td>
            <td><span class="badge badge-${(p.status || 'paid').toLowerCase()}">${p.status}</span></td>
          `;
          recentTable.appendChild(row);
        });
      }
    }
  }

  // --------------------------------------------------------------------------
  // Taxpayer Management
  // --------------------------------------------------------------------------
  async function loadTaxpayers() {
    try {
      // Load city filter options if not loaded
      if (state.cities.length === 0) {
        state.cities = await FiscalAPI.getCities();
        const citySelect = document.getElementById('filter-taxpayer-city');
        if (citySelect) {
          citySelect.innerHTML = '<option value="ALL">All Cities</option>';
          state.cities.forEach(c => {
            citySelect.innerHTML += `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`;
          });
        }
      }

      const list = await FiscalAPI.getTaxpayers({
        search: state.filters.taxpayerSearch,
        city: state.filters.taxpayerCity,
        sort: state.filters.taxpayerSort
      });
      state.taxpayers = list;
      renderTaxpayersTable(list);
    } catch (err) {
      showToast(`Error fetching taxpayers: ${err.message}`, 'error');
    }
  }

  function renderTaxpayersTable(taxpayers) {
    const tbody = document.getElementById('taxpayers-tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (!taxpayers || taxpayers.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding:36px; color:var(--muted);">No taxpayers match the active search criteria.</td></tr>';
      return;
    }

    taxpayers.forEach(t => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td class="mono font-technical" style="font-weight:700;">#${t.taxpayerId}</td>
        <td>
          <div style="font-weight:700; color:var(--ink);">${escapeHtml(t.name)}</div>
          <div style="font-size:11px; color:var(--muted);">${t.age} yrs • ${escapeHtml(t.city)}</div>
        </td>
        <td class="mono font-technical" title="${t.panNo}">${escapeHtml(t.maskedPan || t.panNo)}</td>
        <td>${escapeHtml(t.city)}</td>
        <td class="mono" style="font-weight:600;">${formatINR(t.income)}</td>
        <td class="mono text-muted-strong">${formatINR(t.totalPaid || 0)}</td>
        <td class="mono" style="text-align:center;"><span class="badge badge-category">${t.paymentCount || 0}</span></td>
        <td style="text-align:right;">
          <div style="display:inline-flex; gap:6px;">
            <button class="btn btn-secondary btn-sm" onclick="FiscalApp.viewTaxpayerDetails(${t.taxpayerId})">Details</button>
            <button class="btn btn-secondary btn-sm" onclick="FiscalApp.openEditTaxpayer(${t.taxpayerId})">Edit</button>
            <button class="btn btn-danger btn-sm" onclick="FiscalApp.deleteTaxpayer(${t.taxpayerId})">Delete</button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  }

  async function viewTaxpayerDetails(id) {
    try {
      const t = await FiscalAPI.getTaxpayer(id);
      const payments = await FiscalAPI.getPayments({ taxpayerId: id });

      document.getElementById('modal-taxpayer-name').textContent = t.name;
      document.getElementById('detail-tp-id').textContent = '#' + t.taxpayerId;
      document.getElementById('detail-tp-pan').textContent = t.panNo;
      document.getElementById('detail-tp-age').textContent = t.age + ' years';
      document.getElementById('detail-tp-city').textContent = t.city;
      document.getElementById('detail-tp-income').textContent = formatINR(t.income);
      document.getElementById('detail-tp-totalpaid').textContent = formatINR(t.totalPaid || 0);

      // Render payments table for this taxpayer
      const tbody = document.getElementById('detail-tp-payments-tbody');
      tbody.innerHTML = '';
      if (!payments || payments.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; padding:20px; color:var(--muted);">No payment history for this taxpayer.</td></tr>';
      } else {
        payments.forEach(p => {
          tbody.innerHTML += `
            <tr>
              <td class="mono">${escapeHtml(p.paymentRef)}</td>
              <td>${escapeHtml(p.taxName)}</td>
              <td class="mono">${formatINR(p.amount)}</td>
              <td class="mono text-muted">${p.paymentDate}</td>
              <td><span class="badge badge-${p.status.toLowerCase()}">${p.status}</span></td>
            </tr>
          `;
        });
      }

      openModal('modal-taxpayer-details');
    } catch (err) {
      showToast(`Error fetching details: ${err.message}`, 'error');
    }
  }

  async function openEditTaxpayer(id) {
    const t = state.taxpayers.find(item => item.taxpayerId === id);
    if (!t) return;

    document.getElementById('form-edit-taxpayer-id').value = t.taxpayerId;
    document.getElementById('form-edit-taxpayer-name').value = t.name;
    document.getElementById('form-edit-taxpayer-pan').value = t.panNo;
    document.getElementById('form-edit-taxpayer-age').value = t.age;
    document.getElementById('form-edit-taxpayer-city').value = t.city;
    document.getElementById('form-edit-taxpayer-income').value = t.income;

    openModal('modal-edit-taxpayer');
  }

  async function handleCreateTaxpayer(e) {
    e.preventDefault();
    const data = {
      name: document.getElementById('form-new-taxpayer-name').value.trim(),
      panNo: document.getElementById('form-new-taxpayer-pan').value.trim().toUpperCase(),
      age: parseInt(document.getElementById('form-new-taxpayer-age').value, 10),
      city: document.getElementById('form-new-taxpayer-city').value.trim(),
      income: parseFloat(document.getElementById('form-new-taxpayer-income').value)
    };

    try {
      const created = await FiscalAPI.createTaxpayer(data);
      showToast(`Taxpayer ${created.name} registered successfully`, 'success');
      closeModal('modal-add-taxpayer');
      document.getElementById('form-add-taxpayer').reset();
      loadTaxpayers();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  async function handleUpdateTaxpayer(e) {
    e.preventDefault();
    const id = document.getElementById('form-edit-taxpayer-id').value;
    const data = {
      name: document.getElementById('form-edit-taxpayer-name').value.trim(),
      panNo: document.getElementById('form-edit-taxpayer-pan').value.trim().toUpperCase(),
      age: parseInt(document.getElementById('form-edit-taxpayer-age').value, 10),
      city: document.getElementById('form-edit-taxpayer-city').value.trim(),
      income: parseFloat(document.getElementById('form-edit-taxpayer-income').value)
    };

    try {
      await FiscalAPI.updateTaxpayer(id, data);
      showToast(`Taxpayer #${id} updated successfully`, 'success');
      closeModal('modal-edit-taxpayer');
      loadTaxpayers();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  async function deleteTaxpayer(id) {
    if (!confirm(`Are you sure you want to delete Taxpayer #${id}? All associated payment records will also be removed pursuant to foreign key cascades.`)) {
      return;
    }
    try {
      await FiscalAPI.deleteTaxpayer(id);
      showToast(`Taxpayer #${id} deleted successfully`, 'success');
      loadTaxpayers();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  // --------------------------------------------------------------------------
  // Tax Type & Rate Management
  // --------------------------------------------------------------------------
  async function loadTaxTypes() {
    try {
      const list = await FiscalAPI.getTaxTypes();
      state.taxTypes = list;
      renderTaxTypesGrid(list);
    } catch (err) {
      showToast(`Error fetching tax categories: ${err.message}`, 'error');
    }
  }

  function renderTaxTypesGrid(types) {
    const grid = document.getElementById('taxtypes-grid');
    if (!grid) return;
    grid.innerHTML = '';

    if (!types || types.length === 0) {
      grid.innerHTML = '<div style="padding:40px; color:var(--muted); text-align:center;">No tax types configured yet.</div>';
      return;
    }

    types.forEach(tt => {
      const card = document.createElement('div');
      card.className = 'tactile-surface tactile-surface-hover';
      card.style.padding = 'var(--space-6)';
      card.style.display = 'flex';
      card.style.flexDirection = 'column';
      card.style.justifyContent = 'space-between';
      card.style.gap = 'var(--space-4)';

      card.innerHTML = `
        <div>
          <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:8px;">
            <span class="mono font-technical" style="font-size:11px; color:var(--muted-strong);">CODE #${tt.taxId}</span>
            <span class="badge badge-category" style="font-size:13px; font-weight:800;">${tt.taxRate}%</span>
          </div>
          <h3 style="font-size:18px; font-weight:700; margin-bottom:8px;">${escapeHtml(tt.taxName)}</h3>
          <p style="font-size:13px; color:var(--muted-strong); line-height:1.5;">${escapeHtml(tt.description || 'Statutory tax rule category')}</p>
        </div>

        <div style="border-top:1px solid var(--border); padding-top:12px; display:flex; justify-content:space-between; align-items:center;">
          <div>
            <div style="font-size:10px; font-family:var(--font-technical); color:var(--muted);">TOTAL COLLECTED</div>
            <div class="mono" style="font-weight:700; font-size:15px;">${formatINR(tt.totalCollected || 0)}</div>
          </div>
          <div style="display:flex; gap:6px;">
            <button class="btn btn-secondary btn-sm" onclick="FiscalApp.openEditTaxType(${tt.taxId})">Edit</button>
            <button class="btn btn-danger btn-sm" onclick="FiscalApp.deleteTaxType(${tt.taxId})">Delete</button>
          </div>
        </div>
      `;
      grid.appendChild(card);
    });
  }

  async function handleCreateTaxType(e) {
    e.preventDefault();
    const data = {
      taxName: document.getElementById('form-new-taxtype-name').value.trim(),
      taxRate: parseFloat(document.getElementById('form-new-taxtype-rate').value),
      description: document.getElementById('form-new-taxtype-desc').value.trim()
    };

    try {
      const created = await FiscalAPI.createTaxType(data);
      showToast(`Tax category '${created.taxName}' created successfully`, 'success');
      closeModal('modal-add-taxtype');
      document.getElementById('form-add-taxtype').reset();
      loadTaxTypes();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  async function openEditTaxType(id) {
    const tt = state.taxTypes.find(item => item.taxId === id);
    if (!tt) return;

    document.getElementById('form-edit-taxtype-id').value = tt.taxId;
    document.getElementById('form-edit-taxtype-name').value = tt.taxName;
    document.getElementById('form-edit-taxtype-rate').value = tt.taxRate;
    document.getElementById('form-edit-taxtype-desc').value = tt.description || '';

    openModal('modal-edit-taxtype');
  }

  async function handleUpdateTaxType(e) {
    e.preventDefault();
    const id = document.getElementById('form-edit-taxtype-id').value;
    const data = {
      taxName: document.getElementById('form-edit-taxtype-name').value.trim(),
      taxRate: parseFloat(document.getElementById('form-edit-taxtype-rate').value),
      description: document.getElementById('form-edit-taxtype-desc').value.trim()
    };

    try {
      await FiscalAPI.updateTaxType(id, data);
      showToast(`Tax category #${id} updated successfully`, 'success');
      closeModal('modal-edit-taxtype');
      loadTaxTypes();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  async function deleteTaxType(id) {
    if (!confirm(`Are you sure you want to delete Tax Category #${id}? This operation will be rejected if existing payments reference it.`)) {
      return;
    }
    try {
      await FiscalAPI.deleteTaxType(id);
      showToast(`Tax category #${id} deleted successfully`, 'success');
      loadTaxTypes();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  // --------------------------------------------------------------------------
  // Tax Calculator & Assessment Workbench
  // --------------------------------------------------------------------------
  async function loadAssessmentWorkbench() {
    try {
      const [taxpayers, types] = await Promise.all([
        FiscalAPI.getTaxpayers(),
        FiscalAPI.getTaxTypes()
      ]);

      const tpSelect = document.getElementById('calc-taxpayer-select');
      const ttSelect = document.getElementById('calc-taxtype-select');

      tpSelect.innerHTML = '<option value="">Select a registered taxpayer...</option>';
      taxpayers.forEach(t => {
        tpSelect.innerHTML += `<option value="${t.taxpayerId}" data-income="${t.income}">${escapeHtml(t.name)} (PAN: ${t.maskedPan || t.panNo} • Income: ${formatINR(t.income)})</option>`;
      });

      ttSelect.innerHTML = '<option value="">Select statutory tax rule...</option>';
      types.forEach(tt => {
        ttSelect.innerHTML += `<option value="${tt.taxId}" data-rate="${tt.taxRate}">${escapeHtml(tt.taxName)} (${tt.taxRate}%)</option>`;
      });

    } catch (err) {
      showToast(`Error initializing calculator: ${err.message}`, 'error');
    }
  }

  function handleCalcTaxpayerChange() {
    const tpSelect = document.getElementById('calc-taxpayer-select');
    const selected = tpSelect.options[tpSelect.selectedIndex];
    const amountInput = document.getElementById('calc-taxable-amount');

    if (selected && selected.getAttribute('data-income')) {
      amountInput.value = selected.getAttribute('data-income');
    }
  }

  async function handlePerformCalculation(e) {
    e.preventDefault();
    const tpId = document.getElementById('calc-taxpayer-select').value;
    const ttId = document.getElementById('calc-taxtype-select').value;
    const customAmount = document.getElementById('calc-taxable-amount').value;

    if (!tpId || !ttId) {
      showToast('Please select both a taxpayer and a tax rule category.', 'error');
      return;
    }

    const payload = {
      taxpayerId: parseInt(tpId, 10),
      taxId: parseInt(ttId, 10),
      taxableAmount: customAmount ? parseFloat(customAmount) : null
    };

    try {
      const res = await FiscalAPI.calculateTax(payload);
      renderCalculationResult(res);
      showToast('Statutory tax assessment calculated successfully', 'success');
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  let latestCalcResult = null;

  function renderCalculationResult(res) {
    latestCalcResult = res;
    const panel = document.getElementById('calc-result-panel');
    panel.style.display = 'block';

    document.getElementById('calc-res-amount').textContent = formatINR(res.calculatedTax);
    document.getElementById('calc-res-formula').textContent = res.calculationFormula;
    document.getElementById('calc-res-tp-name').textContent = res.taxpayerName;
    document.getElementById('calc-res-tp-pan').textContent = res.panNo;
    document.getElementById('calc-res-tt-name').textContent = res.taxName;
    document.getElementById('calc-res-rate').textContent = res.taxRate + '%';
    document.getElementById('calc-res-applicable').textContent = formatINR(res.applicableAmount);
    document.getElementById('calc-res-timestamp').textContent = res.timestamp;
  }

  function convertCalcToPayment() {
    if (!latestCalcResult) return;
    openRecordPaymentModalWithPreFill({
      taxpayerId: latestCalcResult.taxpayerId,
      taxId: latestCalcResult.taxId,
      amount: latestCalcResult.calculatedTax,
      remarks: `Assessment calculated on ${latestCalcResult.timestamp}`
    });
  }

  // --------------------------------------------------------------------------
  // Payment Management & Ledger
  // --------------------------------------------------------------------------
  async function loadPayments() {
    try {
      // Pre-fill tax type filter dropdown if needed
      if (state.taxTypes.length === 0) {
        state.taxTypes = await FiscalAPI.getTaxTypes();
      }
      const filterSelect = document.getElementById('filter-payment-taxtype');
      if (filterSelect && filterSelect.children.length <= 1) {
        filterSelect.innerHTML = '<option value="ALL">All Categories</option>';
        state.taxTypes.forEach(tt => {
          filterSelect.innerHTML += `<option value="${tt.taxId}">${escapeHtml(tt.taxName)}</option>`;
        });
      }

      const list = await FiscalAPI.getPayments({
        search: state.filters.paymentSearch,
        taxId: state.filters.paymentTaxType !== 'ALL' ? state.filters.paymentTaxType : null,
        status: state.filters.paymentStatus !== 'ALL' ? state.filters.paymentStatus : null,
        sort: state.filters.paymentSort
      });
      state.payments = list;
      renderPaymentsTable(list);
    } catch (err) {
      showToast(`Error fetching payment ledger: ${err.message}`, 'error');
    }
  }

  function renderPaymentsTable(payments) {
    const tbody = document.getElementById('payments-tbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (!payments || payments.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:36px; color:var(--muted);">No payment entries found matching the filter criteria.</td></tr>';
      return;
    }

    payments.forEach(p => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td class="mono font-technical" style="font-weight:700;">${escapeHtml(p.paymentRef)}</td>
        <td>
          <div style="font-weight:700;">${escapeHtml(p.taxpayerName)}</div>
          <div class="mono" style="font-size:11px; color:var(--muted);">${escapeHtml(p.maskedPan || p.taxpayerPan)}</div>
        </td>
        <td><span class="badge badge-category">${escapeHtml(p.taxName)}</span></td>
        <td class="mono" style="font-weight:700; font-size:15px;">${formatINR(p.amount)}</td>
        <td class="mono text-muted">${p.paymentDate}</td>
        <td><span class="badge badge-${p.status.toLowerCase()}">${p.status}</span></td>
        <td style="text-align:right;">
          <div style="display:inline-flex; gap:6px;">
            <button class="btn btn-secondary btn-sm" onclick="FiscalApp.openStatusModal(${p.paymentId}, '${p.status}')">Update Status</button>
            <button class="btn btn-danger btn-sm" onclick="FiscalApp.deletePayment(${p.paymentId})">Delete</button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  }

  async function openRecordPaymentModalWithPreFill(prefill = {}) {
    const [taxpayers, types] = await Promise.all([
      FiscalAPI.getTaxpayers(),
      FiscalAPI.getTaxTypes()
    ]);

    const tpSelect = document.getElementById('form-payment-taxpayer-id');
    const ttSelect = document.getElementById('form-payment-tax-id');

    tpSelect.innerHTML = '<option value="">Select Taxpayer...</option>';
    taxpayers.forEach(t => {
      tpSelect.innerHTML += `<option value="${t.taxpayerId}">${escapeHtml(t.name)} (ID: #${t.taxpayerId})</option>`;
    });

    ttSelect.innerHTML = '<option value="">Select Tax Category...</option>';
    types.forEach(tt => {
      ttSelect.innerHTML += `<option value="${tt.taxId}">${escapeHtml(tt.taxName)} (${tt.taxRate}%)</option>`;
    });

    if (prefill.taxpayerId) tpSelect.value = prefill.taxpayerId;
    if (prefill.taxId) ttSelect.value = prefill.taxId;
    if (prefill.amount) document.getElementById('form-payment-amount').value = prefill.amount;
    if (prefill.remarks) document.getElementById('form-payment-remarks').value = prefill.remarks;

    openModal('modal-add-payment');
  }

  async function handleRecordPayment(e) {
    e.preventDefault();
    const data = {
      taxpayerId: parseInt(document.getElementById('form-payment-taxpayer-id').value, 10),
      taxId: parseInt(document.getElementById('form-payment-tax-id').value, 10),
      amount: parseFloat(document.getElementById('form-payment-amount').value),
      status: document.getElementById('form-payment-status').value,
      remarks: document.getElementById('form-payment-remarks').value.trim()
    };

    try {
      const created = await FiscalAPI.recordPayment(data);
      showToast(`Payment ${created.paymentRef} committed into ledger`, 'success');
      closeModal('modal-add-payment');
      document.getElementById('form-add-payment').reset();
      loadPayments();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  function openStatusModal(paymentId, currentStatus) {
    document.getElementById('form-status-payment-id').value = paymentId;
    document.getElementById('form-status-select').value = currentStatus;
    document.getElementById('form-status-remarks').value = '';
    openModal('modal-update-status');
  }

  async function handleStatusUpdate(e) {
    e.preventDefault();
    const paymentId = document.getElementById('form-status-payment-id').value;
    const status = document.getElementById('form-status-select').value;
    const remarks = document.getElementById('form-status-remarks').value.trim();

    try {
      await FiscalAPI.updatePaymentStatus(paymentId, status, remarks);
      showToast(`Payment #${paymentId} updated to ${status}`, 'success');
      closeModal('modal-update-status');
      loadPayments();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  async function deletePayment(id) {
    if (!confirm(`Are you sure you want to remove payment record #${id} from the ledger?`)) {
      return;
    }
    try {
      await FiscalAPI.deletePayment(id);
      showToast(`Payment #${id} deleted from ledger`, 'success');
      loadPayments();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  // --------------------------------------------------------------------------
  // User Management (Admin Only)
  // --------------------------------------------------------------------------
  async function loadUsers() {
    try {
      const users = await FiscalAPI.getUsers();
      const tbody = document.getElementById('users-tbody');
      if (!tbody) return;
      tbody.innerHTML = '';

      users.forEach(u => {
        tbody.innerHTML += `
          <tr>
            <td class="mono font-technical">#${u.userId}</td>
            <td><strong>${escapeHtml(u.fullName)}</strong></td>
            <td class="mono">${escapeHtml(u.username)}</td>
            <td>${escapeHtml(u.email)}</td>
            <td><span class="badge badge-category">${u.role}</span></td>
            <td><span class="badge badge-paid">${u.status}</span></td>
            <td class="mono text-muted">${u.createdAt || '—'}</td>
          </tr>
        `;
      });
    } catch (err) {
      showToast(`Error fetching operator roster: ${err.message}`, 'error');
    }
  }

  async function handleCreateUser(e) {
    e.preventDefault();
    const data = {
      username: document.getElementById('form-new-user-username').value.trim(),
      password: document.getElementById('form-new-user-password').value,
      fullName: document.getElementById('form-new-user-fullname').value.trim(),
      email: document.getElementById('form-new-user-email').value.trim(),
      role: document.getElementById('form-new-user-role').value
    };

    try {
      const created = await FiscalAPI.createUser(data);
      showToast(`User account '${created.username}' created successfully`, 'success');
      closeModal('modal-add-user');
      document.getElementById('form-add-user').reset();
      loadUsers();
    } catch (err) {
      showToast(err.message, 'error');
    }
  }

  // --------------------------------------------------------------------------
  // Event Listeners Initialization
  // --------------------------------------------------------------------------
  function initEventListeners() {
    // Login form
    const loginForm = document.getElementById('form-login');
    if (loginForm) loginForm.addEventListener('submit', handleLoginSubmit);

    // Logout
    const logoutBtn = document.getElementById('btn-logout');
    if (logoutBtn) logoutBtn.addEventListener('click', handleLogout);

    // Demo chip quick-fill
    document.querySelectorAll('.demo-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        const u = chip.getAttribute('data-user');
        const p = chip.getAttribute('data-pass');
        const uInput = document.getElementById('login-username');
        const pInput = document.getElementById('login-password');
        if (uInput) uInput.value = u;
        if (pInput) pInput.value = p;
      });
    });

    // Navigation links
    document.querySelectorAll('.nav-item').forEach(item => {
      item.addEventListener('click', (e) => {
        e.preventDefault();
        const v = item.getAttribute('data-view');
        if (v) navigateTo(v);
      });
    });

    // Filter Listeners: Taxpayers
    const tpSearch = document.getElementById('filter-taxpayer-search');
    if (tpSearch) {
      tpSearch.addEventListener('input', debounce(() => {
        state.filters.taxpayerSearch = tpSearch.value.trim();
        loadTaxpayers();
      }, 300));
    }
    const tpCity = document.getElementById('filter-taxpayer-city');
    if (tpCity) {
      tpCity.addEventListener('change', () => {
        state.filters.taxpayerCity = tpCity.value;
        loadTaxpayers();
      });
    }
    const tpSort = document.getElementById('filter-taxpayer-sort');
    if (tpSort) {
      tpSort.addEventListener('change', () => {
        state.filters.taxpayerSort = tpSort.value;
        loadTaxpayers();
      });
    }

    // Filter Listeners: Payments
    const paySearch = document.getElementById('filter-payment-search');
    if (paySearch) {
      paySearch.addEventListener('input', debounce(() => {
        state.filters.paymentSearch = paySearch.value.trim();
        loadPayments();
      }, 300));
    }
    const payStatus = document.getElementById('filter-payment-status');
    if (payStatus) {
      payStatus.addEventListener('change', () => {
        state.filters.paymentStatus = payStatus.value;
        loadPayments();
      });
    }
    const payCategory = document.getElementById('filter-payment-taxtype');
    if (payCategory) {
      payCategory.addEventListener('change', () => {
        state.filters.paymentTaxType = payCategory.value;
        loadPayments();
      });
    }
    const paySort = document.getElementById('filter-payment-sort');
    if (paySort) {
      paySort.addEventListener('change', () => {
        state.filters.paymentSort = paySort.value;
        loadPayments();
      });
    }

    // Modal submit handlers
    const addTpForm = document.getElementById('form-add-taxpayer');
    if (addTpForm) addTpForm.addEventListener('submit', handleCreateTaxpayer);

    const editTpForm = document.getElementById('form-edit-taxpayer');
    if (editTpForm) editTpForm.addEventListener('submit', handleUpdateTaxpayer);

    const addTtForm = document.getElementById('form-add-taxtype');
    if (addTtForm) addTtForm.addEventListener('submit', handleCreateTaxType);

    const editTtForm = document.getElementById('form-edit-taxtype');
    if (editTtForm) editTtForm.addEventListener('submit', handleUpdateTaxType);

    const addPayForm = document.getElementById('form-add-payment');
    if (addPayForm) addPayForm.addEventListener('submit', handleRecordPayment);

    const updateStatusForm = document.getElementById('form-update-status');
    if (updateStatusForm) updateStatusForm.addEventListener('submit', handleStatusUpdate);

    const addUserForm = document.getElementById('form-add-user');
    if (addUserForm) addUserForm.addEventListener('submit', handleCreateUser);

    const calcForm = document.getElementById('form-calculate-tax');
    if (calcForm) calcForm.addEventListener('submit', handlePerformCalculation);

    const calcTpSelect = document.getElementById('calc-taxpayer-select');
    if (calcTpSelect) calcTpSelect.addEventListener('change', handleCalcTaxpayerChange);

    // Global Unauthorized event
    window.addEventListener('fiscal:unauthorized', () => {
      showToast('Session has ended. Please sign in.', 'info');
      showLoginView();
    });
  }

  function debounce(fn, ms) {
    let timer;
    return function(...args) {
      clearTimeout(timer);
      timer = setTimeout(() => fn.apply(this, args), ms);
    };
  }

  // Bootstrapping
  document.addEventListener('DOMContentLoaded', () => {
    initEventListeners();
    checkInitialAuth();
  });

  // Public API attached to window
  window.FiscalApp = {
    navigateTo,
    openModal,
    closeModal,
    viewTaxpayerDetails,
    openEditTaxpayer,
    deleteTaxpayer,
    openEditTaxType,
    deleteTaxType,
    openStatusModal,
    deletePayment,
    openRecordPaymentModalWithPreFill,
    convertCalcToPayment
  };

})();
