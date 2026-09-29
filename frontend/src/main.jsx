import React from 'react'
import ReactDOM from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { router } from './app/router'
import { EmsProvider } from './context/EmsContext'
import { AuthProvider } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'
import './styles/global.css'

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ThemeProvider>
      <AuthProvider>
        <EmsProvider>
          <RouterProvider router={router} />
        </EmsProvider>
      </AuthProvider>
    </ThemeProvider>
  </React.StrictMode>
)
