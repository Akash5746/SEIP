import React from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { RootState } from './store';

// Layout
import Layout from './components/layout/Layout';

// Auth pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';

// Main pages
import DashboardPage from './pages/DashboardPage';
import ExpenseListPage from './pages/expenses/ExpenseListPage';
import CreateExpensePage from './pages/expenses/CreateExpensePage';
import ExpenseDetailPage from './pages/expenses/ExpenseDetailPage';
import ApprovalQueuePage from './pages/manager/ApprovalQueuePage';
import ManagerEmployeeDetailPage from './pages/manager/ManagerEmployeeDetailPage';
import FraudDashboardPage from './pages/FraudDashboardPage';
import ReportsPage from './pages/ReportsPage';
import AdminPage from './pages/AdminPage';
import AuditLogsPage from './pages/AuditLogsPage';
import { hasRole } from './utils/roles';

// ─── Protected Route ──────────────────────────────────────────────────────────
interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: string[];
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, allowedRoles }) => {
  const { isAuthenticated, user } = useSelector((state: RootState) => state.auth);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && user && !allowedRoles.some((allowedRole) => hasRole(user.role, allowedRole))) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
};

// ─── Public Route (redirect if already authed) ────────────────────────────────
const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useSelector((state: RootState) => state.auth);
  if (isAuthenticated) return <Navigate to="/dashboard" replace />;
  return <>{children}</>;
};

// ─── App ─────────────────────────────────────────────────────────────────────
const App: React.FC = () => {
  return (
    <Routes>
      {/* Public */}
      <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
      <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />

      {/* Protected with Layout */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="expenses" element={<ExpenseListPage />} />
        <Route path="expenses/new" element={<CreateExpensePage />} />
        <Route path="expenses/:id/edit" element={<CreateExpensePage />} />
        <Route path="expenses/:id" element={<ExpenseDetailPage />} />

        {/* Manager + Admin only */}
        <Route
          path="manager/queue"
          element={
            <ProtectedRoute allowedRoles={['ROLE_MANAGER', 'ROLE_ADMIN']}>
              <ApprovalQueuePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="manager/employees/:authUserId"
          element={
            <ProtectedRoute allowedRoles={['ROLE_MANAGER', 'ROLE_ADMIN']}>
              <ManagerEmployeeDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="reports"
          element={
            <ProtectedRoute>
              <ReportsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="fraud"
          element={
            <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <FraudDashboardPage />
            </ProtectedRoute>
          }
        />

        {/* Admin only */}
        <Route
          path="admin"
          element={
            <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <AdminPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="audit"
          element={
            <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
              <AuditLogsPage />
            </ProtectedRoute>
          }
        />

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Route>

      {/* Root redirect */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
};

export default App;
