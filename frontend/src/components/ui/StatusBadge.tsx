import React from 'react';
import { ExpenseStatus } from '../../types';

interface StatusBadgeProps {
  status: ExpenseStatus;
  size?: 'sm' | 'md';
}

const statusConfig: Record<ExpenseStatus, { label: string; className: string; dot: string }> = {
  DRAFT: {
    label: 'Draft',
    className: 'bg-slate-800 text-slate-300 border border-slate-600',
    dot: 'bg-slate-400',
  },
  SUBMITTED: {
    label: 'Submitted',
    className: 'bg-blue-950 text-blue-300 border border-blue-800',
    dot: 'bg-blue-400',
  },
  UNDER_REVIEW: {
    label: 'Under Review',
    className: 'bg-amber-950 text-amber-300 border border-amber-800',
    dot: 'bg-amber-400',
  },
  APPROVED: {
    label: 'Approved',
    className: 'bg-emerald-950 text-emerald-300 border border-emerald-800',
    dot: 'bg-emerald-400',
  },
  REJECTED: {
    label: 'Rejected',
    className: 'bg-rose-950 text-rose-300 border border-rose-800',
    dot: 'bg-rose-400',
  },
  REIMBURSED: {
    label: 'Reimbursed',
    className: 'bg-violet-950 text-violet-300 border border-violet-800',
    dot: 'bg-violet-400',
  },
};

const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  const config = statusConfig[status] || {
    label: status,
    className: 'bg-slate-800 text-slate-300 border border-slate-600',
    dot: 'bg-slate-400',
  };

  const sizeClass = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-3 py-1 text-xs';

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full font-semibold ${sizeClass} ${config.className}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${config.dot}`} />
      {config.label}
    </span>
  );
};

export default StatusBadge;
