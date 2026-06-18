import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import {
  CheckCircle,
  XCircle,
  Filter,
  Eye,
  Clock,
  AlertTriangle,
  ChevronDown,
} from 'lucide-react';
import {
  useGetPendingApprovalsQuery,
  useApproveExpenseMutation,
  useRejectExpenseMutation,
} from '../../store/api/expenseApi';
import RiskBadge from '../../components/ui/RiskBadge';
import StatusBadge from '../../components/ui/StatusBadge';
import Modal from '../../components/ui/Modal';
import LoadingSkeleton from '../../components/ui/LoadingSkeleton';
import { ExpenseStatus, RiskLevel } from '../../types';

const ApprovalQueuePage: React.FC = () => {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [riskFilter, setRiskFilter] = useState('');
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectNotes, setRejectNotes] = useState('');
  const [selectedExpenseId, setSelectedExpenseId] = useState<number | null>(null);
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  const { data, isLoading, refetch } = useGetPendingApprovalsQuery({
    page,
    size: 15,
    riskLevel: riskFilter || undefined,
  });

  const [approveExpense] = useApproveExpenseMutation();
  const [rejectExpense] = useRejectExpenseMutation();

  const expenses = data?.data?.content ?? [];
  const totalPages = data?.data?.totalPages ?? 1;
  const totalElements = data?.data?.totalElements ?? 0;

  const handleApprove = async (expenseId: number) => {
    setActionLoading(expenseId);
    try {
      await approveExpense({ expenseId }).unwrap();
      refetch();
    } finally {
      setActionLoading(null);
    }
  };

  const openRejectModal = (expenseId: number) => {
    setSelectedExpenseId(expenseId);
    setRejectNotes('');
    setRejectModalOpen(true);
  };

  const handleReject = async () => {
    if (!selectedExpenseId) return;
    setActionLoading(selectedExpenseId);
    try {
      await rejectExpense({ expenseId: selectedExpenseId, notes: rejectNotes }).unwrap();
      setRejectModalOpen(false);
      refetch();
    } finally {
      setActionLoading(null);
    }
  };

  // Stats from current page
  const highRisk = expenses.filter((e) => e.riskLevel === 'HIGH').length;
  const totalAmount = expenses.reduce((s, e) => s + e.amount, 0);

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-white">Approval Queue</h2>
          <p className="text-sm text-slate-500 mt-0.5">{totalElements} expenses awaiting your review</p>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {[
          { label: 'Pending Review', value: totalElements, icon: Clock, color: 'card-amber', gradient: 'from-amber-500 to-orange-600' },
          { label: 'High Risk', value: highRisk, icon: AlertTriangle, color: 'card-rose', gradient: 'from-rose-500 to-pink-600' },
          { label: 'Total Amount', value: `$${totalAmount.toLocaleString(undefined, { maximumFractionDigits: 0 })}`, icon: CheckCircle, color: 'card-indigo', gradient: 'from-indigo-500 to-violet-600' },
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

      {/* Filter */}
      <div className="glass-card p-4">
        <div className="flex items-center gap-3">
          <Filter size={15} className="text-slate-500" />
          <span className="text-sm text-slate-400">Filter by risk:</span>
          <div className="flex gap-2">
            {[
              { value: '', label: 'All' },
              { value: 'HIGH', label: 'High' },
              { value: 'MEDIUM', label: 'Medium' },
              { value: 'LOW', label: 'Low' },
            ].map((opt) => (
              <button
                key={opt.value}
                onClick={() => { setRiskFilter(opt.value); setPage(0); }}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                  riskFilter === opt.value
                    ? 'bg-indigo-600 text-white'
                    : 'bg-slate-800 text-slate-400 hover:text-white hover:bg-slate-700'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="glass-card overflow-hidden">
        {isLoading ? (
          <LoadingSkeleton rows={6} />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>Title</th>
                    <th>Amount</th>
                    <th>Category</th>
                    <th>Submitted</th>
                    <th>Status</th>
                    <th>Risk</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {expenses.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="text-center py-16">
                        <div className="flex flex-col items-center gap-3">
                          <div className="w-14 h-14 rounded-2xl bg-emerald-950 border border-emerald-800 flex items-center justify-center">
                            <CheckCircle size={24} className="text-emerald-400" />
                          </div>
                          <p className="font-medium text-slate-400">All caught up!</p>
                          <p className="text-sm text-slate-600">No expenses pending review</p>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    expenses.map((expense) => (
                      <tr key={expense.id}>
                        <td>
                          <div className="flex items-center gap-2.5">
                            <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                              {expense.submittedBy?.username?.slice(0, 2).toUpperCase() || 'U'}
                            </div>
                            <div>
                              <p className="text-sm font-medium text-white">{expense.submittedBy?.username || '—'}</p>
                              <p className="text-xs text-slate-500">{expense.submittedBy?.role || 'Employee'}</p>
                            </div>
                          </div>
                        </td>
                        <td>
                          <p className="text-sm font-medium text-white truncate max-w-[160px]">{expense.title}</p>
                          <p className="text-xs text-slate-500 truncate">{expense.merchantName}</p>
                        </td>
                        <td>
                          <span className="font-bold text-white">${expense.amount.toLocaleString()}</span>
                        </td>
                        <td>
                          <span className="text-sm text-slate-400">{expense.category?.name || '—'}</span>
                        </td>
                        <td>
                          <span className="text-sm text-slate-400">
                            {expense.submittedAt ? format(new Date(expense.submittedAt), 'MMM d') : '—'}
                          </span>
                        </td>
                        <td>
                          <StatusBadge status={expense.status as ExpenseStatus} size="sm" />
                        </td>
                        <td>
                          <RiskBadge riskLevel={expense.riskLevel as RiskLevel} score={expense.riskScore} size="sm" />
                        </td>
                        <td>
                          <div className="flex items-center gap-1.5">
                            <button
                              onClick={() => navigate(`/expenses/${expense.id}`)}
                              className="p-1.5 rounded-lg text-slate-500 hover:text-indigo-400 hover:bg-indigo-950 transition-colors"
                              title="View details"
                            >
                              <Eye size={15} />
                            </button>
                            <button
                              onClick={() => handleApprove(expense.id)}
                              disabled={actionLoading === expense.id}
                              className="p-1.5 rounded-lg text-slate-500 hover:text-emerald-400 hover:bg-emerald-950 transition-colors disabled:opacity-50"
                              title="Approve"
                            >
                              <CheckCircle size={15} />
                            </button>
                            <button
                              onClick={() => openRejectModal(expense.id)}
                              disabled={actionLoading === expense.id}
                              className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-950 transition-colors disabled:opacity-50"
                              title="Reject"
                            >
                              <XCircle size={15} />
                            </button>
                          </div>
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
                <p className="text-sm text-slate-500">Page {page + 1} of {totalPages}</p>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage(page - 1)}
                    disabled={page === 0}
                    className="px-3 py-1.5 text-sm rounded-lg text-slate-400 hover:bg-slate-800 disabled:opacity-30 disabled:cursor-not-allowed"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setPage(page + 1)}
                    disabled={page >= totalPages - 1}
                    className="px-3 py-1.5 text-sm rounded-lg text-slate-400 hover:bg-slate-800 disabled:opacity-30 disabled:cursor-not-allowed"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Reject Modal */}
      <Modal
        isOpen={rejectModalOpen}
        onClose={() => setRejectModalOpen(false)}
        title="Reject Expense"
        footer={
          <>
            <button onClick={() => setRejectModalOpen(false)} className="btn-secondary text-sm">Cancel</button>
            <button
              onClick={handleReject}
              disabled={!rejectNotes.trim() || actionLoading !== null}
              className="btn-primary text-sm bg-rose-600 hover:bg-rose-700"
              style={{ background: 'linear-gradient(135deg, #E11D48, #F43F5E)' }}
            >
              <XCircle size={15} />
              Reject Expense
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-400">
            Please provide a reason for rejecting this expense. This will be visible to the employee.
          </p>
          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">
              Rejection Notes <span className="text-rose-400">*</span>
            </label>
            <textarea
              value={rejectNotes}
              onChange={(e) => setRejectNotes(e.target.value)}
              rows={4}
              placeholder="Explain why this expense is being rejected..."
              className="form-input resize-none w-full"
              autoFocus
            />
            <p className="text-xs text-slate-600 mt-1">{rejectNotes.length}/500 characters</p>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default ApprovalQueuePage;
