import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { useAuth } from './context/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Navbar from './components/Navbar';
import OnboardingModal from './components/OnboardingModal';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import DashboardPage from './pages/DashboardPage';
import TrainingsPage from './pages/TrainingsPage';
import PlayersPage from './pages/PlayersPage';
import CoachesPage from './pages/CoachesPage';
import FinancesPage from './pages/FinancesPage';
import ReportsPage from './pages/ReportsPage';
import CalendarPage from './pages/CalendarPage';
import InvitationsPage from './pages/InvitationsPage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import ProfilePage from './pages/ProfilePage';
import './index.css';

function Layout({ children }) {
  return (
    <>
      <Navbar />
      <main>{children}</main>
    </>
  );
}

function OnboardingGate() {
  const { user } = useAuth();
  const location = useLocation();
  const [show, setShow] = useState(false);

  useEffect(() => {
    if (!user) return;
    const key = `onboarded_${user.email}`;
    if (!localStorage.getItem(key)) {
      setShow(true);
    }
  }, [user]);

  const publicPaths = ['/login', '/register', '/forgot-password', '/reset-password'];
  if (!user || publicPaths.some(p => location.pathname.startsWith(p))) return null;
  if (!show) return null;

  function handleClose() {
    localStorage.setItem(`onboarded_${user.email}`, '1');
    setShow(false);
  }

  return <OnboardingModal role={user.role} onClose={handleClose} />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <OnboardingGate />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="/dashboard" element={
            <PrivateRoute><Layout><DashboardPage /></Layout></PrivateRoute>
          } />
          <Route path="/trainings" element={
            <PrivateRoute><Layout><TrainingsPage /></Layout></PrivateRoute>
          } />
          <Route path="/calendar" element={
            <PrivateRoute><Layout><CalendarPage /></Layout></PrivateRoute>
          } />
          <Route path="/players" element={
            <PrivateRoute requireCoach><Layout><PlayersPage /></Layout></PrivateRoute>
          } />
          <Route path="/coaches" element={
            <PrivateRoute requireAdmin><Layout><CoachesPage /></Layout></PrivateRoute>
          } />
          <Route path="/finances" element={
            <PrivateRoute requireCoach><Layout><FinancesPage /></Layout></PrivateRoute>
          } />
          <Route path="/reports" element={
            <PrivateRoute requireCoach><Layout><ReportsPage /></Layout></PrivateRoute>
          } />
          <Route path="/invitations" element={
            <PrivateRoute><Layout><InvitationsPage /></Layout></PrivateRoute>
          } />
          <Route path="/admin/dashboard" element={
            <PrivateRoute requireAdmin><Layout><AdminDashboardPage /></Layout></PrivateRoute>
          } />
          <Route path="/profile" element={
            <PrivateRoute><Layout><ProfilePage /></Layout></PrivateRoute>
          } />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
