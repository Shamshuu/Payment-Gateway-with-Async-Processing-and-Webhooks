import { useState } from 'react';

export default function ApiDocs() {
  const [copied, setCopied] = useState('');
  const apiKey = localStorage.getItem('merchant_api_key') || 'key_test_...';
  
  const copyToClipboard = (text, section) => {
    navigator.clipboard.writeText(text);
    setCopied(section);
    setTimeout(() => setCopied(''), 2000);
  };

  const codeSnippets = {
    createOrder: `curl -X POST http://localhost:8000/api/v1/orders \\
  -H "X-Api-Key: ${apiKey}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "amount": 5000,
    "currency": "INR",
    "receipt": "receipt_123"
  }'`,
    
    sdkIntegration: `<script src="http://localhost:3001/checkout.js"></script>
<button id="pay-btn">Pay Now</button>

<script>
  const gateway = new PaymentGateway({
    apiKey: '${apiKey}',
    onSuccess: (data) => alert('Payment Success: ' + data.paymentId),
    onClose: () => console.log('Modal closed')
  });

  document.getElementById('pay-btn').onclick = () => {
    gateway.open();
  };
</script>`,

    webhookVerify: `const crypto = require('crypto');

function verifyWebhook(payload, signature, secret) {
  const expected = crypto
    .createHmac('sha256', secret)
    .update(JSON.stringify(payload))
    .digest('hex');
    
  return signature === expected;
}`
  };

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
      <h2 style={{ marginBottom: '20px' }}>📖 Integration Documentation</h2>
      
      <div className="card" data-test-id="api-docs">
        
        {/* SECTION 1: CREATE ORDER */}
        <section style={{ marginBottom: '40px' }} data-test-id="section-create-order">
          <h3>1. Create an Order</h3>
          <p>Call this endpoint from your backend to initialize a payment.</p>
          <div style={{ position: 'relative' }}>
            <pre style={{ background: '#1e293b', color: '#e2e8f0', padding: '15px', borderRadius: '6px', overflowX: 'auto' }}>
              <code data-test-id="code-snippet-create-order">{codeSnippets.createOrder}</code>
            </pre>
            <button 
              onClick={() => copyToClipboard(codeSnippets.createOrder, 'order')}
              style={{ position: 'absolute', top: '10px', right: '10px', fontSize: '12px' }}
            >
              {copied === 'order' ? 'Copied!' : 'Copy'}
            </button>
          </div>
        </section>

        {/* SECTION 2: FRONTEND SDK */}
        <section style={{ marginBottom: '40px' }} data-test-id="section-sdk-integration">
          <h3>2. Frontend SDK Integration</h3>
          <p>Add this code to your checkout page to open the payment modal.</p>
          <div style={{ position: 'relative' }}>
            <pre style={{ background: '#1e293b', color: '#e2e8f0', padding: '15px', borderRadius: '6px', overflowX: 'auto' }}>
              <code data-test-id="code-snippet-sdk">{codeSnippets.sdkIntegration}</code>
            </pre>
            <button 
              onClick={() => copyToClipboard(codeSnippets.sdkIntegration, 'sdk')}
              style={{ position: 'absolute', top: '10px', right: '10px', fontSize: '12px' }}
            >
              {copied === 'sdk' ? 'Copied!' : 'Copy'}
            </button>
          </div>
        </section>

        {/* SECTION 3: WEBHOOKS */}
        <section data-test-id="section-webhook-verification">
          <h3>3. Verifying Webhook Signatures</h3>
          <p>Always verify the <code>X-Webhook-Signature</code> header to prevent fraud.</p>
          <div style={{ position: 'relative' }}>
            <pre style={{ background: '#1e293b', color: '#e2e8f0', padding: '15px', borderRadius: '6px', overflowX: 'auto' }}>
              <code data-test-id="code-snippet-webhook">{codeSnippets.webhookVerify}</code>
            </pre>
            <button 
              onClick={() => copyToClipboard(codeSnippets.webhookVerify, 'webhook')}
              style={{ position: 'absolute', top: '10px', right: '10px', fontSize: '12px' }}
            >
              {copied === 'webhook' ? 'Copied!' : 'Copy'}
            </button>
          </div>
        </section>

      </div>
    </div>
  );
}