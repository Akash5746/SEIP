import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import {
  AlertTriangle,
  ArrowRight,
  BarChart2,
  CalendarDays,
  CheckCircle,
  ChevronDown,
  Clock,
  DollarSign,
  PlusCircle,
  Receipt,
  TrendingUp,
  UserCircle2,
  Users,
  XCircle,
} from 'lucide-react';
import { endOfWeek, format, getISOWeek, startOfWeek } from 'date-fns';
import { RootState } from '../store';
import {
  useGetCategorySpendQuery,
  useGetDashboardStatsQuery,
  useGetManagerDashboardQuery,
  useGetMonthlySpendQuery,
  useGetRecentExpensesQuery,
} from '../store/api/analyticsApi';
import {
  useApproveExpenseMutation,
  useGetMyExpensesQuery,
  useGetPendingApprovalsQuery,
  useRejectExpenseMutation,
  useRequestExpenseChangesMutation,
} from '../store/api/expenseApi';
import { useGetMyDepartmentEmployeesQuery } from '../store/api/userApi';
import StatsCard from '../components/ui/StatsCard';
import StatusBadge from '../components/ui/StatusBadge';
import RiskBadge from '../components/ui/RiskBadge';
import MonthlySpendChart from '../components/charts/MonthlySpendChart';
import StatusDonutChart from '../components/charts/StatusDonutChart';
import SpendCandlestickChart from '../components/charts/SpendCandlestickChart';
import CategoryPieChart from '../components/charts/CategoryPieChart';
import LoadingSkeleton, { StatsCardSkeleton } from '../components/ui/LoadingSkeleton';
import Modal from '../components/ui/Modal';
import { Expense, ExpenseStatus, RiskLevel } from '../types';
import { hasRole } from '../utils/roles';

