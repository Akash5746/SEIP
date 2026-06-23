import React, { useState } from 'react';
import { format } from 'date-fns';
import {
  Users,
  Search,
  UserCheck,
  UserX,
  Building,
  Shield,
  ChevronDown,
} from 'lucide-react';
import {
  useGetEmployeesQuery,
  useActivateEmployeeMutation,
  useDeactivateEmployeeMutation,
  useGetDepartmentsQuery,
} from '../store/api/userApi';
import LoadingSkeleton from '../components/ui/LoadingSkeleton';
import { formatRoleLabel, normalizeRole } from '../utils/roles';

const ROLE_COLORS: Record<string, string> = {
  ROLE_ADMIN: 'bg-rose-950 text-rose-300 border border-rose-800',
  ROLE_MANAGER: 'bg-indigo-950 text-indigo-300 border border-indigo-800',
  ROLE_EMPLOYEE: 'bg-emerald-950 text-emerald-300 border border-emerald-800',
};

const AdminPage: React.FC = () => {
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [deptFilter, setDeptFilter] = useState('');
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState<'users' | 'departments'>('users');

  const { data, isLoading } = useGetEmployeesQuery({ page, size: 20, search: search || undefined, department: deptFilter || undefined });
  const { data: deptData } = useGetDepartmentsQuery();
  const [activateEmployee] = useActivateEmployeeMutation();
  const [deactivateEmployee] = useDeactivateEmployeeMutation();

  const employees = data?.data?.content ?? [];
  const totalPages = data?.data?.totalPages ?? 1;
  const totalElements = data?.data?.totalElements ?? 0;
  const departments = deptData?.data ?? [];

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearch(searchInput);
    setPage(0);
  };

  const handleToggleActive = async (id: number, currentlyActive: boolean) => {
    setActionLoading(id);
    try {
      if (currentlyActive) {
        await deactivateEmployee(id).unwrap();
      } else {
        await activateEmployee(id).unwrap();
      }
    } finally {
      setActionLoading(null);
    }
  };

  // Stats
  const activeCount = employees.filter((e) => e.active).length;
  const adminCount = employees.filter((e) => normalizeRole(e.role) === 'ROLE_ADMIN').length;
  const managerCount = employees.filter((e) => normalizeRole(e.role) === 'ROLE_MANAGER').length;

  return (
    <div className="space-y-5">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-white">Admin Panel</h2>
        <p className="text-sm text-slate-500 mt-0.5">Manage users, roles, and departments</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        {[
          { label: 'Total Users', value: totalElements, icon: Users, color: 'card-indigo', gradient: 'from-indigo-500 to-violet-600' },
          { label: 'Active Users', value: activeCount, icon: UserCheck, color: 'card-emerald', gradient: 'from-emerald-500 to-teal-600' },
          { label: 'Managers', value: managerCount, icon: Shield, color: 'card-amber', gradient: 'from-amber-500 to-orange-600' },
          { label: 'Admins', value: adminCount, icon: Shield, color: 'card-rose', gradient: 'from-rose-500 to-pink-600' },
        ].map(({ label, value, icon: Icon, color, gradient }) => (
          <div key={label} className={`glass-card p-4 ${color}`}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-500 mb-1">{label}</p>
                <p className="text-2xl font-bold text-white">{value}</p>
              </div>
              <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${gradient} flex items-center justify-center`}>
                <Icon size={18} className="text-white" />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-slate-800/50 p-1 rounded-xl w-fit">
        {[
          { key: 'users', label: 'User Management', icon: Users },
          { key: 'departments', label: 'Departments', icon: Building },
        ].map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setActiveTab(key as typeof activeTab)}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all ${
              activeTab === key
                ? 'bg-gradient-to-r from-indigo-600 to-violet-600 text-white shadow-lg'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Icon size={14} />
            {label}
          </button>
        ))}
      </div>

      {activeTab === 'users' && (
        <div className="space-y-4">
          {/* Filters */}
          <div className="glass-card p-4">
            <div className="flex flex-col sm:flex-row gap-3">
              <form onSubmit={handleSearch} className="flex-1 relative">
                <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                <input
                  type="text"
                  value={searchInput}
                  onChange={(e) => setSearchInput(e.target.value)}
                  placeholder="Search by username or email..."
                  className="form-input pl-9 w-full"
                />
              </form>
              <div className="relative">
                <Building size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
                <select
                  value={deptFilter}
                  onChange={(e) => { setDeptFilter(e.target.value); setPage(0); }}
                  className="form-input pl-9 pr-8 appearance-none min-w-[180px]"
                >
                  <option value="">All Departments</option>
                  {departments.map((d) => <option key={d} value={d}>{d}</option>)}
                </select>
                <ChevronDown size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
              </div>
            </div>
          </div>

          {/* Table */}
          <div className="glass-card overflow-hidden">
            {isLoading ? (
              <LoadingSkeleton rows={8} />
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>User</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Department</th>
                        <th>Status</th>
                        <th>Joined</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {employees.length === 0 ? (
                        <tr>
                          <td colSpan={7} className="text-center py-12 text-slate-500">No users found</td>
                        </tr>
                      ) : (
                        employees.map((employee) => (
                          <tr key={employee.id}>
                            <td>
                              <div className="flex items-center gap-2.5">
                                <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-white text-sm font-bold flex-shrink-0 ${
                                  employee.active
                                    ? 'bg-gradient-to-br from-indigo-500 to-violet-600'
                                    : 'bg-slate-700'
                                }`}>
                                  {employee.username?.slice(0, 2).toUpperCase() || 'U?'}
                                </div>
                                <p className="text-sm font-medium text-white">{employee.username}</p>
                              </div>
                            </td>
                            <td>
                              <span className="text-sm text-slate-400">{employee.email}</span>
                            </td>
                            <td>
                              <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ${ROLE_COLORS[normalizeRole(employee.role)] || ROLE_COLORS.ROLE_EMPLOYEE}`}>
                                {formatRoleLabel(employee.role)}
                              </span>
                            </td>
                            <td>
                              <span className="text-sm text-slate-400">{employee.department || '—'}</span>
                            </td>
                            <td>
                              <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${
                                employee.active
                                  ? 'bg-emerald-950 text-emerald-300 border border-emerald-800'
                                  : 'bg-slate-800 text-slate-500 border border-slate-700'
                              }`}>
                                <span className={`w-1.5 h-1.5 rounded-full ${employee.active ? 'bg-emerald-400' : 'bg-slate-600'}`} />
                                {employee.active ? 'Active' : 'Inactive'}
                              </span>
                            </td>
                            <td>
                              <span className="text-xs text-slate-500">
                                {employee.createdAt ? format(new Date(employee.createdAt), 'MMM d, yyyy') : '—'}
                              </span>
                            </td>
                            <td>
                              <button
                                onClick={() => handleToggleActive(employee.id, employee.active)}
                                disabled={actionLoading === employee.id}
                                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all disabled:opacity-50 ${
                                  employee.active
                                    ? 'bg-rose-950 text-rose-300 border border-rose-800 hover:bg-rose-900'
                                    : 'bg-emerald-950 text-emerald-300 border border-emerald-800 hover:bg-emerald-900'
                                }`}
                              >
                                {actionLoading === employee.id ? (
                                  <span className="w-3 h-3 border border-current border-t-transparent rounded-full animate-spin" />
                                ) : employee.active ? (
                                  <><UserX size={12} /> Deactivate</>
                                ) : (
                                  <><UserCheck size={12} /> Activate</>
                                )}
                              </button>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>

                {totalPages > 1 && (
                  <div className="flex items-center justify-between px-4 py-3 border-t border-slate-800">
                    <p className="text-sm text-slate-500">
                      Showing {page * 20 + 1}–{Math.min((page + 1) * 20, totalElements)} of {totalElements}
                    </p>
                    <div className="flex gap-2">
                      <button onClick={() => setPage(page - 1)} disabled={page === 0} className="px-3 py-1.5 text-sm rounded-lg text-slate-400 hover:bg-slate-800 disabled:opacity-30">Previous</button>
                      <button onClick={() => setPage(page + 1)} disabled={page >= totalPages - 1} className="px-3 py-1.5 text-sm rounded-lg text-slate-400 hover:bg-slate-800 disabled:opacity-30">Next</button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      )}

      {activeTab === 'departments' && (
        <div className="glass-card p-5">
          <h3 className="font-semibold text-white mb-4">Department Overview</h3>
          {departments.length === 0 ? (
            <div className="text-center py-10 text-slate-500">No departments configured</div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
              {departments.map((dept) => {
                const empCount = employees.filter((e) => e.department === dept).length;
                return (
                  <div key={dept} className="bg-slate-800/50 border border-slate-700 rounded-xl p-4 hover:border-indigo-500/40 transition-colors">
                    <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center mb-3">
                      <Building size={16} className="text-white" />
                    </div>
                    <p className="text-sm font-medium text-white">{dept}</p>
                    <p className="text-xs text-slate-500 mt-0.5">{empCount} employees</p>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default AdminPage;
