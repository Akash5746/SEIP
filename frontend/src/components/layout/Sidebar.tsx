import React, { useState } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  LayoutDashboard,
  Receipt,
  PlusCircle,
  CheckCircle,
  Shield,
  BarChart2,
  FileText,
  Settings,
  LogOut,
  ChevronLeft,
  ChevronRight,
  Zap,
  LucideIcon,
} from 'lucide-react';
import { RootState } from '../../store';
import { logout } from '../../store/slices/authSlice';

interface NavItem {
  label: string;
  path: string;
  icon: LucideIcon;
  roles?: string[];
}

const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
  { label: 'My Expenses', path: '/expenses', icon: Receipt },
  { label: 'Create Expense', path: '/expenses/new', icon: PlusCircle },
  { label: 'Approval Queue', path: '/manager/queue', icon: CheckCircle, roles: ['MANAGER', 'ADMIN'] },
  { label: 'Fraud Dashboard', path: '/fraud', icon: Shield, roles: ['MANAGER', 'ADMIN'] },
  { label: 'Reports', path: '/reports', icon: BarChart2, roles: ['MANAGER', 'ADMIN'] },
  { label: 'Audit Logs', path: '/audit', icon: FileText, roles: ['ADMIN'] },
  { label: 'Admin Panel', path: '/admin', icon: Settings, roles: ['ADMIN'] },
];

const Sidebar: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const dispatch = useDispatch();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useSelector((state: RootState) => state.auth);

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const visibleItems = navItems.filter(
    (item) => !item.roles || (user && item.roles.includes(user.role))
  );

  const userInitials = user?.username
    ? user.username.slice(0, 2).toUpperCase()
    : 'US';

  return (
    <aside
      className={`sidebar-gradient flex flex-col border-r border-slate-800 transition-all duration-300 ${
        collapsed ? 'w-16' : 'w-60'
      } min-h-screen relative flex-shrink-0`}
    >
      {/* Toggle button */}
      <button
        onClick={() => setCollapsed(!collapsed)}
        className="absolute -right-3 top-8 z-10 p-1 rounded-full bg-slate-700 border border-slate-600 text-slate-400 hover:text-white hover:bg-slate-600 transition-all"
      >
        {collapsed ? <ChevronRight size={14} /> : <ChevronLeft size={14} />}
      </button>

      {/* Brand */}
      <div className={`flex items-center gap-3 px-4 py-5 border-b border-slate-800 ${collapsed ? 'justify-center' : ''}`}>
        <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center flex-shrink-0">
          <Zap size={18} className="text-white" />
        </div>
        {!collapsed && (
          <div>
            <p className="font-bold text-white text-sm leading-tight">SEIP</p>
            <p className="text-xs text-slate-500">Expense Platform</p>
          </div>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-2 py-4 space-y-0.5 overflow-y-auto">
        {visibleItems.map((item) => {
          const Icon = item.icon;
          const isExpenseListRoute =
            item.path === '/expenses' &&
            (location.pathname === '/expenses' || /^\/expenses\/\d+$/.test(location.pathname));

          return (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === '/expenses' || item.path === '/dashboard'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150 group ${
                  (isActive || isExpenseListRoute)
                    ? 'bg-gradient-to-r from-indigo-600/30 to-violet-600/20 text-indigo-300 border border-indigo-500/20'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                } ${collapsed ? 'justify-center' : ''}`
              }
              title={collapsed ? item.label : undefined}
            >
              {({ isActive }) => {
                const active = isActive || isExpenseListRoute;

                return (
                <>
                  <Icon
                    size={18}
                    className={`flex-shrink-0 ${active ? 'text-indigo-400' : 'text-slate-500 group-hover:text-slate-300'}`}
                  />
                  {!collapsed && <span className="truncate">{item.label}</span>}
                  {!collapsed && active && (
                    <span className="ml-auto w-1.5 h-1.5 rounded-full bg-indigo-400 flex-shrink-0" />
                  )}
                </>
              )}}
            </NavLink>
          );
        })}
      </nav>

      {/* User section */}
      <div className={`p-3 border-t border-slate-800 ${collapsed ? 'flex justify-center' : ''}`}>
        {collapsed ? (
          <button
            onClick={handleLogout}
            className="p-2 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-slate-800 transition-colors"
            title="Logout"
          >
            <LogOut size={18} />
          </button>
        ) : (
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white text-sm font-bold flex-shrink-0">
              {userInitials}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-white truncate">{user?.username || 'User'}</p>
              <p className="text-xs text-slate-500 truncate">{user?.role || 'Employee'}</p>
            </div>
            <button
              onClick={handleLogout}
              className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-slate-800 transition-colors flex-shrink-0"
              title="Logout"
            >
              <LogOut size={16} />
            </button>
          </div>
        )}
      </div>
    </aside>
  );
};

export default Sidebar;
