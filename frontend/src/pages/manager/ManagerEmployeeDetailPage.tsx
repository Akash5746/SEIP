import React, { useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { format } from 'date-fns';
import { useSelector } from 'react-redux';
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle,
  Clock,
  DollarSign,
  Eye,
  FileText,
  Receipt,
  UserCircle2,
  XCircle,
} from 'lucide-react';
import {
  useApproveExpenseMutation,
  useGetManagerExpenseByIdQuery,
  useGetManagerEmployeeExpensesQuery,
  useRejectExpenseMutation,
  useRequestExpenseChangesMutation,
} from '../../store/api/expenseApi';
import { useGetEmployeeReportQuery } from '../../store/api/analyticsApi';
import { useGetEmployeeByAuthUserIdQuery } from '../../store/api/userApi';
import StatsCard from '../../components/ui/StatsCard';
import StatusBadge from '../../components/ui/StatusBadge';
import RiskBadge from '../../components/ui/RiskBadge';
import LoadingSkeleton, { StatsCardSkeleton } from '../../components/ui/LoadingSkeleton';
import Modal from '../../components/ui/Modal';
import { ExpenseStatus, RiskLevel } from '../../types';
import { RootState } from '../../store';
import { openReceiptInNewTab } from '../../utils/receipts';
import { formatRoleLabel } from '../../utils/roles';

