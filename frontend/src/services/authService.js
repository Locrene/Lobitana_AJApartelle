import { api } from './api';

export const authService = {
  register: (data) => api.post('/auth/register', data),
  login: (email, password) => api.post('/auth/login', { email, password }),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me'),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }), // FR-074
  resetPassword: (data) => api.post('/auth/reset-password', data), // FR-075
};

// The signed-in user's own account; the server takes the user from the session cookie.
export const profileService = {
  get: () => api.get('/me/profile'), // FR-072
  update: (data) => api.put('/me/profile', data), // FR-072
  changePassword: (data) => api.post('/me/password', data), // FR-073
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
