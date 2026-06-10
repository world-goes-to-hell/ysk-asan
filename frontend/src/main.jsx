import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import App from './App.jsx';
import { ToastProvider } from './components/common/ToastProvider.jsx';
import { AuthProvider } from './contexts/AuthContext.jsx';
import './styles/global.css';

// ToastProvider 가 AuthProvider 를 감싼다(AuthContext 가 useToast 를 사용).
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <ToastProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </ToastProvider>
    </BrowserRouter>
  </StrictMode>
);
