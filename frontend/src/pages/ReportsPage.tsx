import React, { useState } from 'react';
import { format, subMonths, startOfMonth, endOfMonth } from 'date-fns';
import { Download, BarChart2, PieChart, TrendingUp, Calendar, RefreshCw } from 'lucide-react';
import {
  useGetMonthlySpendQuery,
  useGetCategorySpendQuery,
  useGetDepartmentSpendQuery,
} from '../store/api/analyticsApi';
import MonthlySpendChart from '../components/charts/MonthlySpendChart';
import CategoryPieChart from '../components/charts/CategoryPieChart';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

const ReportsPage: React.FC = () => {
  const currentYear = new Date().getFullYear();
  const [startDate, setStartDate] = useState(format(startOfMonth(subMonths(new Date(), 6)), 'yyyy-MM-dd'));
  const [endDate, setEndDate] = useState(format(endOfMonth(new Date()), 'yyyy-MM-dd'));
  const [year, setYear] = useState(currentYear);

  const { data: monthlyData, isLoading: monthlyLoading, refetch: refetchMonthly } = useGetMonthlySpendQuery({ year });
  const { data: categoryData, isLoading: categoryLoading } = useGetCategorySpendQuery({ startDate, endDate });
  const { data: departmentData, isLoading: deptLoading } = useGetDepartmentSpendQuery({ startDate, endDate });

  const monthlySpend = monthlyData?.data ?? [];
  const categorySpend = categoryData?.data ?? [];
  const departmentSpend = departmentData?.data ?? [];

  const totalSpend = monthlySpend.reduce((s, m) => s + m.totalAmount, 0);
  const totalExpenses = monthlySpend.reduce((s, m) => s + m.expenseCount, 0);
  const avgPerExpense = totalExpenses > 0 ? totalSpend / totalExpenses : 0;

  const handleExportCSV = () => {
    const headers = ['Month', 'Year', 'Total Amount', 'Expense Count'];
    const rows = monthlySpend.map((m) => [
      new Date(m.year, m.month - 1).toLocaleString('default', { month: 'long' }),
      m.year,
      m.totalAmount.toFixed(2),
      m.expenseCount,
    ]);
    const csv = [headers, ...rows].map((r) => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `expense-report-${year}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-xl font-bold text-white">Reports & Analytics</h2>
          <p className="text-sm text-slate-500 mt-0.5">Spending insights and trends</p>
        </div>
        <button onClick={handleExportCSV} className="btn-secondary text-sm">
          <Download size={15} />
          Export CSV
        </button>
      </div>

      {/* Date range controls */}
      <div className="glass-card p-4">
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2">
            <Calendar size={15} className="text-slate-500" />
            <span className="text-sm text-slate-400">Date Range:</span>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="form-input text-sm py-1.5"
            />
            <span className="text-slate-600">to</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="form-input text-sm py-1.5"
            />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-sm text-slate-400">Year:</span>
            <select
              value={year}
              onChange={(e) => setYear(parseInt(e.target.value))}
              className="form-input text-sm py-1.5 w-28"
            >
              {[currentYear - 2, currentYear - 1, currentYear].map((y) => (
                <option key={y} value={y}>{y}</option>
              ))}
            </select>
          </div>
          <button onClick={() => refetchMonthly()} className="p-2 rounded-lg text-slate-500 hover:text-white hover:bg-slate-800 transition-colors">
            <RefreshCw size={15} />
          </button>
        </div>
      </div>

      {/* Summary KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {[
          { label: 'Total Spend', value: `$${totalSpend.toLocaleString(undefined, { maximumFractionDigits: 0 })}`, icon: TrendingUp, color: 'from-indigo-500 to-violet-600', border: 'card-indigo' },
          { label: 'Total Expenses', value: totalExpenses.toLocaleString(), icon: BarChart2, color: 'from-emerald-500 to-teal-600', border: 'card-emerald' },
          { label: 'Avg Per Expense', value: `$${avgPerExpense.toLocaleString(undefined, { maximumFractionDigits: 0 })}`, icon: PieChart, color: 'from-amber-500 to-orange-600', border: 'card-amber' },
        ].map(({ label, value, icon: Icon, color, border }) => (
          <div key={label} className={`glass-card p-5 ${border}`}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-slate-500 mb-1">{label}</p>
                <p className="text-2xl font-bold text-white">{value}</p>
              </div>
              <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${color} flex items-center justify-center`}>
                <Icon size={18} className="text-white" />
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Monthly spend chart */}
      <div className="glass-card p-5">
        <div className="flex items-center justify-between mb-5">
          <div>
            <h3 className="font-semibold text-white">Monthly Spending — {year}</h3>
            <p className="text-xs text-slate-500 mt-0.5">Total expenditure by month</p>
          </div>
        </div>
        {monthlyLoading ? (
          <div className="skeleton h-56 w-full" />
        ) : monthlySpend.length > 0 ? (
          <MonthlySpendChart data={monthlySpend} height={260} />
        ) : (
          <div className="h-56 flex items-center justify-center text-slate-600">No data for {year}</div>
        )}
      </div>

      {/* Category + Department charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Category Breakdown */}
        <div className="glass-card p-5">
          <h3 className="font-semibold text-white mb-1">Category Breakdown</h3>
          <p className="text-xs text-slate-500 mb-4">Spend by expense category</p>
          {categoryLoading ? (
            <div className="skeleton h-52 w-full" />
          ) : categorySpend.length > 0 ? (
            <>
              <CategoryPieChart data={categorySpend} height={220} />
              <div className="mt-4 space-y-2">
                {categorySpend.slice(0, 5).map((cat, i) => (
                  <div key={i} className="flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2">
                      <span className="text-slate-400">{cat.categoryName}</span>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="text-xs text-slate-600">{cat.expenseCount} expenses</span>
                      <span className="font-semibold text-white">${cat.totalAmount.toLocaleString(undefined, { maximumFractionDigits: 0 })}</span>
                      <span className="text-xs text-slate-500 w-10 text-right">{cat.percentage.toFixed(1)}%</span>
                    </div>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="h-40 flex items-center justify-center text-slate-600">No category data</div>
          )}
        </div>

        {/* Department Spend */}
        <div className="glass-card p-5">
          <h3 className="font-semibold text-white mb-1">Department Spend</h3>
          <p className="text-xs text-slate-500 mb-4">Budget utilization by department</p>
          {deptLoading ? (
            <div className="skeleton h-52 w-full" />
          ) : departmentSpend.length > 0 ? (
            <ResponsiveContainer width="100%" height={220}>
              <BarChart
                data={departmentSpend.map((d) => ({ name: d.department, amount: d.totalAmount, avg: d.averageAmount }))}
                margin={{ top: 5, right: 10, left: 0, bottom: 30 }}
                barSize={20}
              >
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(51,65,85,0.4)" />
                <XAxis
                  dataKey="name"
                  tick={{ fill: '#64748B', fontSize: 11 }}
                  axisLine={{ stroke: '#334155' }}
                  tickLine={false}
                  angle={-30}
                  textAnchor="end"
                />
                <YAxis
                  tick={{ fill: '#64748B', fontSize: 11 }}
                  axisLine={{ stroke: '#334155' }}
                  tickLine={false}
                  tickFormatter={(v) => `$${(v / 1000).toFixed(0)}k`}
                />
                <Tooltip
                  contentStyle={{ background: '#1E293B', border: '1px solid #334155', borderRadius: '8px', fontSize: '12px' }}
                  formatter={(val: number) => [`$${val.toLocaleString()}`, 'Total']}
                />
                <Bar dataKey="amount" fill="#6366F1" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-40 flex items-center justify-center text-slate-600">No department data</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ReportsPage;
