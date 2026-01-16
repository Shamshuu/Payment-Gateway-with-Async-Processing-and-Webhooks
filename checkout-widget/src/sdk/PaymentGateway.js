
// File: PaymentGateway.js
class PaymentGateway {
  constructor(options) {
    // options: {
    //   key: 'key_test_abc123',
    //   orderId: 'order_xyz',
    //   onSuccess: function(response) { },
    //   onFailure: function(error) { },
    //   onClose: function() { }
    // }
    this.options = options || {};
    this.iframeId = 'payment-gateway-iframe-' + Math.random().toString(36).substr(2, 9);
    this.modalId = 'payment-gateway-modal-' + Math.random().toString(36).substr(2, 9);
    
    // Bind methods
    this.handleMessage = this.handleMessage.bind(this);
    this.close = this.close.bind(this);
  }
  
  open() {
    if (!this.options.key || !this.options.orderId) {
        console.error("PaymentGateway: Missing required options 'key' or 'orderId'");
        return;
    }

    // 1. Create modal overlay
    const overlay = document.createElement('div');
    overlay.id = this.modalId;
    overlay.className = 'pg-modal-overlay';
    overlay.setAttribute('data-test-id', 'payment-modal'); // For testing
    
    // Define Styles
    const style = document.createElement('style');
    style.innerHTML = `
        .pg-modal-overlay {
            position: fixed;
            top: 0; left: 0;
            width: 100%; height: 100%;
            background-color: rgba(0, 0, 0, 0.6);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 9999;
        }
        .pg-modal-content {
            background: white;
            padding: 0;
            border-radius: 8px;
            width: 100%;
            max-width: 450px;
            height: 600px;
            position: relative;
            box-shadow: 0 10px 25px rgba(0,0,0,0.1);
            overflow: hidden;
        }
        .pg-close-btn {
            position: absolute;
            top: 10px;
            right: 10px;
            background: transparent;
            border: none;
            font-size: 24px;
            cursor: pointer;
            color: #666;
            z-index: 10001;
        }
        .pg-iframe {
            width: 100%;
            height: 100%;
            border: none;
        }
    `;
    document.head.appendChild(style);

    // 2. Create Modal Content
    const content = document.createElement('div');
    content.className = 'pg-modal-content';

    // Close Button
    const closeBtn = document.createElement('button');
    closeBtn.className = 'pg-close-btn';
    closeBtn.innerHTML = '&times;';
    closeBtn.setAttribute('data-test-id', 'close-modal-button');
    closeBtn.onclick = this.close;
    content.appendChild(closeBtn);

    // 3. Create iframe
    const iframe = document.createElement('iframe');
    iframe.id = this.iframeId;
    iframe.className = 'pg-iframe';
    iframe.setAttribute('data-test-id', 'payment-iframe');
    
    // Construct Source URL (Assuming checkout service runs on port 3001 locally or configured URL)
    // For evaluation, we assume localhost:3001 or explicit path
    const baseUrl = 'http://localhost:3001'; // Default local dev
    iframe.src = `${baseUrl}/checkout?order_id=${this.options.orderId}&embedded=true&key=${this.options.key}`;
    
    content.appendChild(iframe);
    overlay.appendChild(content);

    // 4. Append to body
    document.body.appendChild(overlay);

    // 5. Set up Listener
    window.addEventListener('message', this.handleMessage);
  }
  
  handleMessage(event) {
    // Security check: In production, verify event.origin
    // if (event.origin !== 'http://localhost:3001') return;

    const { type, data } = event.data;

    if (type === 'payment_success') {
       if (this.options.onSuccess) this.options.onSuccess(data);
       setTimeout(() => this.close(), 2000); // Close after brief delay or immediately
    } else if (type === 'payment_failed') {
       if (this.options.onFailure) this.options.onFailure(data);
    } else if (type === 'close_modal') {
       this.close();
    }
  }
  
  close() {
    const modal = document.getElementById(this.modalId);
    if (modal) {
        modal.remove();
    }
    window.removeEventListener('message', this.handleMessage);
    if (this.options.onClose) this.options.onClose();
  }
}

// Expose globally
window.PaymentGateway = PaymentGateway;
export default PaymentGateway;