const formatAmount = (amount: number, currency = 'INR') =>
  `${currency} ${amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const calculatePercentChange = (current: number, previous: number) => {
  if (previous === 0) {
    return current === 0 ? 0 : 100;
  }

  return Math.round(((current - previous) / previous) * 100);
};

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useSelector((state: RootState) => state.auth);
  const isManager = hasRole(user?.role, 'ROLE_MANAGER');
  const currentYear = new Date().getFullYear();
  const currentMonth = new Date().getMonth() + 1;

  const [selectedWeekLabel, setSelectedWeekLabel] = useState<string | null>(null);
  const [selectedDepartmentEmployeeId, setSelectedDepartmentEmployeeId] = useState('');
  const [reviewAction, setReviewAction] = useState<'reject' | 'changes' | null>(null);
  const [selectedExpenseId, setSelectedExpenseId] = useState<number | null>(null);
  const [reviewNotes, setReviewNotes] = useState('');
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  const { data: personalStatsData, isLoading: personalStatsLoading } = useGetDashboardStatsQuery();
  const { data: monthlyData, isLoading: monthlyLoading } = useGetMonthlySpendQuery({ year: currentYear });
  const { data: categoryData, isLoading: categoryLoading } = useGetCategorySpendQuery({
    startDate: format(new Date(currentYear, 0, 1), 'yyyy-MM-dd'),
    endDate: format(new Date(), 'yyyy-MM-dd'),
  });
  const { data: recentData, isLoading: recentLoading } = useGetRecentExpensesQuery({ limit: 6 });
  const { data: expensesData, isLoading: expensesLoading } = useGetMyExpensesQuery({ page: 0, size: 200 });

  const { data: managerDashboardData, isLoading: managerStatsLoading } = useGetManagerDashboardQuery(undefined, {
    skip: !isManager,
  });
  const { data: departmentEmployeesData, isLoading: departmentEmployeesLoading } = useGetMyDepartmentEmployeesQuery(
    undefined,
    { skip: !isManager }
  );
  const {
    data: pendingApprovalsData,
    isLoading: pendingApprovalsLoading,
    refetch: refetchPendingApprovals,
  } = useGetPendingApprovalsQuery({ page: 0, size: 20 }, { skip: !isManager });

  const [approveExpense] = useApproveExpenseMutation();
  const [rejectExpense] = useRejectExpenseMutation();
  const [requestExpenseChanges] = useRequestExpenseChangesMutation();

  const personalStats = personalStatsData?.data;
  const monthlySpend = monthlyData?.data ?? [];
  const categorySpend = categoryData?.data ?? [];
  const recentExpenses = recentData?.data ?? [];
  const allExpenses = expensesData?.data?.content ?? [];
  const managerDashboard = managerDashboardData?.data;
  const departmentEmployees = departmentEmployeesData?.data ?? [];
  const visibleDepartmentEmployees = departmentEmployees.filter(
    (employee) => employee.active && employee.role === 'ROLE_EMPLOYEE'
  );
  const pendingApprovals = pendingApprovalsData?.data?.content ?? [];
  const selectedDepartmentEmployee = visibleDepartmentEmployees.find(
    (employee) => String(employee.authUserId ?? employee.id) === selectedDepartmentEmployeeId
  );
  const departmentEmployeePlaceholder = departmentEmployeesLoading
    ? 'Loading employees...'
    : visibleDepartmentEmployees.length === 0
      ? 'No employees found in your department.'
      : 'Select employee';

  useEffect(() => {
    if (!selectedDepartmentEmployeeId) {
      return;
    }

    if (!selectedDepartmentEmployee) {
      setSelectedDepartmentEmployeeId('');
    }
  }, [selectedDepartmentEmployee, selectedDepartmentEmployeeId]);

  const currentMonthStats = monthlySpend.find((entry) => entry.month === currentMonth);
  const previousMonthStats = currentMonth > 1
    ? monthlySpend.find((entry) => entry.month === currentMonth - 1)
    : undefined;

  const totalExpensesTrend = previousMonthStats
    ? {
        value: calculatePercentChange(currentMonthStats?.expenseCount ?? 0, previousMonthStats.expenseCount),
        label: 'vs last month',
      }
    : undefined;
  const thisMonthTrend = previousMonthStats
    ? {
        value: calculatePercentChange(currentMonthStats?.totalAmount ?? 0, previousMonthStats.totalAmount),
        label: 'vs last month',
      }
    : undefined;

  const statusDistribution = Object.entries(
    recentExpenses.reduce<Record<string, number>>((acc, expense) => {
      acc[expense.status] = (acc[expense.status] ?? 0) + 1;
      return acc;
    }, {})
  ).map(([label, value]) => ({ label: label.replace(/_/g, ' '), value }));

  const weeklyBuckets = useMemo(() => {
    const grouped = allExpenses.reduce<Record<string, { label: string; start: Date; end: Date; expenses: Expense[] }>>(
      (acc, expense) => {
        const baseDate = new Date(expense.expenseDate || expense.createdAt);
        const weekStart = startOfWeek(baseDate, { weekStartsOn: 1 });
        const weekEnd = endOfWeek(baseDate, { weekStartsOn: 1 });
        const key = format(weekStart, 'yyyy-MM-dd');

        if (!acc[key]) {
          acc[key] = {
            label: `W${getISOWeek(baseDate)} · ${format(weekStart, 'MMM d')}`,
            start: weekStart,
            end: weekEnd,
            expenses: [],
          };
        }

        acc[key].expenses.push(expense);
        return acc;
      },
      {}
    );

    return Object.values(grouped)
      .sort((a, b) => a.start.getTime() - b.start.getTime())
      .map((bucket, index, buckets) => {
        const total = bucket.expenses.reduce((sum, expense) => sum + expense.amount, 0);
        const previousTotal = index > 0
          ? buckets[index - 1].expenses.reduce((sum, expense) => sum + expense.amount, 0)
          : total;
        const nextTotal = index < buckets.length - 1
          ? buckets[index + 1].expenses.reduce((sum, expense) => sum + expense.amount, 0)
          : total;

        return {
          ...bucket,
          total,
          candle: {
            label: bucket.label,
            open: previousTotal,
            close: total,
            high: Math.max(previousTotal, total, nextTotal),
            low: Math.min(previousTotal, total, nextTotal),
          },
        };
      });
  }, [allExpenses]);

  const selectedWeek = weeklyBuckets.find((bucket) => bucket.label === selectedWeekLabel)
    ?? (weeklyBuckets.length > 0 ? weeklyBuckets[weeklyBuckets.length - 1] : undefined);

  useEffect(() => {
    if (!selectedWeekLabel && weeklyBuckets.length > 0) {
      setSelectedWeekLabel(weeklyBuckets[weeklyBuckets.length - 1].label);
    }
  }, [selectedWeekLabel, weeklyBuckets]);

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
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

  const handleApprove = async (expenseId: number) => {
    setActionLoading(expenseId);
    try {
      await approveExpense({ expenseId }).unwrap();
      await refetchPendingApprovals();
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
      await refetchPendingApprovals();
    } finally {
      setActionLoading(null);
    }
  };

  if (isManager) {
    return (
      <div className="space-y-6">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
          <div>
            <h2 className="text-2xl font-bold text-white">
              {getGreeting()}, <span className="gradient-text">{user?.username || 'Manager'}</span>
            </h2>
            <p className="mt-1 text-sm text-slate-400">
              Department metrics are isolated to your team. Select an employee to review their expenses and approvals.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <button onClick={() => navigate('/manager/queue')} className="btn-secondary text-sm">
              <CheckCircle size={16} />
              Approval Queue
            </button>
            <button onClick={() => navigate('/reports')} className="btn-primary text-sm">
              <BarChart2 size={16} />
              Department Reports
            </button>
          </div>
        </div>

        {managerStatsLoading ? (
          <StatsCardSkeleton />
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatsCard
              title="Department Employees"
              value={managerDashboard?.totalEmployees ?? 0}
              subtitle={managerDashboard?.departmentName || 'Department scope'}
              icon={Users}
              colorClass="card-indigo"
              gradient="from-indigo-500 to-violet-600"
            />
            <StatsCard
              title="Pending Approvals"
              value={managerDashboard?.pendingApprovals ?? 0}
              subtitle="Awaiting your review"
              icon={Clock}
              colorClass="card-amber"
              gradient="from-amber-500 to-orange-600"
            />
            <StatsCard
              title="Department Spend"
              value={formatAmount(Number(managerDashboard?.departmentExpensesThisMonth ?? 0))}
              subtitle="This month"
              icon={DollarSign}
              colorClass="card-emerald"
              gradient="from-emerald-500 to-teal-600"
            />
            <StatsCard
              title="High Risk Alerts"
              value={managerDashboard?.highRiskAlerts ?? 0}
              subtitle="Within your department"
              icon={AlertTriangle}
              colorClass="card-rose"
              gradient="from-rose-500 to-pink-600"
            />
          </div>
        )}

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="glass-card p-5">
            <div className="mb-3 flex items-center gap-2 text-white">
              <UserCircle2 size={18} className="text-indigo-400" />
              <h3 className="font-semibold">Your Personal Expense Snapshot</h3>
            </div>
            {personalStatsLoading ? (
              <LoadingSkeleton rows={3} />
            ) : (
              <div className="grid gap-3 sm:grid-cols-2">
                <div className="rounded-2xl border border-slate-700 bg-slate-900/40 p-4">
                  <p className="text-xs uppercase tracking-wide text-slate-500">Your expenses</p>
                  <p className="mt-2 text-2xl font-bold text-white">{personalStats?.totalExpenses ?? 0}</p>
                </div>
                <div className="rounded-2xl border border-slate-700 bg-slate-900/40 p-4">
                  <p className="text-xs uppercase tracking-wide text-slate-500">Your month spend</p>
                  <p className="mt-2 text-2xl font-bold text-white">
                    {formatAmount(Number(managerDashboard?.personalThisMonthAmount ?? 0))}
                  </p>
                </div>
              </div>
            )}
          </div>

          <div className="glass-card p-5">
            <div className="mb-3 flex items-center gap-2 text-white">
              <Users size={18} className="text-indigo-400" />
              <h3 className="font-semibold">Department Employees</h3>
            </div>
            <p className="mb-3 text-sm text-slate-500">
              Only employees in {managerDashboard?.departmentName || 'your department'} are listed here.
            </p>
            <div className="flex flex-col gap-3 sm:flex-row">
              <div className="relative flex-1">
                <select
                  value={selectedDepartmentEmployeeId}
                  onChange={(event) => setSelectedDepartmentEmployeeId(event.target.value)}
                  className="form-input w-full appearance-none pr-10"
                  disabled={departmentEmployeesLoading || visibleDepartmentEmployees.length === 0}
                >
                  <option value="">{departmentEmployeePlaceholder}</option>
                  {visibleDepartmentEmployees.map((employee) => (
                    <option key={employee.authUserId ?? employee.id} value={employee.authUserId ?? employee.id}>
                      {employee.username}
                    </option>
                  ))}
                </select>
                <ChevronDown size={16} className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-500" />
              </div>
              <button
                onClick={() => selectedDepartmentEmployee && navigate(`/manager/employees/${selectedDepartmentEmployeeId}`)}
                disabled={!selectedDepartmentEmployee}
                className="btn-primary text-sm disabled:cursor-not-allowed disabled:opacity-50"
              >
                <ArrowRight size={16} />
                Open Employee Detail
              </button>
            </div>
            {!departmentEmployeesLoading && visibleDepartmentEmployees.length === 0 && (
              <p className="mt-3 text-sm text-slate-500">No employees found in your department.</p>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
          <div className="glass-card p-5 xl:col-span-2">
            <div className="mb-5 flex items-center justify-between">
              <div>
                <h3 className="font-semibold text-white">Department Monthly Spending</h3>
                <p className="mt-0.5 text-xs text-slate-500">{currentYear} · scoped to your department</p>
              </div>
              <div className="flex items-center gap-2 text-xs font-medium text-emerald-400">
                <TrendingUp size={14} />
                <span>Department trend</span>
              </div>
            </div>
            {monthlyLoading ? (
              <div className="skeleton h-56 w-full" />
            ) : (
              <MonthlySpendChart data={monthlySpend} height={240} />
            )}
          </div>

          <div className="glass-card p-5">
            <div className="mb-4">
              <h3 className="font-semibold text-white">Category Mix</h3>
              <p className="mt-0.5 text-xs text-slate-500">Department spending by category</p>
            </div>
            {categoryLoading ? (
              <div className="skeleton h-56 w-full" />
            ) : categorySpend.length > 0 ? (
              <CategoryPieChart data={categorySpend} height={240} />
            ) : (
              <div className="flex h-56 items-center justify-center text-slate-600">No category data available</div>
            )}
          </div>
        </div>

        <div className="glass-card overflow-hidden">
          <div className="flex items-center justify-between border-b border-slate-800 p-5">
            <div>
              <h3 className="font-semibold text-white">Department Approval Queue</h3>
              <p className="mt-0.5 text-xs text-slate-500">Only pending expenses from employees in your department</p>
            </div>
            <button
              onClick={() => navigate('/manager/queue')}
              className="flex items-center gap-1.5 text-sm text-indigo-400 transition-colors hover:text-indigo-300"
            >
              View full queue <ArrowRight size={14} />
            </button>
          </div>

          {pendingApprovalsLoading ? (
            <LoadingSkeleton rows={6} />
          ) : pendingApprovals.length === 0 ? (
            <div className="py-16 text-center text-slate-500">No department expenses are waiting for review.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>Expense</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Risk</th>
                    <th>Submitted</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {pendingApprovals.map((expense) => {
                    const employee = visibleDepartmentEmployees.find(
                      (item) => (item.authUserId ?? item.id) === expense.employeeId
                    );
                    return (
                      <tr key={expense.id}>
                        <td>
                          <button
                            onClick={() => expense.employeeId && navigate(`/manager/employees/${expense.employeeId}`)}
                            className="text-left text-sm font-medium text-indigo-300 hover:text-indigo-200"
                          >
                            {employee?.username || `Employee #${expense.employeeId ?? 'N/A'}`}
                          </button>
                        </td>
                        <td>
                          <div>
                            <p className="text-sm font-medium text-white">{expense.title}</p>
                            <p className="text-xs text-slate-500">{expense.merchantName || expense.expenseNumber}</p>
                          </div>
                        </td>
                        <td className="font-semibold text-white">{formatAmount(expense.amount, expense.currency)}</td>
                        <td><StatusBadge status={expense.status as ExpenseStatus} size="sm" /></td>
                        <td><RiskBadge riskLevel={expense.riskLevel as RiskLevel} score={expense.riskScore} size="sm" /></td>
                        <td className="text-xs text-slate-500">
                          {expense.submittedAt ? format(new Date(expense.submittedAt), 'MMM d, yyyy') : '—'}
                        </td>
                        <td>
                          <div className="flex items-center gap-1.5">
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
                  })}
                </tbody>
              </table>
            </div>
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
                ? 'Explain what the employee needs to update before the expense can be approved.'
                : 'This rejection note will be visible to the employee.'}
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
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white">
            {getGreeting()}, <span className="gradient-text">{user?.username || 'User'}</span>
          </h2>
          <p className="mt-1 text-sm text-slate-400">
            {format(new Date(), 'EEEE, MMMM d, yyyy')} · your dashboard is scoped only to your expenses.
          </p>
        </div>
        <div className="flex gap-3">
          <button onClick={() => navigate('/reports')} className="btn-secondary text-sm">
            <BarChart2 size={16} />
            Reports
          </button>
          <button onClick={() => navigate('/expenses/new')} className="btn-primary text-sm">
            <PlusCircle size={16} />
            New Expense
          </button>
        </div>
      </div>

      {personalStatsLoading ? (
        <StatsCardSkeleton />
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <StatsCard
            title="Total Expenses"
            value={personalStats?.totalExpenses ?? 0}
            subtitle="All time submissions"
            icon={Receipt}
            trend={totalExpensesTrend}
            colorClass="card-indigo"
            gradient="from-indigo-500 to-violet-600"
          />
          <StatsCard
            title="Pending Approval"
            value={personalStats?.pendingCount ?? 0}
            subtitle="Awaiting review"
            icon={Clock}
            colorClass="card-amber"
            gradient="from-amber-500 to-orange-600"
          />
          <StatsCard
            title="This Month"
            value={formatAmount(Number(personalStats?.thisMonthAmount ?? 0))}
            subtitle="Your spend only"
            icon={DollarSign}
            trend={thisMonthTrend}
            colorClass="card-emerald"
            gradient="from-emerald-500 to-teal-600"
          />
          <StatsCard
            title="High Risk Alerts"
            value={personalStats?.highRiskAlerts ?? 0}
            subtitle="From your expenses"
            icon={AlertTriangle}
            colorClass="card-rose"
            gradient="from-rose-500 to-pink-600"
          />
        </div>
      )}

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="glass-card p-5 lg:col-span-2">
          <div className="mb-5 flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-white">Monthly Spending</h3>
              <p className="mt-0.5 text-xs text-slate-500">{currentYear} · scoped to your account</p>
            </div>
            <div className="flex items-center gap-2 text-xs font-medium text-emerald-400">
              <TrendingUp size={14} />
              <span>Year to date</span>
            </div>
          </div>
          {monthlyLoading ? (
            <div className="skeleton h-56 w-full" />
          ) : monthlySpend.length > 0 ? (
            <MonthlySpendChart data={monthlySpend} height={240} />
          ) : (
            <div className="flex h-56 items-center justify-center text-slate-600">No spending data available</div>
          )}
        </div>

        <div className="glass-card p-5">
          <h3 className="mb-4 font-semibold text-white">Quick Actions</h3>
          <div className="space-y-3">
            {[
              {
                label: 'Submit New Expense',
                description: 'Create and submit expense',
                path: '/expenses/new',
                icon: PlusCircle,
                color: 'from-indigo-500 to-violet-600',
              },
              {
                label: 'View All Expenses',
                description: 'Browse your expense history',
                path: '/expenses',
                icon: Receipt,
                color: 'from-slate-600 to-slate-700',
              },
              {
                label: 'Reports & Analytics',
                description: 'Your scoped reporting view',
                path: '/reports',
                icon: BarChart2,
                color: 'from-emerald-600 to-teal-700',
              },
            ].map((action) => {
              const Icon = action.icon;
              return (
                <button
                  key={action.path}
                  onClick={() => navigate(action.path)}
                  className="group flex w-full items-center gap-3 rounded-xl border border-slate-700 p-3.5 transition-all hover:border-indigo-500/40 hover:bg-slate-800/50"
                >
                  <div className={`flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-lg bg-gradient-to-br ${action.color}`}>
                    <Icon size={16} className="text-white" />
                  </div>
                  <div className="flex-1 text-left">
                    <p className="text-sm font-medium text-white">{action.label}</p>
                    <p className="text-xs text-slate-500">{action.description}</p>
                  </div>
                  <ArrowRight size={16} className="text-slate-600 transition-colors group-hover:text-indigo-400" />
                </button>
              );
            })}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <div className="glass-card p-5">
          <div className="mb-4">
            <h3 className="font-semibold text-white">Recent Status Mix</h3>
            <p className="mt-0.5 text-xs text-slate-500">Your latest 6 expenses</p>
          </div>
          {recentLoading ? (
            <div className="skeleton h-60 w-full" />
          ) : statusDistribution.length > 0 ? (
            <>
              <StatusDonutChart data={statusDistribution} total={recentExpenses.length} height={240} />
              <div className="mt-3 flex flex-wrap gap-2">
                {statusDistribution.map((item) => (
                  <div key={item.label} className="rounded-full border border-slate-700 bg-slate-800/60 px-3 py-1 text-xs text-slate-300">
                    {item.label}: {item.value}
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="flex h-60 items-center justify-center text-slate-600">No recent expense data available</div>
          )}
        </div>

        <div className="glass-card p-5 lg:col-span-2">
          <div className="mb-4">
            <h3 className="font-semibold text-white">Weekly Spend Candles</h3>
            <p className="mt-0.5 text-xs text-slate-500">Your own weekly expense activity</p>
          </div>
          {expensesLoading ? (
            <div className="skeleton h-60 w-full" />
          ) : weeklyBuckets.length > 0 ? (
            <div className="space-y-4">
              <SpendCandlestickChart
                data={weeklyBuckets.map((bucket) => bucket.candle)}
                activeLabel={selectedWeek?.label}
                onSelect={(item) => setSelectedWeekLabel(item.label)}
                height={260}
              />
              {selectedWeek && (
                <div className="rounded-2xl border border-slate-700 bg-slate-900/40 p-4">
                  <div className="mb-3 flex items-start justify-between gap-4">
                    <div>
                      <p className="flex items-center gap-2 text-sm font-semibold text-white">
                        <CalendarDays size={15} className="text-indigo-400" />
                        {selectedWeek.label}
                      </p>
                      <p className="mt-1 text-xs text-slate-500">
                        {format(selectedWeek.start, 'MMM d')} - {format(selectedWeek.end, 'MMM d, yyyy')}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-lg font-bold text-white">{formatAmount(selectedWeek.total)}</p>
                      <p className="text-xs text-slate-500">{selectedWeek.expenses.length} expense(s)</p>
                    </div>
                  </div>
                  <div className="space-y-2">
                    {selectedWeek.expenses
                      .sort((a, b) => new Date(b.expenseDate).getTime() - new Date(a.expenseDate).getTime())
                      .map((expense) => (
                        <button
                          key={expense.id}
                          onClick={() => navigate(`/expenses/${expense.id}`)}
                          className="flex w-full items-center justify-between rounded-xl border border-slate-800 bg-slate-950/40 px-3 py-2 text-left transition-colors hover:border-indigo-500/30 hover:bg-slate-900"
                        >
                          <div className="min-w-0">
                            <p className="truncate text-sm font-medium text-white">{expense.title}</p>
                            <p className="truncate text-xs text-slate-500">
                              {expense.merchantName} · {format(new Date(expense.expenseDate), 'EEE, MMM d')}
                            </p>
                          </div>
                          <div className="ml-4 flex items-center gap-3">
                            <StatusBadge status={expense.status as ExpenseStatus} size="sm" />
                            <span className="text-sm font-semibold text-white">
                              {formatAmount(expense.amount, expense.currency)}
                            </span>
                          </div>
                        </button>
                      ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="flex h-60 items-center justify-center text-slate-600">No weekly expense history available</div>
          )}
        </div>
      </div>

      <div className="glass-card overflow-hidden">
        <div className="flex items-center justify-between border-b border-slate-800 p-5">
          <h3 className="font-semibold text-white">Recent Expenses</h3>
          <button
            onClick={() => navigate('/expenses')}
            className="flex items-center gap-1.5 text-sm text-indigo-400 transition-colors hover:text-indigo-300"
          >
            View all <ArrowRight size={14} />
          </button>
        </div>
        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Expense</th>
                <th>Merchant</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Risk</th>
                <th>Date</th>
              </tr>
            </thead>
            <tbody>
              {recentLoading ? (
                Array.from({ length: 5 }).map((_, rowIndex) => (
                  <tr key={rowIndex}>
                    {Array.from({ length: 6 }).map((_, colIndex) => (
                      <td key={colIndex}><div className="skeleton h-4 w-full" /></td>
                    ))}
                  </tr>
                ))
              ) : recentExpenses.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-10 text-center text-slate-500">
                    No expenses yet.{' '}
                    <button onClick={() => navigate('/expenses/new')} className="text-indigo-400 hover:underline">
                      Create your first one
                    </button>
                  </td>
                </tr>
              ) : (
                recentExpenses.map((expense) => (
                  <tr key={expense.id} className="cursor-pointer" onClick={() => navigate(`/expenses/${expense.id}`)}>
                    <td><p className="max-w-[180px] truncate text-sm font-medium text-white">{expense.title}</p></td>
                    <td className="text-sm text-slate-400">{expense.merchantName}</td>
                    <td className="font-semibold text-white">{formatAmount(expense.amount)}</td>
                    <td><StatusBadge status={expense.status as ExpenseStatus} size="sm" /></td>
                    <td><RiskBadge riskLevel={expense.riskLevel as RiskLevel} size="sm" /></td>
                    <td className="text-xs text-slate-500">{format(new Date(expense.createdAt), 'MMM d, yyyy')}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
