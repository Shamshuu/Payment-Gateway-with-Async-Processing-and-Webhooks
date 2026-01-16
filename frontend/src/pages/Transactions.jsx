import { useEffect, useState } from 'react';
import axios from 'axios';

export default function Transactions() {
  const [payments, setPayments] = useState([]);
  const [refundInputs, setRefundInputs] = useState({}); // New state for refund inputs

  const apiKey = localStorage.getItem('merchant_api_key');
  const apiSecret = localStorage.getItem('merchant_api_secret');

  // --- DATE FORMATTER ---
  const formatDate = (dateInput) => {
    if (!dateInput) return 'N/A';
    try {
      let date;
      if (Array.isArray(dateInput)) {
        date = new Date(
          dateInput[0],
          dateInput[1] - 1,
          dateInput[2],
          dateInput[3] || 0,
          dateInput[4] || 0,
          dateInput[5] || 0
        );
      } else {
        date = new Date(dateInput);
      }

      if (isNaN(date.getTime())) return 'Invalid Date';

      return new Intl.DateTimeFormat('en-US', {
        year: 'numeric', month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit'
      }).format(date);
    } catch (e) {
      return 'Error';
    }
  };

  const fetchPayments = async () => {
    try {
      const res = await axios.get('http://localhost:8000/api/v1/payments', {
        headers: { 'X-Api-Key': apiKey, 'X-Api-Secret': apiSecret }
      });
      // Sort by newest first based on ID
      const sorted = res.data.sort((a, b) => b.id.localeCompare(a.id));
      setPayments(sorted);
    } catch (err) {
      console.error(err);
    }
  };

  useEffect(() => {
    fetchPayments();
  }, []);

  // --- CAPTURE LOGIC ---
  const handleCapture = async (paymentId) => {
    if (!window.confirm('Are you sure you want to capture this payment?')) return;
    try {
      await axios.post(
        `http://localhost:8000/api/v1/payments/${paymentId}/capture`,
        { amount: 500 }, // Send dummy amount or dynamic if needed
        { headers: { 'X-Api-Key': apiKey, 'X-Api-Secret': apiSecret } }
      );
      alert('Payment Captured Successfully!');
      fetchPayments(); // Refresh list to update UI
    } catch (err) {
      alert(err.response?.data?.error?.description || 'Capture Failed');
    }
  };

  // --- REFUND LOGIC ---
  const handleRefund = async (paymentId) => {
    const amount = refundInputs[paymentId];
    if (!amount) return alert('Please enter a refund amount');

    try {
      await axios.post(
        `http://localhost:8000/api/v1/payments/${paymentId}/refunds`,
        {
          amount: parseInt(amount),
          reason: "Dashboard Refund"
        },
        { headers: { 'X-Api-Key': apiKey, 'X-Api-Secret': apiSecret } }
      );
      alert('Refund Initiated!');
      setRefundInputs({ ...refundInputs, [paymentId]: '' }); // Clear input
      fetchPayments();
    } catch (err) {
      alert(err.response?.data?.error?.description || 'Refund Failed');
    }
  };

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>Transaction History</h2>
        <button onClick={fetchPayments} style={{ padding: '5px 10px', fontSize: '14px' }}>Refresh</button>
      </div>

      <table data-test-id="transactions-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Order ID</th>
            <th>Amount</th>
            <th>Method</th>
            <th>Status</th>
            <th>Time</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {payments.map(p => (
            <tr key={p.id} data-test-id="transaction-row" data-payment-id={p.id}>
              <td data-test-id="payment-id">
                <small>{p.id}</small>
              </td>
              <td data-test-id="order-id">{p.order_id}</td>
              <td data-test-id="amount">₹{(p.amount / 100).toFixed(2)}</td>
              <td data-test-id="method">{p.method}</td>
              <td data-test-id="status">
                <span style={{
                  padding: '4px 8px', borderRadius: '4px', fontSize: '12px',
                  backgroundColor: p.status === 'success' ? '#dcfce7' : p.status === 'failed' ? '#fee2e2' : '#ffedd5',
                  color: p.status === 'success' ? '#166534' : p.status === 'failed' ? '#991b1b' : '#9a3412',
                  border: `1px solid ${p.status === 'success' ? '#bbf7d0' : p.status === 'failed' ? '#fecaca' : '#fed7aa'}`
                }}>
                  {p.status}
                  {p.captured && <strong> (C)</strong>}
                </span>
              </td>
              <td data-test-id="created-at">{formatDate(p.createdAt)}</td>
              <td>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>

                  {/* Capture Button */}
                  {p.status === 'success' && !p.captured && (
                    <button
                      onClick={() => handleCapture(p.id)}
                      style={{
                        padding: '5px 10px',
                        fontSize: '12px',
                        background: '#3b82f6',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer'
                      }}
                    >
                      Capture
                    </button>
                  )}

                  {/* Refund Input & Button */}
                  {p.status === 'success' && (
                    <>
                      <input
                        type="number"
                        placeholder="Amt"
                        style={{ width: '60px', padding: '4px', fontSize: '12px' }}
                        value={refundInputs[p.id] || ''}
                        onChange={(e) => setRefundInputs({ ...refundInputs, [p.id]: e.target.value })}
                      />
                      <button
                        onClick={() => handleRefund(p.id)}
                        style={{
                          padding: '5px 10px',
                          fontSize: '12px',
                          background: '#ef4444',
                          color: 'white',
                          border: 'none',
                          borderRadius: '4px',
                          cursor: 'pointer'
                        }}
                      >
                        Refund
                      </button>
                    </>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}