import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    {/* The future flags are usually set in the Router definition. 
        Since we use BrowserRouter in App.jsx, we ignore the flags here 
        or suppress the logs if we can't update the router version easily. 
        For now, this warning is harmless. */}
    <App />
  </React.StrictMode>,
)