const formatAmount = (amount: number, currency = 'INR') =>
  `${currency} ${amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const ManagerEmployeeDetailPage: React.FC = () => {
  const navigate = useNavigate();
  const accessToken = useSelector((state: RootState) => state.auth.accessToken);
  const { authUserId: authUserIdParam } = useParams<{ authUserId: string }>();
  const authUserId = authUserIdParam ? Number(authUserIdParam) : Number.NaN;

  const [reviewAction, setReviewAction] = useState<'reject' | 'changes' | null>(null);
  const [selectedExpenseId, setSelectedExpenseId] = useState<number | null>(null);
  const [selectedExpenseDetailId, setSelectedExpenseDetailId] = useState<number | null>(null);
  const [reviewNotes, setReviewNotes] = useState('');
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  const { data: employeeData, isLoading: employeeLoading } = useGetEmployeeByAuthUserIdQuery(authUserId, {
    skip: Number.isNaN(authUserId),
  });
  const { data: reportData, isLoading: reportLoading } = useGetEmployeeReportQuery(authUserId, {
    skip: Number.isNaN(authUserId),
  });
  const {
    data: expensesData,
    isLoading: expensesLoading,
    refetch: refetchExpenses,
  } = useGetManagerEmployeeExpensesQuery(
    { employeeAuthUserId: authUserId, page: 0, size: 50 },
    { skip: Number.isNaN(authUserId) }
  );

  const [approveExpense] = useApproveExpenseMutation();
  const [rejectExpense] = useRejectExpenseMutation();
  const [requestExpenseChanges] = useRequestExpenseChangesMutation();
  const { data: selectedExpenseData, isLoading: selectedExpenseLoading } = useGetManagerExpenseByIdQuery(
    selectedExpenseDetailId ?? 0,
    { skip: selectedExpenseDetailId === null }
  );

  const employee = employeeData?.data;
  const employeeReport = reportData?.data;
  const expenses = expensesData?.data?.content ?? [];
  const selectedExpense = selectedExpenseData?.data;

  const metrics = useMemo(() => {
    const pending = expenses.filter((expense) => expense.status === 'SUBMITTED' || expense.status === 'UNDER_REVIEW');
    const approved = expenses.filter((expense) => expense.status === 'APPROVED' || expense.status === 'REIMBURSED');
    const rejected = expenses.filter((expense) => expense.status === 'REJECTED');
    const highRisk = expenses.filter((expense) => expense.riskLevel === 'HIGH');

    return {
      pendingCount: pending.length,
      approvedCount: approved.length,
      rejectedCount: rejected.length,
      highRiskCount: highRisk.length,
      pendingAmount: pending.reduce((sum, expense) => sum + expense.amount, 0),
    };
  }, [expenses]);

  const openReviewModal = (mode: 'reject' | 'changes', expenseId: number) => {
    setReviewAction(mode);
    setSelectedExpenseId(expenseId);
    setReviewNotes('');
  };

  const handleOpenReceipt = async (receiptUrl: string) => {
    try {
      await openReceiptInNewTab(receiptUrl, accessToken);
    } catch {
      // Keep the modal open; opening the file is the only failed action here.
    }
  };

  const closeReviewModal = () => {
    setReviewAction(null);
    setSelectedExpenseId(null);
    setReviewNotes('');
  };

  const closeExpenseDetailModal = () => {
    setSelectedExpenseDetailId(null);
  };

  const refreshPageData = async () => {
    await Promise.all([refetchExpenses()]);
  };

  const handleApprove = async (expenseId: number) => {
    setActionLoading(expenseId);
    try {
      await approveExpense({ expenseId }).unwrap();
      await refreshPageData();
    } finally {
      setActionLoading(null);
    }
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
      await refreshPageData();
    } finally {
      setActionLoading(null);
    }
  };

  if (Number.isNaN(authUserId)) {
    return <div className="glass-card p-8 text-center text-slate-500">Invalid employee selection.</div>;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <button
            onClick={() => navigate('/dashboard')}
            className="mb-3 inline-flex items-center gap-2 text-sm text-slate-400 transition-colors hover:text-white"
          >
            <ArrowLeft size={15} />
            Back to Dashboard
          </button>
          <h2 className="text-2xl font-bold text-white">
            Review <span className="gradient-text">{employee?.username || 'Employee'}</span>
          </h2>
          <p className="mt-1 text-sm text-slate-400">
            Authorized employee detail view with expense analytics, history, risk alerts, and approval actions.
          </p>
        </div>
        <button onClick={() => navigate('/manager/queue')} className="btn-secondary text-sm">
          <Clock size={16} />
          Open Approval Queue
        </button>
      </div>

      {employeeLoading || reportLoading ? (
        <StatsCardSkeleton />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatsCard
            title="Total Claims"
            value={employeeReport?.totalClaims ?? 0}
            subtitle="All employee submissions"
            icon={Receipt}
            colorClass="card-indigo"
            gradient="from-indigo-500 to-violet-600"
          />
          <StatsCard
            title="Pending Approval"
            value={formatAmount(metrics.pendingAmount)}
            subtitle={`${metrics.pendingCount} expense(s) awaiting action`}
            icon={Clock}
            colorClass="card-amber"
            gradient="from-amber-500 to-orange-600"
          />
          <StatsCard
            title="Approved Amount"
            value={formatAmount(Number(employeeReport?.approvedAmount ?? 0))}
            subtitle={`${metrics.approvedCount} approved / reimbursed`}
            icon={DollarSign}
            colorClass="card-emerald"
            gradient="from-emerald-500 to-teal-600"
          />
          <StatsCard
            title="High Risk Alerts"
            value={metrics.highRiskCount}
            subtitle={`${metrics.rejectedCount} rejected expense(s)`}
            icon={AlertTriangle}
            colorClass="card-rose"
            gradient="from-rose-500 to-pink-600"
          />
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <div className="glass-card p-5">
          <div className="mb-4 flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 text-sm font-bold text-white">
              {employee?.username?.slice(0, 2).toUpperCase() || 'EM'}
            </div>
            <div>
              <h3 className="font-semibold text-white">{employee?.username || 'Employee'}</h3>
              <p className="text-sm text-slate-500">{formatRoleLabel(employee?.role)} · {employee?.department || 'No department'}</p>
            </div>
          </div>
          {employeeLoading ? (
            <LoadingSkeleton rows={4} />
          ) : (
            <div className="space-y-3 text-sm">
              <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-3">
                <p className="text-slate-500">Email</p>
                <p className="mt-1 font-medium text-white">{employee?.email || '—'}</p>
              </div>
              <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-3">
                <p className="text-slate-500">Total Amount</p>
                <p className="mt-1 font-semibold text-white">{formatAmount(Number(employeeReport?.totalAmount ?? 0))}</p>
              </div>
              <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-3">
                <p className="text-slate-500">Rejected Amount</p>
                <p className="mt-1 font-semibold text-white">{formatAmount(Number(employeeReport?.rejectedAmount ?? 0))}</p>
              </div>
              <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-3">
                <p className="text-slate-500">Employee Auth ID</p>
                <p className="mt-1 font-semibold text-white">{employee?.authUserId ?? authUserId}</p>
              </div>
            </div>
          )}
        </div>

        <div className="glass-card overflow-hidden xl:col-span-2">
          <div className="flex items-center justify-between border-b border-slate-800 p-5">
            <div>
              <h3 className="font-semibold text-white">Expense History</h3>
              <p className="mt-0.5 text-xs text-slate-500">Only this employee&apos;s expenses are shown here.</p>
            </div>
          </div>

          {expensesLoading ? (
            <LoadingSkeleton rows={8} />
          ) : expenses.length === 0 ? (
            <div className="py-16 text-center text-slate-500">No expenses found for this employee.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Expense</th>
                    <th>Category</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Risk</th>
                    <th>Date</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {expenses.map((expense) => {
                    const reviewable = expense.status === 'SUBMITTED' || expense.status === 'UNDER_REVIEW';

                    return (
                      <tr key={expense.id}>
                        <td>
                          <div>
                            <p className="text-sm font-medium text-white">{expense.title}</p>
                            <p className="text-xs text-slate-500">{expense.merchantName || expense.expenseNumber}</p>
                          </div>
                        </td>
                        <td className="text-sm text-slate-400">{expense.categoryName || expense.category?.name || '—'}</td>
                        <td className="font-semibold text-white">{formatAmount(expense.amount, expense.currency)}</td>
                        <td><StatusBadge status={expense.status as ExpenseStatus} size="sm" /></td>
                        <td><RiskBadge riskLevel={expense.riskLevel as RiskLevel} score={expense.riskScore} size="sm" /></td>
                        <td className="text-xs text-slate-500">
                          {expense.expenseDate ? format(new Date(expense.expenseDate), 'MMM d, yyyy') : '—'}
                        </td>
                          <td>
                            <div className="flex items-center gap-1.5">
                              <button
                                onClick={() => setSelectedExpenseDetailId(expense.id)}
                                className="rounded-lg p-1.5 text-slate-500 transition-colors hover:bg-indigo-950 hover:text-indigo-400"
                                title="View supporting documents"
                              >
                                <Eye size={15} />
                              </button>
                              {reviewable ? (
                                <>
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
                                </>
                              ) : (
                                <span className="text-xs text-slate-600">Read only</span>
                              )}
                            </div>
                          </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
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
              ? 'Provide clear instructions so the employee can update and resubmit the expense.'
              : 'Provide a rejection reason. The employee will see this note.'}
          </p>
          <textarea
            value={reviewNotes}
            onChange={(event) => setReviewNotes(event.target.value)}
            rows={4}
            className="form-input w-full resize-none"
            placeholder="Add review notes..."
          />
        </div>
      </Modal>

      <Modal
        isOpen={selectedExpenseDetailId !== null}
        onClose={closeExpenseDetailModal}
        title={selectedExpense?.title || 'Expense Detail'}
        size="xl"
      >
        {selectedExpenseLoading || !selectedExpense ? (
          <LoadingSkeleton rows={6} />
        ) : (
          <div className="space-y-5">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
                <p className="text-xs uppercase tracking-wide text-slate-500">Approval Status</p>
                <div className="mt-2">
                  <StatusBadge status={selectedExpense.status as ExpenseStatus} />
                </div>
              </div>
              <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
                <p className="text-xs uppercase tracking-wide text-slate-500">Amount</p>
                <p className="mt-2 text-lg font-semibold text-white">
                  {formatAmount(selectedExpense.amount, selectedExpense.currency)}
                </p>
              </div>
              <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
                <p className="text-xs uppercase tracking-wide text-slate-500">Submitted</p>
                <p className="mt-2 text-sm text-white">
                  {selectedExpense.submittedAt ? format(new Date(selectedExpense.submittedAt), 'MMM d, yyyy HH:mm') : 'Not submitted'}
                </p>
              </div>
              <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
                <p className="text-xs uppercase tracking-wide text-slate-500">Decision Date</p>
                <p className="mt-2 text-sm text-white">
                  {selectedExpense.reviewedAt ? format(new Date(selectedExpense.reviewedAt), 'MMM d, yyyy HH:mm') : 'Pending'}
                </p>
              </div>
            </div>

            <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
              <p className="text-xs uppercase tracking-wide text-slate-500">Manager Remarks</p>
              <p className="mt-2 text-sm text-slate-300">
                {selectedExpense.reviewNotes?.trim() || 'No remarks recorded yet.'}
              </p>
            </div>

            <div className="rounded-xl border border-slate-700 bg-slate-900/40 p-4">
              <div className="mb-3 flex items-center gap-2">
                <FileText size={16} className="text-indigo-400" />
                <p className="text-sm font-semibold text-white">Supporting Documents</p>
              </div>
              {!selectedExpense.receipts || selectedExpense.receipts.length === 0 ? (
                <p className="text-sm text-slate-500">No supporting documents attached.</p>
              ) : (
                <div className="space-y-3">
                  {selectedExpense.receipts.map((receipt) => (
                    <button
                      type="button"
                      key={receipt.id}
                      onClick={() => handleOpenReceipt(receipt.fileUrl)}
                      className="flex items-center justify-between rounded-xl border border-slate-700 bg-slate-950/40 px-3 py-2 transition-colors hover:border-indigo-500/30"
                    >
                      <div>
                        <p className="text-sm font-medium text-white">{receipt.fileName}</p>
                        <p className="text-xs text-slate-500">
                          {receipt.contentType} · {format(new Date(receipt.uploadTime), 'MMM d, yyyy')}
                        </p>
                      </div>
                      <span className="text-sm text-indigo-400">Open</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default ManagerEmployeeDetailPage;
