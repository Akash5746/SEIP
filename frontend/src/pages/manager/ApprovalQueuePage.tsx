import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import {
  CheckCircle,
  Clock,
  Eye,
  Filter,
  AlertTriangle,
  XCircle,
} from 'lucide-react';
import {
  useApproveExpenseMutation,
  useGetPendingApprovalsQuery,
  useRejectExpenseMutation,
  useRequestExpenseChangesMutation,
} from '../../store/api/expenseApi';
import { useGetMyDepartmentEmployeesQuery } from '../../store/api/userApi';
import RiskBadge from '../../components/ui/RiskBadge';
import StatusBadge from '../../components/ui/StatusBadge';
import Modal from '../../components/ui/Modal';
import LoadingSkeleton from '../../components/ui/LoadingSkeleton';
import { ExpenseStatus, RiskLevel } from '../../types';

const formatAmount = (amount: number, currency = 'INR') =>
  `${currency} ${amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const ApprovalQueuePage: React.FC = () => {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [riskFilter, setRiskFilter] = useState('');
  const [reviewAction, setReviewAction] = useState<'reject' | 'changes' | null>(null);
  const [reviewNotes, setReviewNotes] = useState('');
  const [selectedExpenseId, setSelectedExpenseId] = useState<number | null>(null);
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  const { data, isLoading, refetch } = useGetPendingApprovalsQuery({
    page,
    size: 15,
    riskLevel: riskFilter || undefined,
  });
  const { data: departmentEmployeesData } = useGetMyDepartmentEmployeesQuery();

  const [approveExpense] = useApproveExpenseMutation();
  const [rejectExpense] = useRejectExpenseMutation();
  const [requestExpenseChanges] = useRequestExpenseChangesMutation();

  const expenses = data?.data?.content ?? [];
  const departmentEmployees = (departmentEmployeesData?.data ?? []).filter(
    (employee) => employee.active && employee.role === 'ROLE_EMPLOYEE'
  );
  const totalPages = data?.data?.totalPages ?? 1;
  const totalElements = data?.data?.totalElements ?? 0;

  const employeeMap = useMemo(
    () => new Map(departmentEmployees.map((employee) => [employee.authUserId ?? employee.id, employee])),
    [departmentEmployees]
  );

  const handleApprove = async (expenseId: number) => {
    setActionLoading(expenseId);
    try {
      await approveExpense({ expenseId }).unwrap();
      await refetch();
    } finally {
      setActionLoading(null);
    }
  };

  const openReviewModal = (mode: 'reject' | 'changes', expenseId: number) => {
    setReviewAction(mode);
    setSelectedExpenseId(expenseId);
    setReviewNotes('');
  };

  const closeReviewModal = () => {
    setReviewAction(null);
    setSelectedExpenseId(null);
    setReviewNotes('');
  };

  const handleReviewSubmit = async () => {
    if (!selectedExpenseId || !reviewAction || !reviewNotes.trim()) {
      return;
    }

    setActionLoading(selectedExpenseId);
    try {
      if (reviewAction === 'reject') {
        await rejectExpense({ expenseId: selectedExpenseId, notes: reviewNotes }).unwrap();
      } else {
        await requestExpenseChanges({ expenseId: selectedExpenseId, notes: reviewNotes }).unwrap();
      }
      closeReviewModal();
      await refetch();
    } finally {
      setActionLoading(null);
    }
  };

  const highRisk = expenses.filter((expense) => expense.riskLevel === 'HIGH').length;
  const totalAmount = expenses.reduce((sum, expense) => sum + expense.amount, 0);

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-white">Approval Queue</h2>
          <p className="mt-0.5 text-sm text-slate-500">{totalElements} department expenses awaiting your review</p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        {[
          { label: 'Pending Review', value: totalElements, icon: Clock, color: 'card-amber', gradient: 'from-amber-500 to-orange-600' },
          { label: 'High Risk', value: highRisk, icon: AlertTriangle, color: 'card-rose', gradient: 'from-rose-500 to-pink-600' },
          { label: 'Total Amount', value: formatAmount(totalAmount), icon: CheckCircle, color: 'card-indigo', gradient: 'from-indigo-500 to-violet-600' },
        ].map(({ label, value, icon: Icon, color, gradient }) => (
          <div key={label} className={`glass-card p-4 ${color}`}>
            <div className="flex items-center justify-between">
              <div>
                <p className="mb-1 text-xs text-slate-500">{label}</p>
                <p className="text-2xl font-bold text-white">{value}</p>
              </div>
              <div className={`flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br ${gradient}`}>
                <Icon size={18} className="text-white" />
              </div>
            </div>
          </div>
        ))}
      </div>

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
            ].map((option) => (
              <button
                key={option.value}
                onClick={() => {
                  setRiskFilter(option.value);
                  setPage(0);
                }}
                className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-all ${
                  riskFilter === option.value
                    ? 'bg-indigo-600 text-white'
                    : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-white'
                }`}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>
      </div>

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
                      <td colSpan={8} className="py-16 text-center">
                        <div className="flex flex-col items-center gap-3">
                          <div className="flex h-14 w-14 items-center justify-center rounded-2xl border border-emerald-800 bg-emerald-950">
                            <CheckCircle size={24} className="text-emerald-400" />
                          </div>
                          <p className="font-medium text-slate-400">All caught up</p>
                          <p className="text-sm text-slate-600">No department expenses are pending review</p>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    expenses.map((expense) => {
                      const employee = employeeMap.get(expense.employeeId ?? -1);

                      return (
                        <tr key={expense.id}>
                          <td>
                            <button
                              onClick={() => expense.employeeId && navigate(`/manager/employees/${expense.employeeId}`)}
                              className="flex items-center gap-2.5 text-left"
                            >
                              <div className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 text-xs font-bold text-white">
                                {(employee?.username || 'U').slice(0, 2).toUpperCase()}
                              </div>
                              <div>
                                <p className="text-sm font-medium text-white">
                                  {employee?.username || `Employee #${expense.employeeId ?? 'N/A'}`}
                                </p>
                                <p className="text-xs text-slate-500">{employee?.department || 'Department employee'}</p>
                              </div>
                            </button>
                          </td>
                          <td>
                            <p className="max-w-[160px] truncate text-sm font-medium text-white">{expense.title}</p>
                            <p className="truncate text-xs text-slate-500">{expense.merchantName}</p>
                          </td>
                          <td className="font-bold text-white">{formatAmount(expense.amount, expense.currency)}</td>
                          <td className="text-sm text-slate-400">{expense.categoryName || expense.category?.name || '—'}</td>
                          <td className="text-sm text-slate-400">
                            {expense.submittedAt ? format(new Date(expense.submittedAt), 'MMM d') : '—'}
                          </td>
                          <td><StatusBadge status={expense.status as ExpenseStatus} size="sm" /></td>
                          <td><RiskBadge riskLevel={expense.riskLevel as RiskLevel} score={expense.riskScore} size="sm" /></td>
                          <td>
                            <div className="flex items-center gap-1.5">
                              <button
                                onClick={() => expense.employeeId && navigate(`/manager/employees/${expense.employeeId}`)}
                                className="rounded-lg p-1.5 text-slate-500 transition-colors hover:bg-indigo-950 hover:text-indigo-400"
                                title="Open employee detail"
                              >
                                <Eye size={15} />
                              </button>
                              <button
                                onClick={() => handleApprove(expense.id)}
                                disabled={actionLoading === expense.id}
                                className="rounded-lg p-1.5 text-slate-500 transition-colors hover:bg-emerald-950 hover:text-emerald-400 disabled:opacity-50"
                                title="Approve"
                              >
                                <CheckCircle size={15} />
                              </button>
                              <button
                                onClick={() => openReviewModal('changes', expense.id)}
                                disabled={actionLoading === expense.id}
                                className="rounded-lg p-1.5 text-slate-500 transition-colors hover:bg-amber-950 hover:text-amber-400 disabled:opacity-50"
                                title="Request changes"
                              >
                                <Clock size={15} />
                              </button>
                              <button
                                onClick={() => openReviewModal('reject', expense.id)}
                                disabled={actionLoading === expense.id}
                                className="rounded-lg p-1.5 text-slate-500 transition-colors hover:bg-rose-950 hover:text-rose-400 disabled:opacity-50"
                                title="Reject"
                              >
                                <XCircle size={15} />
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-slate-800 px-4 py-3">
                <p className="text-sm text-slate-500">Page {page + 1} of {totalPages}</p>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage(page - 1)}
                    disabled={page === 0}
                    className="rounded-lg px-3 py-1.5 text-sm text-slate-400 hover:bg-slate-800 disabled:opacity-30 disabled:cursor-not-allowed"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setPage(page + 1)}
                    disabled={page >= totalPages - 1}
                    className="rounded-lg px-3 py-1.5 text-sm text-slate-400 hover:bg-slate-800 disabled:opacity-30 disabled:cursor-not-allowed"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      <Modal
        isOpen={reviewAction !== null}
        onClose={closeReviewModal}
        title={reviewAction === 'changes' ? 'Request Changes' : 'Reject Expense'}
        footer={(
          <>
            <button onClick={closeReviewModal} className="btn-secondary text-sm">Cancel</button>
            <button
              onClick={handleReviewSubmit}
              disabled={!reviewNotes.trim() || actionLoading !== null}
              className="btn-primary text-sm"
            >
              {reviewAction === 'changes' ? 'Request Changes' : 'Reject Expense'}
            </button>
          </>
        )}
      >
        <div className="space-y-4">
          <p className="text-sm text-slate-400">
            {reviewAction === 'changes'
              ? 'Tell the employee what needs to be fixed before approval.'
              : 'Provide a rejection reason that will be shown to the employee.'}
          </p>
          <textarea
            value={reviewNotes}
            onChange={(event) => setReviewNotes(event.target.value)}
            rows={4}
            placeholder="Add review notes..."
            className="form-input w-full resize-none"
          />
        </div>
      </Modal>
    </div>
  );
};

export default ApprovalQueuePage;
