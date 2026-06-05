import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function PrivateRoute({ children, requireCoach = false, requireAdmin = false }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (requireAdmin && user.role !== 'ADMIN') return <Navigate to="/dashboard" replace />;
  if (requireCoach && user.role !== 'COACH' && user.role !== 'ADMIN') return <Navigate to="/dashboard" replace />;
  return children;
}
