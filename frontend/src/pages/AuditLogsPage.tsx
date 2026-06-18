import React, { useState } from 'react';
import { format } from 'date-fns';
import { Search, Filter, Download, Clock, User, Database, Server } from 'lucide-react';
import { useGetAuditLogsQuery } from '../store/api/auditApi';
import LoadingSkeleton from '../components/ui/LoadingSkeleton';

const ACTION_COLORS: Record<string, string> = {
  CREATE: 'bg-emerald-950 text-emerald-300 border border-emerald-800',
  UPDATE: 'bg-blue-950 text-blue-300 border border-blue-800',
  DELETE: 'bg-rose-950 text-rose-300 border border-rose-800',
  LOGIN: 'bg-indigo-950 text-indigo-300 border border-indigo-800',
  LOGOUT: 'bg-slate-800 text-slate-300 border border-slate-600',
  APPROVE: 'bg-emerald-950 text-emerald-300 border border-emerald-800',
  REJECT: 'bg-rose-950 text-rose-300 border border-rose-800',
  SUBMIT: 'bg-violet-950 text-violet-300 border border-violet-800',
  VIEW: 'bg-sky-950 text-sky-300 border border-sky-800',
  FRAUD_ANALYSIS: 'bg-amber-950 text-amber-300 border border-amber-800',
  EXPORT: 'bg-teal-950 text-teal-300 border border-teal-800',
};

const RESOURCE_ICONS: Record<string, React.ReactNode> = {
  EXPENSE: <Database size={12} />,
  USER: <User size={12} />,
  AUTH: <Server size={12} />,
  FRAUD: <Database size={12} />,
};

const AuditLogsPage: React.FC = () => {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [resourceFilter, setResourceFilter] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [showFilters, setShowFilters] = useState(false);

  const { data, isLoading } = useGetAuditLogsQuery({
    page,
    size: 20,
    search: search || undefined,
    action: actionFilter || undefined,
    resourceType: resourceFilter || undefined,
    startDate: startDate || undefined,
    endDate: endDate || undefined,
  });

  const logs = data?.data?.content ?? [];
  const totalPages = data?.data?.totalPages ?? 1;
  const totalElements = data?.data?.totalElements ?? 0;

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearch(searchInput);
    setPage(0);
  };

  const handleExportCSV = () => {
    const headers = ['Timestamp', 'User', 'Action', 'Resource', 'Resource ID', 'IP', 'Service'];
    const rows = logs.map((log) => [
      log.timestamp,
      log.username,
      log.action,
      log.resourceType,
      log.resourceId,
      log.ipAddress ?? '',
      log.service ?? '',
    ]);
    const csv = [headers, ...rows].map((r) => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `audit-logs-${format(new Date(), 'yyyy-MM-dd')}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const getActionColor = (action: string) => {
    const key = Object.keys(ACTION_COLORS).find((k) => action.toUpperCase().includes(k));
    return key ? ACTION_COLORS[key] : 'bg-slate-800 text-slate-300 border border-slate-700';
  };

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-white">Audit Logs</h2>
          <p className="text-sm text-slate-500 mt-0.5">{totalElements.toLocaleString()} events recorded</p>
        </div>
        <div className="flex gap-2">
          <button onClick={() => setShowFilters(!showFilters)} className="btn-secondary text-sm">
            <Filter size={15} />
            Filters
          </button>
          <button onClick={handleExportCSV} className="btn-secondary text-sm">
            <Download size={15} />
            Export
          </button>
        </div>
      </div>

      {/* Search */}
      <div className="glass-card p-4 space-y-3">
        <form onSubmit={handleSearch} className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search by username, action, resource ID..."
            className="form-input pl-9 w-full"
          />
        </form>

        {showFilters && (
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2 border-t border-slate-800">
            <div>
              <label className="block text-xs text-slate-500 mb-1">Action</label>
              <select
                value={actionFilter}
                onChange={(e) => { setActionFilter(e.target.value); setPage(0); }}
                className="form-input text-sm py-1.5"
              >
                <option value="">All Actions</option>
                {Object.keys(ACTION_COLORS).map((a) => <option key={a} value={a}>{a}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs text-slate-500 mb-1">Resource Type</label>
              <select
                value={resourceFilter}
                onChange={(e) => { setResourceFilter(e.target.value); setPage(0); }}
                className="form-input text-sm py-1.5"
              >
                <option value="">All Resources</option>
                {Object.keys(RESOURCE_ICONS).map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs text-slate-500 mb-1">From Date</label>
              <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="form-input text-sm py-1.5" />
            </div>
            <div>
              <label className="block text-xs text-slate-500 mb-1">To Date</label>
              <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="form-input text-sm py-1.5" />
            </div>
          </div>
        )}
      </div>

      {/* Table */}
      <div className="glass-card overflow-hidden">
        {isLoading ? (
          <LoadingSkeleton rows={10} />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Timestamp</th>
                    <th>User</th>
                    <th>Action</th>
                    <th>Resource</th>
                    <th>Resource ID</th>
                    <th>Service</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="text-center py-14 text-slate-500">
                        No audit logs found for the current filters
                      </td>
                    </tr>
                  ) : (
                    logs.map((log) => (
                      <tr key={log.id}>
                        <td>
                          <div className="flex items-center gap-1.5 text-slate-400 text-xs">
                            <Clock size={11} />
                            <span className="whitespace-nowrap">
                              {format(new Date(log.timestamp), 'MMM d, HH:mm:ss')}
                            </span>
                          </div>
                        </td>
                        <td>
                          <div className="flex items-center gap-2">
                            <div className="w-6 h-6 rounded-md bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                              {log.username?.slice(0, 1).toUpperCase() || '?'}
                            </div>
                            <div>
                              <p className="text-sm text-white">{log.username || log.userId}</p>
                            </div>
                          </div>
                        </td>
                        <td>
                          <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ${getActionColor(log.action)}`}>
                            {log.action}
                          </span>
                        </td>
                        <td>
                          <div className="flex items-center gap-1.5 text-sm text-slate-400">
                            {RESOURCE_ICONS[log.resourceType] || <Database size={12} />}
                            {log.resourceType}
                          </div>
                        </td>
                        <td>
                          <span className="font-mono text-xs text-slate-500">{log.resourceId || '—'}</span>
                        </td>
                        <td>
                          <span className="text-xs text-slate-500">{log.service || '—'}</span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between px-4 py-3 border-t border-slate-800">
                <p className="text-sm text-slate-500">
                  Showing {page * 20 + 1}–{Math.min((page + 1) * 20, totalElements)} of {totalElements.toLocaleString()}
                </p>
                <div className="flex gap-2">
                  <button onClick={() => setPage(page - 1)} disabled={page === 0} className="px-3 py-1.5 text-sm rounded-lg text-slate-400 hover:bg-slate-800 disabled:opacity-30">Previous</button>
                  <span className="px-3 py-1.5 text-sm text-slate-400">{page + 1} / {totalPages}</span>
                  <button onClick={() => setPage(page + 1)} disabled={page >= totalPages - 1} className="px-3 py-1.5 text-sm rounded-lg text-slate-400 hover:bg-slate-800 disabled:opacity-30">Next</button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default AuditLogsPage;
