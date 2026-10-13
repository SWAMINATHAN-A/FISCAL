/**
 * FISCAL — Custom Tax Management System
 * Client-Side API Communication Layer (Fetch API abstraction)
 */
const FiscalAPI = (function() {
  'use strict';

  const BASE_URL = ''; // Relative path compatible with contextPath or root deployment

  async function request(endpoint, options = {}) {
    const config = {
      method: options.method || 'GET',
      headers: {
        'Accept': 'application/json',
        ...(options.headers || {})
      },
      credentials: 'same-origin'
    };

    if (options.body) {
      config.headers['Content-Type'] = 'application/json;charset=UTF-8';
      config.body = JSON.stringify(options.body);
    }

    try {
      const response = await fetch(`${BASE_URL}${endpoint}`, config);
      const data = await response.json().catch(() => null);

      if (!response.ok) {
        const errorMsg = (data && data.message) ? data.message : `HTTP ${response.status}: ${response.statusText}`;
        if (response.status === 401 && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/status')) {
          // Trigger session expiration event
          window.dispatchEvent(new CustomEvent('fiscal:unauthorized'));
        }
        throw new Error(errorMsg);
      }

      return data ? data.data : null;
    } catch (err) {
      console.warn(`[FiscalAPI Error] ${endpoint}:`, err.message);
      throw err;
    }
  }

  return {
    // Authentication
    login: (username, password) => request('/api/auth/login', { method: 'POST', body: { username, password } }),
    logout: () => request('/api/auth/logout', { method: 'POST' }),
    checkStatus: () => request('/api/auth/status'),
    getMe: () => request('/api/auth/me'),

    // Dashboard
    getDashboard: () => request('/api/dashboard/summary'),

    // Taxpayers
    getTaxpayers: (params = {}) => {
      const qs = new URLSearchParams();
      if (params.search) qs.append('search', params.search);
      if (params.city) qs.append('city', params.city);
      if (params.sort) qs.append('sort', params.sort);
      return request(`/api/taxpayers${qs.toString() ? '?' + qs.toString() : ''}`);
    },
    getTaxpayer: (id) => request(`/api/taxpayers/${id}`),
    getCities: () => request('/api/taxpayers/cities'),
    createTaxpayer: (data) => request('/api/taxpayers', { method: 'POST', body: data }),
    updateTaxpayer: (id, data) => request(`/api/taxpayers/${id}`, { method: 'PUT', body: data }),
    deleteTaxpayer: (id) => request(`/api/taxpayers/${id}`, { method: 'DELETE' }),

    // Tax Types
    getTaxTypes: (params = {}) => {
      const qs = new URLSearchParams();
      if (params.search) qs.append('search', params.search);
      return request(`/api/tax-types${qs.toString() ? '?' + qs.toString() : ''}`);
    },
    getTaxType: (id) => request(`/api/tax-types/${id}`),
    createTaxType: (data) => request('/api/tax-types', { method: 'POST', body: data }),
    updateTaxType: (id, data) => request(`/api/tax-types/${id}`, { method: 'PUT', body: data }),
    deleteTaxType: (id) => request(`/api/tax-types/${id}`, { method: 'DELETE' }),

    // Tax Calculation & Assessments
    calculateTax: (data) => request('/api/tax/calculate', { method: 'POST', body: data }),
    getTaxRecords: (params = {}) => {
      const qs = new URLSearchParams();
      if (params.search) qs.append('search', params.search);
      if (params.taxpayerId) qs.append('taxpayerId', params.taxpayerId);
      if (params.taxTypeId) qs.append('taxTypeId', params.taxTypeId);
      if (params.status) qs.append('status', params.status);
      return request(`/api/tax-records${qs.toString() ? '?' + qs.toString() : ''}`);
    },

    // Payments Ledger
    getPayments: (params = {}) => {
      const qs = new URLSearchParams();
      if (params.search) qs.append('search', params.search);
      if (params.taxpayerId) qs.append('taxpayerId', params.taxpayerId);
      if (params.taxId) qs.append('taxId', params.taxId);
      if (params.status) qs.append('status', params.status);
      if (params.sort) qs.append('sort', params.sort);
      return request(`/api/payments${qs.toString() ? '?' + qs.toString() : ''}`);
    },
    getPayment: (id) => request(`/api/payments/${id}`),
    recordPayment: (data) => request('/api/payments', { method: 'POST', body: data }),
    updatePaymentStatus: (id, status, remarks) => request(`/api/payments/${id}/status`, { method: 'POST', body: { status, remarks } }),
    deletePayment: (id) => request(`/api/payments/${id}`, { method: 'DELETE' }),

    // User Administration
    getUsers: () => request('/api/users'),
    createUser: (data) => request('/api/users', { method: 'POST', body: data })
  };
})();
