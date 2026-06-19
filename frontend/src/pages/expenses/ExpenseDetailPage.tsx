import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import {
  ArrowLeft,
  Calendar,
  Building,
  Tag,
  DollarSign,
  FileText,
  Shield,
  AlertTriangle,
  CheckCircle,
  Download,
  ExternalLink,
  Hash,
  Send,
} from 'lucide-react';
import { useGetExpenseByIdQuery, useSubmitExpenseMutation } from '../../store/api/expenseApi';
import { useGetFraudAnalysisQuery } from '../../store/api/fraudApi';
import StatusBadge from '../../components/ui/StatusBadge';
import RiskBadge from '../../components/ui/RiskBadge';
import LoadingSkeleton from '../../components/ui/LoadingSkeleton';
import { ExpenseStatus, RiskLevel } from '../../types';

const ExpenseDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const expenseId = parseInt(id ?? '0', 10);
  const [activeTab, setActiveTab] = useState<'details' | 'fraud' | 'receipts'>('details');
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { data: expenseData, isLoading: expenseLoading } = useGetExpenseByIdQuery(expenseId, { skip: !expenseId });
  const { data: fraudData, isLoading: fraudLoading } = useGetFraudAnalysisQuery(expenseId, { skip: !expenseId });
  const [submitExpense, { isLoading: isSubmitting }] = useSubmitExpenseMutation();

  const expense = expenseData?.data;
  const fraud = fraudData?.data;

  if (expenseLoading) {
    return (
      <div className="space-y-4">
        <div className="skeleton h-8 w-48" />
        <LoadingSkeleton variant="card" />
      </div>
    );
  }

  if (!expense) {
    return (
      <div className="text-center py-20">
        <p className="text-slate-500 text-lg">Expense not found.</p>
        <button onClick={() => navigate('/expenses')} className="btn-primary mt-4 text-sm">
          <ArrowLeft size={15} /> Back to Expenses
        </button>
      </div>
    );
  }

  const riskScoreColor =
    (expense.riskScore || 0) >= 70
      ? 'text-rose-400'
      : (expense.riskScore || 0) >= 40
      ? 'text-amber-400'
      : 'text-emerald-400';

  const handleSubmitForApproval = async () => {
    setSubmitError(null);

    try {
      await submitExpense(expenseId).unwrap();
    } catch {
      setSubmitError('Failed to submit expense. Please try again.');
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-5">
      {/* Back + header */}
      <div className="flex items-start gap-4">
        <button
          onClick={() => navigate('/expenses')}
          className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors mt-0.5"
        >
          <ArrowLeft size={18} />
        </button>
        <div className="flex-1">
          <div className="flex items-center gap-3 flex-wrap">
            <h2 className="text-xl font-bold text-white">{expense.title}</h2>
            <StatusBadge status={expense.status as ExpenseStatus} />
            <RiskBadge riskLevel={expense.riskLevel as RiskLevel} score={expense.riskScore} />
          </div>
          <div className="flex items-center gap-3 mt-1.5 text-sm text-slate-500">
            <span className="flex items-center gap-1.5">
              <Hash size={12} /> {expense.expenseNumber}
            </span>
            <span>·</span>
            <span>
              {expense.submittedAt
                ? `Submitted ${format(new Date(expense.submittedAt), 'MMM d, yyyy')}`
                : `Created ${format(new Date(expense.createdAt), 'MMM d, yyyy')}`}
            </span>
          </div>
        </div>
        <div className="text-right space-y-3">
          <p className="text-3xl font-bold gradient-text">
            {expense.currency} {expense.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
          </p>
          {expense.status === 'DRAFT' && (
            <button
              onClick={handleSubmitForApproval}
              disabled={isSubmitting}
              className="btn-primary text-sm ml-auto"
            >
              {isSubmitting ? (
                <span className="flex items-center gap-2">
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Submitting...
                </span>
              ) : (
                <span className="flex items-center gap-2">
                  <Send size={15} />
                  Submit for Approval
                </span>
              )}
            </button>
          )}
        </div>
      </div>

      {submitError && (
        <div className="rounded-lg border border-rose-800 bg-rose-950 px-4 py-3 text-sm text-rose-300">
          {submitError}
        </div>
      )}

      {/* Tabs */}
      <div className="flex gap-1 bg-slate-800/50 p-1 rounded-xl w-fit">
        {[
          { key: 'details', label: 'Details', icon: FileText },
          { key: 'fraud', label: 'Fraud Analysis', icon: Shield },
          { key: 'receipts', label: `Receipts (${expense.receipts?.length ?? 0})`, icon: Download },
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

      {/* Details Tab */}
      {activeTab === 'details' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
          {/* Main info */}
          <div className="lg:col-span-2 space-y-4">
            <div className="glass-card p-5">
              <h3 className="font-semibold text-white mb-4">Expense Information</h3>
              <div className="grid grid-cols-2 gap-4">
                {[
                  { icon: Building, label: 'Merchant', value: expense.merchantName },
                  { icon: Calendar, label: 'Date', value: format(new Date(expense.expenseDate), 'MMMM d, yyyy') },
                  { icon: Tag, label: 'Category', value: expense.category?.name || '—' },
                  { icon: DollarSign, label: 'Currency', value: expense.currency },
                ].map(({ icon: Icon, label, value }) => (
                  <div key={label} className="space-y-1">
                    <div className="flex items-center gap-1.5 text-xs text-slate-500">
                      <Icon size={12} /> {label}
                    </div>
                    <p className="text-sm font-medium text-white">{value}</p>
                  </div>
                ))}
              </div>
              {expense.description && (
                <div className="mt-4 pt-4 border-t border-slate-700">
                  <p className="text-xs text-slate-500 mb-1.5">Description</p>
                  <p className="text-sm text-slate-300">{expense.description}</p>
                </div>
              )}
              {expense.reviewNotes && (
                <div className="mt-4 pt-4 border-t border-slate-700">
                  <p className="text-xs text-slate-500 mb-1.5">Review Notes</p>
                  <p className="text-sm text-slate-300 bg-slate-800/50 rounded-lg p-3">{expense.reviewNotes}</p>
                </div>
              )}
            </div>

            {/* Line Items */}
            {expense.items && expense.items.length > 0 && (
              <div className="glass-card p-5">
                <h3 className="font-semibold text-white mb-4">Line Items</h3>
                <div className="space-y-2">
                  {expense.items.map((item, i) => (
                    <div key={i} className="flex items-center justify-between py-2 border-b border-slate-800 last:border-0">
                      <div>
                        <p className="text-sm text-white">{item.description}</p>
                        <p className="text-xs text-slate-500">Qty: {item.quantity}</p>
                      </div>
                      <p className="text-sm font-semibold text-white">
                        ${(item.amount * item.quantity).toFixed(2)}
                      </p>
                    </div>
                  ))}
                  <div className="flex justify-between pt-2 font-semibold">
                    <span className="text-slate-400">Total</span>
                    <span className="text-white">
                      ${expense.items.reduce((s, i) => s + i.amount * i.quantity, 0).toFixed(2)}
                    </span>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Sidebar info */}
          <div className="space-y-4">
            <div className="glass-card p-5">
              <h3 className="font-semibold text-white mb-4">Status Timeline</h3>
              <div className="space-y-3">
                {[
                  { label: 'Created', date: expense.createdAt, done: true },
                  { label: 'Submitted', date: expense.submittedAt, done: !!expense.submittedAt },
                  { label: 'Under Review', date: null, done: ['UNDER_REVIEW', 'APPROVED', 'REJECTED', 'REIMBURSED'].includes(expense.status) },
                  { label: expense.status === 'REJECTED' ? 'Rejected' : 'Approved', date: expense.updatedAt, done: ['APPROVED', 'REJECTED', 'REIMBURSED'].includes(expense.status) },
                  { label: 'Reimbursed', date: null, done: expense.status === 'REIMBURSED' },
                ].map(({ label, date, done }, i) => (
                  <div key={i} className="flex items-start gap-3">
                    <div className={`w-5 h-5 rounded-full flex-shrink-0 flex items-center justify-center mt-0.5 ${done ? 'bg-emerald-600' : 'bg-slate-800 border border-slate-700'}`}>
                      {done && <CheckCircle size={12} className="text-white" />}
                    </div>
                    <div>
                      <p className={`text-sm font-medium ${done ? 'text-white' : 'text-slate-600'}`}>{label}</p>
                      {date && done && (
                        <p className="text-xs text-slate-500">{format(new Date(date), 'MMM d, yyyy HH:mm')}</p>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Fraud Analysis Tab */}
      {activeTab === 'fraud' && (
        <div className="space-y-4">
          {fraudLoading ? (
            <LoadingSkeleton variant="card" />
          ) : fraud ? (
            <>
              {/* Risk Score Gauge */}
              <div className="glass-card p-6">
                <div className="flex items-center justify-between mb-6">
                  <div>
                    <h3 className="font-semibold text-white">AI Fraud Risk Score</h3>
                    <p className="text-sm text-slate-500 mt-0.5">Machine learning based risk assessment</p>
                  </div>
                  <RiskBadge riskLevel={fraud.riskLevel as RiskLevel} score={fraud.riskScore} />
                </div>

                <div className="flex items-center gap-8">
                  {/* Circular gauge */}
                  <div className="relative w-36 h-36 flex-shrink-0">
                    <svg viewBox="0 0 120 120" className="w-full h-full -rotate-90">
                      <circle cx="60" cy="60" r="50" fill="none" stroke="#1E293B" strokeWidth="12" />
                      <circle
                        cx="60"
                        cy="60"
                        r="50"
                        fill="none"
                        stroke={fraud.riskScore >= 70 ? '#F43F5E' : fraud.riskScore >= 40 ? '#F59E0B' : '#10B981'}
                        strokeWidth="12"
                        strokeDasharray={`${(fraud.riskScore / 100) * 314} 314`}
                        strokeLinecap="round"
                      />
                    </svg>
                    <div className="absolute inset-0 flex flex-col items-center justify-center">
                      <span className={`text-3xl font-bold ${riskScoreColor}`}>{fraud.riskScore}</span>
                      <span className="text-xs text-slate-500">/ 100</span>
                    </div>
                  </div>

                  <div className="flex-1 space-y-3">
                    <div className="flex justify-between text-sm">
                      <span className="text-slate-400">ML Fraud Probability</span>
                      <span className={`font-semibold ${riskScoreColor}`}>
                        {(fraud.mlFraudProbability * 100).toFixed(1)}%
                      </span>
                    </div>
                    <div className="w-full bg-slate-800 rounded-full h-2">
                      <div
                        className={`h-2 rounded-full transition-all ${fraud.riskScore >= 70 ? 'bg-rose-500' : fraud.riskScore >= 40 ? 'bg-amber-500' : 'bg-emerald-500'}`}
                        style={{ width: `${fraud.mlFraudProbability * 100}%` }}
                      />
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-slate-400">Duplicate Detected</span>
                      <span className={fraud.isDuplicate ? 'text-rose-400 font-semibold' : 'text-emerald-400'}>
                        {fraud.isDuplicate ? '⚠ Yes' : '✓ No'}
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Fraud Flags */}
              {fraud.flags && fraud.flags.length > 0 && (
                <div className="glass-card p-5">
                  <h3 className="font-semibold text-white mb-4 flex items-center gap-2">
                    <AlertTriangle size={16} className="text-amber-400" />
                    Risk Flags ({fraud.flags.length})
                  </h3>
                  <div className="space-y-3">
                    {fraud.flags.map((flag, i) => (
                      <div key={i} className="flex items-start gap-3 p-3 bg-slate-800/50 rounded-lg border border-slate-700">
                        <div className={`w-2 h-2 rounded-full mt-1.5 flex-shrink-0 ${
                          flag.riskContribution >= 30 ? 'bg-rose-500' : flag.riskContribution >= 15 ? 'bg-amber-500' : 'bg-yellow-500'
                        }`} />
                        <div className="flex-1">
                          <div className="flex items-center justify-between">
                            <p className="text-sm font-medium text-white">{flag.flagType.replace(/_/g, ' ')}</p>
                            <span className="text-xs text-slate-500">+{flag.riskContribution} pts</span>
                          </div>
                          <p className="text-xs text-slate-400 mt-0.5">{flag.flagDescription}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {fraud.flags.length === 0 && (
                <div className="glass-card p-8 text-center">
                  <div className="w-14 h-14 rounded-2xl bg-emerald-950 border border-emerald-800 flex items-center justify-center mx-auto mb-3">
                    <CheckCircle size={24} className="text-emerald-400" />
                  </div>
                  <p className="font-semibold text-white">No Fraud Flags Detected</p>
                  <p className="text-sm text-slate-500 mt-1">This expense passed all fraud checks</p>
                </div>
              )}
            </>
          ) : (
            <div className="glass-card p-8 text-center">
              <Shield size={32} className="text-slate-600 mx-auto mb-3" />
              <p className="text-slate-400">No fraud analysis available yet.</p>
            </div>
          )}
        </div>
      )}

      {/* Receipts Tab */}
      {activeTab === 'receipts' && (
        <div className="glass-card p-5">
          <h3 className="font-semibold text-white mb-4">Attached Receipts</h3>
          {!expense.receipts || expense.receipts.length === 0 ? (
            <div className="text-center py-10">
              <Download size={28} className="text-slate-600 mx-auto mb-3" />
              <p className="text-slate-500">No receipts attached</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {expense.receipts.map((receipt) => (
                <div key={receipt.id} className="flex items-center gap-3 p-3 bg-slate-800/50 rounded-lg border border-slate-700">
                  <div className="w-10 h-10 rounded-lg bg-indigo-950 border border-indigo-800 flex items-center justify-center flex-shrink-0">
                    <FileText size={18} className="text-indigo-400" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-white truncate">{receipt.fileName}</p>
                    <p className="text-xs text-slate-500">
                      {receipt.contentType} · {format(new Date(receipt.uploadTime), 'MMM d')}
                    </p>
                  </div>
                  <a
                    href={receipt.fileUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="p-2 text-slate-500 hover:text-indigo-400 transition-colors"
                  >
                    <ExternalLink size={15} />
                  </a>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default ExpenseDetailPage;
