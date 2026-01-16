import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';

export default function Dashboard() {
  const [stats, setStats] = useState({
    totalVolume: 0,
    successRate: 0,
    totalCount: 0,
    successCount: 0
  });
  const [loading, setLoading] = useState(true);

  // Get User Details from LocalStorage
  const email = localStorage.getItem('merchant_email');
  const apiKey = localStorage.getItem('merchant_api_key');
  const apiSecret = localStorage.getItem('merchant_api_secret');

  useEffect(() => {
    calculateStats();
  }, []);

  const calculateStats = async () => {
    try {
      // Fetch all payments to calculate stats locally
      const res = await axios.get('http://localhost:8000/api/v1/payments', {
        headers: { 'X-Api-Key': apiKey, 'X-Api-Secret': apiSecret }
      });
      
      const payments = res.data;
      const total = payments.length;
      
      if (total === 0) {
        setStats({ totalVolume: 0, successRate: 0, totalCount: 0, successCount: 0 });
      } else {
        const successful = payments.filter(p => p.status === 'success');
        const successCount = successful.length;
        
        // Calculate Total Volume (Sum of successful amounts)
        // Assuming amount is in cents/paisa, divide by 100 for display
        const volume = successful.reduce((sum, p) => sum + (p.amount || 0), 0) / 100;
        
        // Calculate Success Rate
        const rate = (successCount / total) * 100;

        setStats({
          totalVolume: volume,
          successRate: rate.toFixed(1),
          totalCount: total,
          successCount: successCount
        });
      }
    } catch (err) {
      console.error("Failed to fetch stats", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
      <h2 style={{ marginBottom: '20px' }}>👋 Welcome back, Merchant</h2>

      {/* STATS GRID */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px', marginBottom: '30px' }}>
        {/* Card 1: Total Revenue */}
        <div className="card" style={{ padding: '20px', borderLeft: '4px solid #2563eb' }}>
          <h4 style={{ margin: '0 0 10px', color: '#64748b' }}>Total Revenue</h4>
          <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#0f172a' }}>
            ₹{stats.totalVolume.toLocaleString()}
          </div>
        </div>

        {/* Card 2: Success Rate */}
        <div className="card" style={{ padding: '20px', borderLeft: '4px solid #16a34a' }}>
          <h4 style={{ margin: '0 0 10px', color: '#64748b' }}>Success Rate</h4>
          <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#0f172a' }}>
            {stats.successRate}%
            <span style={{ fontSize: '12px', color: '#64748b', fontWeight: 'normal', marginLeft: '5px' }}>
              ({stats.successCount}/{stats.totalCount})
            </span>
          </div>
        </div>

        {/* Card 3: Quick Action */}
        <div className="card" style={{ padding: '20px', borderLeft: '4px solid #f59e0b', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Link to="/dashboard/transactions" style={{ textDecoration: 'none', color: '#2563eb', fontWeight: 'bold' }}>
            View All Transactions →
          </Link>
        </div>
      </div>

      {/* ACCOUNT DETAILS SECTION */}
      <div className="card" style={{ padding: '30px' }}>
        <h3 style={{ marginTop: 0, borderBottom: '1px solid #eee', paddingBottom: '15px' }}>🔑 Account Details</h3>
        
        <div style={{ display: 'grid', gap: '20px', marginTop: '20px' }}>
          <div>
            <label style={{ display: 'block', fontWeight: 'bold', color: '#475569', marginBottom: '5px' }}>Email Address</label>
            <div style={{ padding: '10px', background: '#f8fafc', borderRadius: '6px', border: '1px solid #e2e8f0' }}>
              {email}
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 'bold', color: '#475569', marginBottom: '5px' }}>API Public Key</label>
              <code style={{ display: 'block', padding: '10px', background: '#f1f5f9', borderRadius: '6px', border: '1px solid #e2e8f0', color: '#0f172a' }}>
                {apiKey || 'Not Logged In'}
              </code>
            </div>

            <div>
              <label style={{ display: 'block', fontWeight: 'bold', color: '#475569', marginBottom: '5px' }}>API Secret Key</label>
              <code style={{ display: 'block', padding: '10px', background: '#f1f5f9', borderRadius: '6px', border: '1px solid #e2e8f0', color: '#dc2626' }}>
                {apiSecret || 'Not Logged In'}
              </code>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}