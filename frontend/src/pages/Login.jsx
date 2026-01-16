import { useState } from 'react';
import axios from 'axios';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isRegistering, setIsRegistering] = useState(false);
  const [error, setError] = useState('');

  const handleAuth = async (e) => {
    e.preventDefault();
    setError('');

    const endpoint = isRegistering ? '/auth/register' : '/auth/login';
    
    try {
      const res = await axios.post(`http://localhost:8000/api/v1${endpoint}`, {
        email: email,
        password: password 
      });

      const data = res.data;
      localStorage.setItem('merchant_email', data.email);
      localStorage.setItem('merchant_api_key', data.api_key);
      localStorage.setItem('merchant_api_secret', data.api_secret);
      
      window.location.href = '/dashboard';

    } catch (err) {
      setError(err.response?.data?.error || 'Authentication failed');
    }
  };

  return (
    <div className="card" style={{maxWidth: '400px', margin: '2rem auto'}}>
      <h2>{isRegistering ? 'Register Merchant' : 'Merchant Login'}</h2>
      
      {error && <div className="error" style={{marginBottom: '1rem'}}>{error}</div>}
      
      <form data-test-id="login-form" onSubmit={handleAuth}>
        <input 
          data-test-id="email-input"
          type="email" 
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="Enter email"
          required
        />
        <input 
          data-test-id="password-input"
          type="password" 
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Enter password"
          required
        />
        
        <button data-test-id="login-button" type="submit" style={{width: '100%', marginTop: '10px'}}>
          {isRegistering ? 'Create Account' : 'Login'}
        </button>
      </form>

      <div style={{marginTop: '1rem', textAlign: 'center', fontSize: '0.9rem'}}>
        <span style={{color: '#666'}}>
          {isRegistering ? 'Already have an account? ' : 'Need an account? '}
        </span>
        <button 
          onClick={() => setIsRegistering(!isRegistering)}
          style={{background: 'none', color: '#2563eb', border: 'none', padding: 0, textDecoration: 'underline', width: 'auto'}}
        >
          {isRegistering ? 'Login here' : 'Register here'}
        </button>
      </div>
    </div>
  );
}