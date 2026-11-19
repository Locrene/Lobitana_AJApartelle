import { api } from './api';

export const authService = {
  register: (data) => api.post('/auth/register', data),
  login: (email, password) => api.post('/auth/login', { email, password }),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me'),
};

export const dashboardService = {
  guest: () => api.get('/guest/dashboard'),
  staff: () => api.get('/staff/dashboard'),
  owner: () => api.get('/owner/dashboard'),
};

export const staffAccountService = {
  list: () => api.get('/owner/staff'),
  create: (data) => api.post('/owner/staff', data),
  setActive: (id, active) => api.patch(`/owner/staff/${id}`, { active }),
};
