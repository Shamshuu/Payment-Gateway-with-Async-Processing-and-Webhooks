(function(window) {
    class PaymentGateway {
        constructor(config) {
            this.config = config;
            this.iframe = null;
        }

        open() {
            // 1. Create Modal Overlay
            const overlay = document.createElement('div');
            overlay.style.position = 'fixed';
            overlay.style.top = '0';
            overlay.style.left = '0';
            overlay.style.width = '100%';
            overlay.style.height = '100%';
            overlay.style.backgroundColor = 'rgba(0,0,0,0.5)';
            overlay.style.zIndex = '9999';
            overlay.style.display = 'flex';
            overlay.style.justifyContent = 'center';
            overlay.style.alignItems = 'center';
            overlay.id = 'pg-modal';

            // 2. Create Iframe
            // Points to YOUR React App, passing the Order ID
            const iframe = document.createElement('iframe');
            iframe.src = `http://localhost:3001?order_id=${this.config.orderId}`;
            iframe.style.width = '450px';
            iframe.style.height = '600px';
            iframe.style.border = 'none';
            iframe.style.borderRadius = '8px';
            iframe.style.backgroundColor = 'white';

            // 3. Append to DOM
            overlay.appendChild(iframe);
            document.body.appendChild(overlay);
            this.iframe = overlay;

            // 4. Listen for Messages from React App (Success/Failure)
            window.addEventListener('message', this.handleMessage.bind(this));
        }

        handleMessage(event) {
            // Security: In production, check event.origin
            const { type, data } = event.data;

            if (type === 'PAYMENT_SUCCESS') {
                if (this.config.onSuccess) this.config.onSuccess(data);
                this.close();
            } else if (type === 'PAYMENT_FAILED') {
                if (this.config.onFailure) this.config.onFailure(data);
            }
        }

        close() {
            if (this.iframe) {
                document.body.removeChild(this.iframe);
                this.iframe = null;
            }
        }
    }
    window.PaymentGateway = PaymentGateway;
})(window);