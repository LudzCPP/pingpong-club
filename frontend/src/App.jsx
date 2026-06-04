import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Navbar from './components/Navbar';
import LoginPage from './pages/LoginPage';
import TrainingsPage from './pages/TrainingsPage';
import PlayersPage from './pages/PlayersPage';
import FinancesPage from './pages/FinancesPage';
import './index.css';

function Layout({ children }) {
  return (
    <>
      <Navbar />
      <main className="main-content">{children}</main>
    </>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/trainings" element={
            <PrivateRoute><Layout><TrainingsPage /></Layout></PrivateRoute>
          } />
          <Route path="/players" element={
            <PrivateRoute requireCoach><Layout><PlayersPage /></Layout></PrivateRoute>
          } />
          <Route path="/finances" element={
            <PrivateRoute><Layout><FinancesPage /></Layout></PrivateRoute>
          } />
          <Route path="*" element={<Navigate to="/trainings" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
