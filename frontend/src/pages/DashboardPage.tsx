import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import {
  Receipt,
  Clock,
  DollarSign,
  AlertTriangle,
  PlusCircle,
  BarChart2,
  ArrowRight,
  TrendingUp,
} from 'lucide-react';
import { format } from 'date-fns';
import { RootState } from '../store';
import { useGetDashboardStatsQuery, useGetMonthlySpendQuery, useGetRecentExpensesQuery } from '../store/api/analyticsApi';
import StatsCard from '../components/ui/StatsCard';
import StatusBadge from '../components/ui/StatusBadge';
import RiskBadge from '../components/ui/RiskBadge';
import MonthlySpendChart from '../components/charts/MonthlySpendChart';
import { StatsCardSkeleton } from '../components/ui/LoadingSkeleton';
import { ExpenseStatus, RiskLevel } from '../types';

const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useSelector((state: RootState) => state.auth);
  const currentYear = new Date().getFullYear();

  const { data: statsData, isLoading: statsLoading } = useGetDashboardStatsQuery();
  const { data: monthlyData, isLoading: monthlyLoading } = useGetMonthlySpendQuery({ year: currentYear });
  const { data: recentData, isLoading: recentLoading } = useGetRecentExpensesQuery({ limit: 6 });

  const stats = statsData?.data;
  const monthlySpend = monthlyData?.data ?? [];
  const recentExpenses = recentData?.data ?? [];

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-bold text-white">
            {getGreeting()}, <span className="gradient-text">{user?.username || 'User'}</span> 👋
          </h2>
          <p className="text-slate-400 text-sm mt-1">
            {format(new Date(), 'EEEE, MMMM d, yyyy')} • Here's your expense overview
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

      {/* Stats Cards */}
      {statsLoading ? (
        <StatsCardSkeleton />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatsCard
            title="Total Expenses"
            value={stats?.totalExpenses ?? 0}
            subtitle="All time submissions"
            icon={Receipt}
            trend={{ value: 12, label: 'vs last month' }}
            colorClass="card-indigo"
            gradient="from-indigo-500 to-violet-600"
          />
          <StatsCard
            title="Pending Approval"
            value={stats?.pendingCount ?? 0}
            subtitle="Awaiting review"
            icon={Clock}
            colorClass="card-amber"
            gradient="from-amber-500 to-orange-600"
          />
          <StatsCard
            title="This Month"
            value={`$${((stats?.thisMonthAmount ?? 0) / 1000).toFixed(1)}k`}
            subtitle="Total spend"
            icon={DollarSign}
            trend={{ value: -5, label: 'vs last month' }}
            colorClass="card-emerald"
            gradient="from-emerald-500 to-teal-600"
          />
          <StatsCard
            title="High Risk Alerts"
            value={stats?.highRiskAlerts ?? 0}
            subtitle="Require attention"
            icon={AlertTriangle}
            colorClass="card-rose"
            gradient="from-rose-500 to-pink-600"
          />
        </div>
      )}

      {/* Charts + Recent Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Monthly Spend Chart */}
        <div className="lg:col-span-2 glass-card p-5">
          <div className="flex items-center justify-between mb-5">
            <div>
              <h3 className="font-semibold text-white">Monthly Spending</h3>
              <p className="text-xs text-slate-500 mt-0.5">{currentYear} • Total by month</p>
            </div>
            <div className="flex items-center gap-2 text-xs text-emerald-400 font-medium">
              <TrendingUp size={14} />
              <span>Year to date</span>
            </div>
          </div>
          {monthlyLoading ? (
            <div className="skeleton h-48 w-full" />
          ) : monthlySpend.length > 0 ? (
            <MonthlySpendChart data={monthlySpend} height={220} />
          ) : (
            <div className="h-48 flex items-center justify-center text-slate-600">
              <p>No spending data available</p>
            </div>
          )}
        </div>

        {/* Quick Actions */}
        <div className="glass-card p-5">
          <h3 className="font-semibold text-white mb-4">Quick Actions</h3>
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
                description: 'Browse expense history',
                path: '/expenses',
                icon: Receipt,
                color: 'from-slate-600 to-slate-700',
              },
              {
                label: 'Analytics & Reports',
                description: 'View spending reports',
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
                  className="w-full flex items-center gap-3 p-3.5 rounded-xl border border-slate-700 hover:border-indigo-500/40 hover:bg-slate-800/50 transition-all group"
                >
                  <div className={`w-9 h-9 rounded-lg bg-gradient-to-br ${action.color} flex items-center justify-center flex-shrink-0`}>
                    <Icon size={16} className="text-white" />
                  </div>
                  <div className="flex-1 text-left">
                    <p className="text-sm font-medium text-white">{action.label}</p>
                    <p className="text-xs text-slate-500">{action.description}</p>
                  </div>
                  <ArrowRight size={16} className="text-slate-600 group-hover:text-indigo-400 transition-colors" />
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Recent Expenses */}
      <div className="glass-card overflow-hidden">
        <div className="flex items-center justify-between p-5 border-b border-slate-800">
          <h3 className="font-semibold text-white">Recent Expenses</h3>
          <button
            onClick={() => navigate('/expenses')}
            className="flex items-center gap-1.5 text-sm text-indigo-400 hover:text-indigo-300 transition-colors"
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
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 6 }).map((_, j) => (
                      <td key={j}><div className="skeleton h-4 w-full" /></td>
                    ))}
                  </tr>
                ))
              ) : recentExpenses.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-10 text-slate-500">
                    No expenses yet. <button onClick={() => navigate('/expenses/new')} className="text-indigo-400 hover:underline">Create your first one</button>
                  </td>
                </tr>
              ) : (
                recentExpenses.map((exp) => (
                  <tr
                    key={exp.id}
                    className="cursor-pointer"
                    onClick={() => navigate(`/expenses/${exp.id}`)}
                  >
                    <td>
                      <p className="font-medium text-white text-sm truncate max-w-[160px]">{exp.title}</p>
                    </td>
                    <td className="text-slate-400 text-sm">{exp.merchantName}</td>
                    <td>
                      <span className="font-semibold text-white">${exp.amount.toLocaleString()}</span>
                    </td>
                    <td>
                      <StatusBadge status={exp.status as ExpenseStatus} size="sm" />
                    </td>
                    <td>
                      <RiskBadge riskLevel={exp.riskLevel as RiskLevel} size="sm" />
                    </td>
                    <td className="text-slate-500 text-xs">
                      {format(new Date(exp.createdAt), 'MMM d, yyyy')}
                    </td>
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
