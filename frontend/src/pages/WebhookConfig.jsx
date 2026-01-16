import { useEffect, useState } from 'react';
import axios from 'axios';

export default function WebhookConfig() {
  const [logs, setLogs] = useState([]);
  const [webhookUrl, setWebhookUrl] = useState('');
  const [secret, setSecret] = useState('');

  const apiKey = localStorage.getItem('merchant_api_key');
  const apiSecret = localStorage.getItem('merchant_api_secret');

  // Load logs on mount
  useEffect(() => {
    fetchLogs();
    // In a real app, you would also fetch the current Webhook URL here
    // e.g. axios.get('/merchant/profile')...
  }, []);

  const fetchLogs = async () => {
    try {
      const res = await axios.get('http://localhost:8000/api/v1/webhooks?limit=20', {
        headers: { 'X-Api-Key': apiKey, 'X-Api-Secret': apiSecret }
      });
      // Handle response structure: { data: [...], total: ... }
      setLogs(res.data.data || []);
    } catch (err) {
      console.error("Failed to fetch logs", err);
    }
  };

  const handleRetry = async (logId) => {
    try {
      await axios.post(
        `http://localhost:8000/api/v1/webhooks/${logId}/retry`,
        {},
        { headers: { 'X-Api-Key': apiKey, 'X-Api-Secret': apiSecret } }
      );
      alert('Retry Scheduled!');
      fetchLogs(); // Refresh status
    } catch (err) {
      alert('Retry Failed: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleSaveConfig = (e) => {
    e.preventDefault();
    alert('Note: To save the Webhook URL, you need to implement a Backend Endpoint (e.g., PUT /merchant). For now, this is UI only.');
  };

  const handleRegenerateSecret = () => {
    alert('Note: To regenerate the secret, you need to implement a Backend Endpoint (e.g., POST /merchant/regenerate-secret). For now, this is UI only.');
  };

  const handleTestWebhook = () => {
    alert('Note: To send a test webhook, you need to implement a Backend Endpoint (e.g., POST /webhooks/test). For now, this is UI only.');
  };

  return (
    <div className="card" data-test-id="webhook-config" style={{ maxWidth: '800px', margin: '2rem auto' }}>
      <h2>Webhook Configuration</h2>

      {/* Configuration Section */}
      <div style={{ background: '#f9fafb', padding: '20px', borderRadius: '8px', marginBottom: '30px', border: '1px solid #e5e7eb' }}>
        <h3 style={{ marginTop: 0, fontSize: '16px' }}>Settings</h3>
        <form data-test-id="webhook-config-form" onSubmit={handleSaveConfig} style={{ display: 'flex', gap: '10px', alignItems: 'flex-end' }}>
          <div style={{ flex: 1 }}>
            <label style={{ display: 'block', marginBottom: '5px', fontSize: '14px', fontWeight: '500' }}>Webhook URL</label>
            <input
              data-test-id="webhook-url-input"
              type="url"
              placeholder="https://your-server.com/webhook"
              value={webhookUrl}
              onChange={(e) => setWebhookUrl(e.target.value)}
              style={{ width: '100%', padding: '8px', border: '1px solid #d1d5db', borderRadius: '4px' }}
            />
          </div>
          <button data-test-id="save-webhook-button" type="submit" style={{ height: '35px', padding: '0 15px', background: '#2563eb', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
            Save
          </button>
        </form>
        <div style={{ marginTop: '15px', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '10px' }}>
          <span style={{ color: '#666' }}>Webhook Secret: </span>
          <code data-test-id="webhook-secret" style={{ background: '#eee', padding: '2px 5px', borderRadius: '4px' }}>
            {/* Displaying a placeholder or actual secret if available in API response */}
            {apiSecret || 'Not Logged In'}
          </code>
          <button
            data-test-id="regenerate-secret-button"
            onClick={handleRegenerateSecret}
            style={{ fontSize: '12px', padding: '4px 8px', cursor: 'pointer', background: 'none', border: '1px solid #666', color: '#666', borderRadius: '4px' }}
          >
            Regenerate
          </button>
        </div>
        <div style={{ marginTop: '15px' }}>
          <button
            data-test-id="test-webhook-button"
            onClick={handleTestWebhook}
            style={{ fontSize: '14px', padding: '8px 16px', cursor: 'pointer', background: '#f3f4f6', border: '1px solid #d1d5db', color: '#374151', borderRadius: '4px' }}
          >
            Send Test Webhook
          </button>
        </div>
      </div>

      {/* Logs Section */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
        <h3 style={{ margin: 0 }}>Delivery Logs</h3>
        <button onClick={fetchLogs} style={{ background: 'white', border: '1px solid #ccc', padding: '5px 10px', borderRadius: '4px', cursor: 'pointer' }}>
          Refresh
        </button>
      </div>

      <table data-test-id="webhook-logs-table" style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
        <thead>
          <tr style={{ background: '#f3f4f6', textAlign: 'left' }}>
            <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>Event</th>
            <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>Status</th>
            <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>Attempts</th>
            <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>Time</th>
            <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>Resp</th>
            <th style={{ padding: '10px', borderBottom: '1px solid #e5e7eb' }}>Action</th>
          </tr>
        </thead>
        <tbody>
          {logs.length === 0 ? (
            <tr><td colSpan="6" style={{ textAlign: 'center', padding: '20px', color: '#666' }}>No logs found</td></tr>
          ) : (
            logs.map((log) => (
              <tr data-test-id="webhook-log-item" key={log.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                <td style={{ padding: '10px' }}>{log.event}</td>
                <td style={{ padding: '10px' }}>
                  <span style={{
                    padding: '2px 6px', borderRadius: '4px', fontSize: '12px', fontWeight: '500',
                    background: log.status === 'success' ? '#dcfce7' : log.status === 'failed' ? '#fee2e2' : '#ffedd5',
                    color: log.status === 'success' ? '#166534' : log.status === 'failed' ? '#991b1b' : '#9a3412'
                  }}>
                    {log.status}
                  </span>
                </td>
                <td style={{ padding: '10px' }}>{log.attempts}/5</td>
                <td style={{ padding: '10px', color: '#666' }}>
                  {new Date(log.created_at || log.createdAt).toLocaleTimeString()}
                </td>
                <td style={{ padding: '10px' }}>
                  <code>{log.response_code || '-'}</code>
                </td>
                <td style={{ padding: '10px' }}>
                  {log.status !== 'success' && (
                    <button
                      data-test-id="webhook-retry-button"
                      onClick={() => handleRetry(log.id)}
                      style={{ fontSize: '12px', padding: '4px 8px', cursor: 'pointer', background: 'none', border: '1px solid #2563eb', color: '#2563eb', borderRadius: '4px' }}
                    >
                      Retry
                    </button>
                  )}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}