import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { Bell, ChevronDown, User, Settings, LogOut } from 'lucide-react';
import { RootState } from '../../store';
import { useDispatch } from 'react-redux';
import { logout } from '../../store/slices/authSlice';

const PAGE_TITLES: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/expenses': 'Expense History',
  '/expenses/new': 'Create Expense',
  '/manager/queue': 'Approval Queue',
  '/fraud': 'Fraud Dashboard',
  '/reports': 'Reports & Analytics',
  '/admin': 'Admin Panel',
  '/audit': 'Audit Logs',
  '/profile': 'My Profile',
};

const roleColors: Record<string, string> = {
  ADMIN: 'bg-rose-900 text-rose-300 border border-rose-800',
  MANAGER: 'bg-indigo-900 text-indigo-300 border border-indigo-800',
  EMPLOYEE: 'bg-emerald-900 text-emerald-300 border border-emerald-800',
};

const Navbar: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { user } = useSelector((state: RootState) => state.auth);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [hasNotifications] = useState(true);

  // Handle dynamic routes
  const getTitle = () => {
    if (location.pathname.startsWith('/expenses/') && location.pathname !== '/expenses/new') {
      return 'Expense Details';
    }
    return PAGE_TITLES[location.pathname] || 'SEIP';
  };

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  const userInitials = user?.username ? user.username.slice(0, 2).toUpperCase() : 'US';
  const roleClass = user ? roleColors[user.role] || roleColors.EMPLOYEE : roleColors.EMPLOYEE;

  return (
    <header className="h-14 border-b border-slate-800 bg-surface/80 backdrop-blur-md flex items-center px-6 gap-4 sticky top-0 z-30">
      {/* Page Title */}
      <div className="flex-1">
        <h1 className="text-base font-semibold text-white">{getTitle()}</h1>
      </div>

      {/* Right side */}
      <div className="flex items-center gap-3">
        {/* Role badge */}
        {user && (
          <span className={`hidden sm:inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${roleClass}`}>
            {user.role}
          </span>
        )}

        {/* Notification bell */}
        <button className="relative p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors">
          <Bell size={18} />
          {hasNotifications && (
            <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-rose-500" />
          )}
        </button>

        {/* Profile dropdown */}
        <div className="relative">
          <button
            onClick={() => setDropdownOpen(!dropdownOpen)}
            className="flex items-center gap-2 p-1.5 rounded-lg hover:bg-slate-800 transition-colors"
          >
            <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white text-xs font-bold">
              {userInitials}
            </div>
            <span className="hidden sm:block text-sm text-slate-300 max-w-[120px] truncate">
              {user?.username}
            </span>
            <ChevronDown size={14} className={`text-slate-500 transition-transform ${dropdownOpen ? 'rotate-180' : ''}`} />
          </button>

          {dropdownOpen && (
            <>
              <div className="fixed inset-0 z-10" onClick={() => setDropdownOpen(false)} />
              <div className="absolute right-0 top-full mt-2 w-48 glass-card py-1 z-20 shadow-xl"
                style={{ border: '1px solid rgba(99,102,241,0.2)' }}>
                <div className="px-3 py-2 border-b border-slate-700">
                  <p className="text-sm font-medium text-white truncate">{user?.username}</p>
                  <p className="text-xs text-slate-500 truncate">{user?.email}</p>
                </div>
                <button
                  onClick={() => { navigate('/profile'); setDropdownOpen(false); }}
                  className="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
                >
                  <User size={15} /> Profile
                </button>
                <button
                  onClick={() => { navigate('/admin'); setDropdownOpen(false); }}
                  className="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
                >
                  <Settings size={15} /> Settings
                </button>
                <div className="border-t border-slate-700 mt-1 pt-1">
                  <button
                    onClick={handleLogout}
                    className="w-full flex items-center gap-2.5 px-3 py-2 text-sm text-rose-400 hover:text-rose-300 hover:bg-rose-950 transition-colors"
                  >
                    <LogOut size={15} /> Logout
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </header>
  );
};

export default Navbar;
