import axios from 'axios';

// Create axios instance
const api = axios.create({
  baseURL: 'http://localhost:8000/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Automatically add the API Key from localStorage to every request
api.interceptors.request.use((config) => {
  const apiKey = localStorage.getItem('merchant_api_key');
  if (apiKey) {
    config.headers['X-Api-Key'] = apiKey;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

export default api;