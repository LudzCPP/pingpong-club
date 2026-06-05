import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function PrivateRoute({ children, requireCoach = false }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (requireCoach && user.role !== 'COACH') return <Navigate to="/dashboard" replace />;
  return children;
}
