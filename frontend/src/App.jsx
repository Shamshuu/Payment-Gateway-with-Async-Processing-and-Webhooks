import { BrowserRouter, Routes, Route, Link, Navigate, useLocation } from 'react-router-dom';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import ApiDocs from './pages/ApiDocs';
import Transactions from './pages/Transactions';
import WebhookConfig from './pages/WebhookConfig';

// Checkout is served from separate container on port 3001
function CheckoutRedirect() {
  const location = useLocation();
  // Redirect to checkout service with query params
  window.location.href = `http://localhost:3001${location.search}`;
  return <div>Redirecting to checkout...</div>;
}

function Layout({ children }) {
  const location = useLocation();
  const isAuthenticated = !!localStorage.getItem('merchant_api_key');
  const isCheckout = location.pathname === '/checkout';

  if (isCheckout) {
    return <>{children}</>;
  }

  return (
    <>
      {isAuthenticated && (
        <nav style={{ background: '#fff', borderBottom: '1px solid #eee', padding: '10px 0', marginBottom: '20px' }}>
          <div className="container" style={{ display: 'flex', alignItems: 'center', padding: '0 20px' }}>
            <strong style={{ fontSize: '1.2rem', marginRight: '30px' }}>Payment Gateway</strong>

            {/* Navigation Links */}
            <Link to="/dashboard" style={{ marginRight: '20px', textDecoration: 'none', color: '#333' }}>Dashboard</Link>
            <Link to="/dashboard/transactions" style={{ marginRight: '20px', textDecoration: 'none', color: '#333' }}>Transactions</Link>
            <Link to="/dashboard/webhooks" style={{ marginRight: '20px', textDecoration: 'none', color: '#333' }}>Webhooks</Link>
            <Link to="/dashboard/docs" style={{ marginRight: '20px', textDecoration: 'none', color: '#333' }}>Docs</Link>

            <button
              onClick={() => { localStorage.clear(); window.location.href = '/login'; }}
              style={{ marginLeft: 'auto', padding: '6px 12px', cursor: 'pointer', background: '#f1f5f9', border: 'none', borderRadius: '4px', color: '#475569' }}
            >
              Logout
            </button>
          </div>
        </nav>
      )}

      <div className="container" style={{ padding: '0 20px' }}>
        {children}
      </div>
    </>
  );
}

function App() {
  const isAuthenticated = !!localStorage.getItem('merchant_api_key');

  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/checkout" element={<CheckoutRedirect />} />

          {/* Protected Routes */}
          <Route
            path="/dashboard"
            element={isAuthenticated ? <Dashboard /> : <Navigate to="/login" />}
          />
          <Route
            path="/dashboard/transactions"
            element={isAuthenticated ? <Transactions /> : <Navigate to="/login" />}
          />
          <Route
            path="/dashboard/webhooks"
            element={isAuthenticated ? <WebhookConfig /> : <Navigate to="/login" />}
          />

          <Route
            path="/dashboard/docs"
            element={isAuthenticated ? <ApiDocs /> : <Navigate to="/login" />}
          />

          {/* Fallback */}
          <Route path="/" element={<Navigate to="/dashboard" />} />
          <Route path="*" element={<Navigate to="/dashboard" />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}
export default App;