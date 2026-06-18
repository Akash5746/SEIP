import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { format } from 'date-fns';
import { Search, Plus, SlidersHorizontal, Eye, Trash2, ChevronDown } from 'lucide-react';
import { useGetMyExpensesQuery, useDeleteExpenseMutation } from '../../store/api/expenseApi';
import StatusBadge from '../../components/ui/StatusBadge';
import RiskBadge from '../../components/ui/RiskBadge';
import LoadingSkeleton from '../../components/ui/LoadingSkeleton';
import { ExpenseStatus, RiskLevel } from '../../types';

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: 'All Statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'SUBMITTED', label: 'Submitted' },
  { value: 'UNDER_REVIEW', label: 'Under Review' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'REIMBURSED', label: 'Reimbursed' },
];

const ExpenseListPage: React.FC = () => {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');

  const { data, isLoading } = useGetMyExpensesQuery({ page, size: 15, status: status || undefined });
  const [deleteExpense] = useDeleteExpenseMutation();

  const expenses = data?.data?.content ?? [];
  const totalPages = data?.data?.totalPages ?? 1;
  const totalElements = data?.data?.totalElements ?? 0;

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearch(searchInput);
    setPage(0);
  };

  const handleDelete = async (id: number, e: React.MouseEvent) => {
    e.stopPropagation();
    if (confirm('Are you sure you want to delete this expense?')) {
      await deleteExpense(id);
    }
  };

  const filteredExpenses = search
    ? expenses.filter(
        (e) =>
          e.title.toLowerCase().includes(search.toLowerCase()) ||
          e.merchantName.toLowerCase().includes(search.toLowerCase()) ||
          e.expenseNumber.toLowerCase().includes(search.toLowerCase())
      )
    : expenses;

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-white">My Expenses</h2>
          <p className="text-sm text-slate-500 mt-0.5">{totalElements} total expenses</p>
        </div>
        <button onClick={() => navigate('/expenses/new')} className="btn-primary text-sm">
          <Plus size={16} />
          New Expense
        </button>
      </div>

      {/* Filters */}
      <div className="glass-card p-4">
        <div className="flex flex-col sm:flex-row gap-3">
          {/* Search */}
          <form onSubmit={handleSearch} className="flex-1 relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search by title, merchant, or expense #..."
              className="form-input pl-9 pr-4 w-full"
            />
          </form>

          {/* Status filter */}
          <div className="relative">
            <SlidersHorizontal size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
            <select
              value={status}
              onChange={(e) => { setStatus(e.target.value); setPage(0); }}
              className="form-input pl-9 pr-8 appearance-none min-w-[160px]"
            >
              {STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
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
                    <th>Expense #</th>
                    <th>Title</th>
                    <th>Amount</th>
                    <th>Category</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Risk</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredExpenses.length === 0 ? (
                    <tr>
                      <td colSpan={8} className="text-center py-14 text-slate-500">
                        <div className="flex flex-col items-center gap-3">
                          <div className="w-16 h-16 rounded-2xl bg-slate-800 flex items-center justify-center">
                            <Search size={24} className="text-slate-600" />
                          </div>
                          <div>
                            <p className="font-medium text-slate-400">No expenses found</p>
                            <p className="text-sm text-slate-600 mt-1">Try adjusting your filters or create a new expense</p>
                          </div>
                          <button onClick={() => navigate('/expenses/new')} className="btn-primary text-sm mt-1">
                            <Plus size={15} /> Create Expense
                          </button>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    filteredExpenses.map((expense) => (
                      <tr
                        key={expense.id}
                        className="cursor-pointer"
                        onClick={() => navigate(`/expenses/${expense.id}`)}
                      >
                        <td>
                          <span className="font-mono text-xs text-slate-400">{expense.expenseNumber}</span>
                        </td>
                        <td>
                          <p className="font-medium text-white text-sm truncate max-w-[200px]">{expense.title}</p>
                          <p className="text-xs text-slate-500 truncate max-w-[200px]">{expense.merchantName}</p>
                        </td>
                        <td>
                          <span className="font-semibold text-white">
                            {expense.currency || '$'}{expense.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                          </span>
                        </td>
                        <td>
                          <span className="text-sm text-slate-400">{expense.category?.name || '—'}</span>
                        </td>
                        <td>
                          <span className="text-sm text-slate-400">
                            {format(new Date(expense.expenseDate), 'MMM d, yyyy')}
                          </span>
                        </td>
                        <td>
                          <StatusBadge status={expense.status as ExpenseStatus} size="sm" />
                        </td>
                        <td>
                          <RiskBadge riskLevel={expense.riskLevel as RiskLevel} size="sm" />
                        </td>
                        <td>
                          <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                            <button
                              onClick={() => navigate(`/expenses/${expense.id}`)}
                              className="p-1.5 rounded-lg text-slate-500 hover:text-indigo-400 hover:bg-indigo-950 transition-colors"
                              title="View details"
                            >
                              <Eye size={15} />
                            </button>
                            {(expense.status === 'DRAFT') && (
                              <button
                                onClick={(e) => handleDelete(expense.id, e)}
                                className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-950 transition-colors"
                                title="Delete"
                              >
                                <Trash2 size={15} />
                              </button>
                            )}
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
                <p className="text-sm text-slate-500">
                  Showing {page * 15 + 1}–{Math.min((page + 1) * 15, totalElements)} of {totalElements}
                </p>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage(page - 1)}
                    disabled={page === 0}
                    className="px-3 py-1.5 text-sm rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    Previous
                  </button>
                  <span className="px-3 py-1.5 text-sm text-slate-400">
                    {page + 1} / {totalPages}
                  </span>
                  <button
                    onClick={() => setPage(page + 1)}
                    disabled={page >= totalPages - 1}
                    className="px-3 py-1.5 text-sm rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default ExpenseListPage;
