import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shield, AlertTriangle, TrendingUp, Copy, Eye, RefreshCw } from 'lucide-react';
import {
  useGetFraudSummaryQuery,
  useGetHighRiskExpensesQuery,
  useGetFraudTrendQuery,
  useTriggerFraudAnalysisMutation,
} from '../store/api/fraudApi';
import StatsCard from '../components/ui/StatsCard';
import RiskBadge from '../components/ui/RiskBadge';
import LoadingSkeleton, { StatsCardSkeleton } from '../components/ui/LoadingSkeleton';
import FraudTrendChart from '../components/charts/FraudTrendChart';
import CategoryPieChart from '../components/charts/CategoryPieChart';
import { RiskLevel } from '../types';

const FraudDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  const { data: summaryData, isLoading: summaryLoading } = useGetFraudSummaryQuery();
  const { data: highRiskData, isLoading: highRiskLoading } = useGetHighRiskExpensesQuery({ page, size: 10 });
  const { data: trendData, isLoading: trendLoading } = useGetFraudTrendQuery({ months: 6 });
  const [triggerAnalysis] = useTriggerFraudAnalysisMutation();

  const summary = summaryData?.data;
  const highRiskExpenses = highRiskData?.data?.content ?? [];
  const totalPages = highRiskData?.data?.totalPages ?? 1;
  const trendChartData = trendData?.data ?? [];

  // Build distribution data for pie chart
  const distributionData = summary
    ? [
        { categoryName: 'High Risk', totalAmount: summary.highRiskCount, percentage: summary.totalAnalyzed ? (summary.highRiskCount / summary.totalAnalyzed) * 100 : 0, expenseCount: summary.highRiskCount },
        { categoryName: 'Medium Risk', totalAmount: summary.mediumRiskCount, percentage: summary.totalAnalyzed ? (summary.mediumRiskCount / summary.totalAnalyzed) * 100 : 0, expenseCount: summary.mediumRiskCount },
        { categoryName: 'Low Risk', totalAmount: summary.lowRiskCount, percentage: summary.totalAnalyzed ? (summary.lowRiskCount / summary.totalAnalyzed) * 100 : 0, expenseCount: summary.lowRiskCount },
      ].filter((d) => d.totalAmount > 0)
    : [];

  const handleTriggerAnalysis = async (expenseId: number) => {
    await triggerAnalysis(expenseId);
  };

  return (
    <div className="space-y-5">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-white">Fraud Intelligence Dashboard</h2>
        <p className="text-sm text-slate-500 mt-0.5">AI-powered fraud detection and risk analysis</p>
      </div>

      {/* Summary Stats */}
      {summaryLoading ? (
        <StatsCardSkeleton />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatsCard
            title="Total Analyzed"
            value={summary?.totalAnalyzed ?? 0}
            subtitle="Expenses processed"
            icon={Shield}
            colorClass="card-indigo"
            gradient="from-indigo-500 to-violet-600"
          />
          <StatsCard
            title="High Risk"
            value={summary?.highRiskCount ?? 0}
            subtitle="Require immediate review"
            icon={AlertTriangle}
            colorClass="card-rose"
            gradient="from-rose-500 to-pink-600"
          />
          <StatsCard
            title="Medium Risk"
            value={summary?.mediumRiskCount ?? 0}
            subtitle="Monitor closely"
            icon={TrendingUp}
            colorClass="card-amber"
            gradient="from-amber-500 to-orange-600"
          />
          <StatsCard
            title="Duplicates"
            value={summary?.duplicatesDetected ?? 0}
            subtitle="Potential double submissions"
            icon={Copy}
            colorClass="card-violet"
            gradient="from-violet-500 to-purple-600"
          />
        </div>
      )}

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* Trend Chart */}
        <div className="lg:col-span-2 glass-card p-5">
          <div className="flex items-center justify-between mb-5">
            <div>
              <h3 className="font-semibold text-white">Risk Trend (6 months)</h3>
              <p className="text-xs text-slate-500 mt-0.5">Expense risk distribution over time</p>
            </div>
          </div>
          {trendLoading ? (
            <div className="skeleton h-48 w-full" />
          ) : trendChartData.length > 0 ? (
            <FraudTrendChart data={trendChartData} height={220} />
          ) : (
            <div className="h-48 flex items-center justify-center text-slate-600">
              <p>No trend data available</p>
            </div>
          )}
        </div>

        {/* Risk Distribution */}
        <div className="glass-card p-5">
          <h3 className="font-semibold text-white mb-1">Risk Distribution</h3>
          <p className="text-xs text-slate-500 mb-3">Current portfolio breakdown</p>
          {summaryLoading ? (
            <div className="skeleton h-52 w-full" />
          ) : distributionData.length > 0 ? (
            <>
              <CategoryPieChart data={distributionData} height={200} />
              <div className="mt-3 space-y-2">
                {[
                  { label: 'High Risk', count: summary?.highRiskCount ?? 0, color: 'bg-rose-500' },
                  { label: 'Medium Risk', count: summary?.mediumRiskCount ?? 0, color: 'bg-amber-500' },
                  { label: 'Low Risk', count: summary?.lowRiskCount ?? 0, color: 'bg-emerald-500' },
                ].map(({ label, count, color }) => (
                  <div key={label} className="flex items-center justify-between text-sm">
                    <div className="flex items-center gap-2">
                      <span className={`w-2.5 h-2.5 rounded-full ${color}`} />
                      <span className="text-slate-400">{label}</span>
                    </div>
                    <span className="text-white font-medium">{count}</span>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="h-40 flex items-center justify-center text-slate-600">No data</div>
          )}
        </div>
      </div>

      {/* High Risk Expenses Table */}
      <div className="glass-card overflow-hidden">
        <div className="flex items-center justify-between p-5 border-b border-slate-800">
          <div>
            <h3 className="font-semibold text-white">High Risk Expenses</h3>
            <p className="text-xs text-slate-500 mt-0.5">Expenses with elevated fraud risk scores</p>
          </div>
        </div>
        {highRiskLoading ? (
          <LoadingSkeleton rows={5} />
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Expense</th>
                    <th>Amount</th>
                    <th>Risk Score</th>
                    <th>Risk Level</th>
                    <th>Fraud Flags</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {highRiskExpenses.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="text-center py-12">
                        <div className="flex flex-col items-center gap-3">
                          <div className="w-12 h-12 rounded-xl bg-emerald-950 border border-emerald-800 flex items-center justify-center">
                            <Shield size={20} className="text-emerald-400" />
                          </div>
                          <p className="text-slate-400 font-medium">No high-risk expenses found</p>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    highRiskExpenses.map((expense) => (
                      <tr key={expense.id}>
                        <td>
                          <p className="text-sm font-medium text-white truncate max-w-[180px]">{expense.title}</p>
                          <p className="text-xs text-slate-500">{expense.merchantName}</p>
                        </td>
                        <td>
                          <span className="font-bold text-white">${expense.amount.toLocaleString()}</span>
                        </td>
                        <td>
                          <div className="flex items-center gap-2">
                            <div className="flex-1 bg-slate-800 rounded-full h-1.5 max-w-[80px]">
                              <div
                                className={`h-1.5 rounded-full ${
                                  expense.riskScore >= 70 ? 'bg-rose-500' : expense.riskScore >= 40 ? 'bg-amber-500' : 'bg-emerald-500'
                                }`}
                                style={{ width: `${expense.riskScore}%` }}
                              />
                            </div>
                            <span className={`text-sm font-bold ${
                              expense.riskScore >= 70 ? 'text-rose-400' : expense.riskScore >= 40 ? 'text-amber-400' : 'text-emerald-400'
                            }`}>
                              {expense.riskScore}
                            </span>
                          </div>
                        </td>
                        <td>
                          <RiskBadge riskLevel={expense.riskLevel as RiskLevel} size="sm" />
                        </td>
                        <td>
                          <div className="flex flex-wrap gap-1 max-w-[200px]">
                            {(expense as unknown as { fraudFlags?: { flagType: string }[] }).fraudFlags?.slice(0, 2).map((flag, i) => (
                              <span key={i} className="px-1.5 py-0.5 bg-rose-950 text-rose-300 border border-rose-800 rounded text-xs">
                                {flag.flagType.replace(/_/g, ' ')}
                              </span>
                            )) ?? <span className="text-xs text-slate-600">—</span>}
                          </div>
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
                              onClick={() => handleTriggerAnalysis(expense.id)}
                              className="p-1.5 rounded-lg text-slate-500 hover:text-violet-400 hover:bg-violet-950 transition-colors"
                              title="Re-analyze"
                            >
                              <RefreshCw size={15} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-between px-4 py-3 border-t border-slate-800">
                <p className="text-sm text-slate-500">Page {page + 1} of {totalPages}</p>
                <div className="flex gap-2">
                  <button onClick={() => setPage(page - 1)} disabled={page === 0} className="px-3 py-1.5 text-sm rounded-lg text-slate-400 hover:bg-slate-800 disabled:opacity-30">Previous</button>
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

export default FraudDashboardPage;
