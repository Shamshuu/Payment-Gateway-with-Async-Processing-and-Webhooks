import { useState, useEffect } from 'react';
import axios from 'axios';
import './Checkout.css';

// Ensure this matches your backend URL
const API_URL = 'http://localhost:8000/api/v1';

export default function Checkout() {
  const [orderId, setOrderId] = useState('');
  const [order, setOrder] = useState(null);
  const [method, setMethod] = useState(null);
  const [status, setStatus] = useState('loading');
  const [errorMsg, setErrorMsg] = useState('');
  const [paymentId, setPaymentId] = useState('');

  // Form States
  const [vpa, setVpa] = useState('');
  const [cardData, setCardData] = useState({ number: '', expiry: '', cvv: '', name: '' });

  // 1. Fetch Order on Mount
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const oid = params.get('order_id');

    if (oid) {
      setOrderId(oid);
      fetchOrder(oid);
    } else {
      setStatus('error');
      setErrorMsg('Missing order_id in URL');
    }
  }, []);

  const fetchOrder = async (oid) => {
    try {
      const res = await axios.get(`${API_URL}/orders/${oid}/public`);
      setOrder(res.data);
      setStatus('ready');
    } catch (err) {
      setStatus('error');
      setErrorMsg('Order not found');
    }
  };

  // 2. Poll Payment Status
  const startPolling = (pid) => {
    const interval = setInterval(async () => {
      try {
        const res = await axios.get(`${API_URL}/payments/${pid}/public`);
        if (res.data.status === 'success') {
          setStatus('success');
          // Notify parent SDK of success
          window.parent.postMessage({ type: 'payment_success', data: { payment_id: pid, status: 'success' } }, '*');
          clearInterval(interval);
        } else if (res.data.status === 'failed') {
          setStatus('failed');
          setErrorMsg(res.data.error_description || 'Payment declined');
          // Notify parent SDK of failure
          window.parent.postMessage({ type: 'payment_failure', data: { payment_id: pid, status: 'failed', error: res.data.error_description || 'Payment declined' } }, '*');
          clearInterval(interval);
        }
      } catch (e) {
        console.error("Polling error", e);
      }
    }, 2000);
  };

  // 3. Handle Payment Submission
  const handlePayment = async (e) => {
    e.preventDefault();
    setStatus('processing');

    const payload = {
      order_id: orderId,
      method: method
    };

    if (method === 'upi') {
      payload.vpa = vpa;
    } else {
      const [month, year] = cardData.expiry.split('/');
      payload.card = {
        number: cardData.number,
        expiry_month: month,
        expiry_year: year,
        cvv: cardData.cvv,
        holder_name: cardData.name
      };
    }

    try {
      const res = await axios.post(`${API_URL}/payments/public`, payload);
      setPaymentId(res.data.id);
      startPolling(res.data.id);
    } catch (err) {
      setStatus('failed');
      setErrorMsg(err.response?.data?.error?.description || 'Validation Failed');
    }
  };

  // --- RENDERERS ---

  if (status === 'loading') return (
    <div className="checkout-layout">
      <div className="checkout-card">Loading Order Details...</div>
    </div>
  );

  if (status === 'processing') return (
    <div className="checkout-layout">
      <div className="checkout-card" style={{ textAlign: 'center' }} data-test-id="processing-state">
        <div className="spinner"></div>
        <span data-test-id="processing-message">Processing payment...</span>
      </div>
    </div>
  );

  if (status === 'success') return (
    <div className="checkout-layout">
      <div className="checkout-card" data-test-id="success-state">
        <h2 style={{ color: '#10b981' }}>Payment Successful!</h2>
        <div style={{ marginBottom: '10px' }}>
          Payment ID: <strong data-test-id="payment-id">{paymentId}</strong>
        </div>
        <div data-test-id="success-message">Your payment has been processed successfully.</div>
      </div>
    </div>
  );

  if (status === 'failed' || status === 'error') return (
    <div className="checkout-layout">
      <div className="checkout-card" data-test-id="error-state">
        <h2 style={{ color: '#dc2626' }}>Payment Failed</h2>
        <div className="error" data-test-id="error-message">{errorMsg}</div>
        <button className="retry" data-test-id="retry-button" onClick={() => setStatus('ready')}>Try Again</button>
      </div>
    </div>
  );

  return (
    <div className="checkout-layout">
      <div className="checkout-card" data-test-id="checkout-container">
        {/* Order Summary */}
        <div data-test-id="order-summary">
          <h2>Complete Payment</h2>
          <div className="row">
            <span>Amount:</span>
            <strong data-test-id="order-amount">₹{(order.amount / 100).toFixed(2)}</strong>
          </div>
          <div className="row">
            <span>Order ID:</span>
            <span style={{ color: '#6b7280' }} data-test-id="order-id">{order.id}</span>
          </div>
        </div>

        {/* Method Selection */}
        <div className="btn-group" data-test-id="payment-methods">
          <button
            data-test-id="method-upi"
            className={method === 'upi' ? 'active' : ''}
            onClick={() => setMethod('upi')}
          >UPI</button>
          <button
            data-test-id="method-card"
            className={method === 'card' ? 'active' : ''}
            onClick={() => setMethod('card')}
          >Card</button>
        </div>

        {/* UPI Form */}
        {method === 'upi' && (
          <form data-test-id="upi-form" onSubmit={handlePayment}>
            <input
              data-test-id="vpa-input"
              placeholder="username@bank"
              value={vpa}
              onChange={(e) => setVpa(e.target.value)}
              required
            />
            <button type="submit" data-test-id="pay-button">Pay ₹{(order.amount / 100).toFixed(2)}</button>
          </form>
        )}

        {/* Card Form */}
        {method === 'card' && (
          <form data-test-id="card-form" onSubmit={handlePayment}>
            <input
              data-test-id="card-number-input"
              placeholder="Card Number"
              value={cardData.number}
              onChange={(e) => setCardData({ ...cardData, number: e.target.value })}
              required
            />
            <div style={{ display: 'flex', gap: '10px' }}>
              <input
                data-test-id="expiry-input"
                placeholder="MM/YY"
                value={cardData.expiry}
                onChange={(e) => setCardData({ ...cardData, expiry: e.target.value })}
                required
              />
              <input
                data-test-id="cvv-input"
                placeholder="CVV"
                value={cardData.cvv}
                onChange={(e) => setCardData({ ...cardData, cvv: e.target.value })}
                required
              />
            </div>
            <input
              data-test-id="cardholder-name-input"
              placeholder="Name on Card"
              value={cardData.name}
              onChange={(e) => setCardData({ ...cardData, name: e.target.value })}
              required
            />
            <button type="submit" data-test-id="pay-button">Pay ₹{(order.amount / 100).toFixed(2)}</button>
          </form>
        )}
      </div>
    </div>
  );
}