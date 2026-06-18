import React from 'react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { MonthlySpend } from '../../types';

interface MonthlySpendChartProps {
  data: MonthlySpend[];
  height?: number;
}

const MONTH_NAMES = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

const CustomTooltip = ({ active, payload, label }: { active?: boolean; payload?: { value: number }[]; label?: string }) => {
  if (active && payload && payload.length) {
    return (
      <div className="glass-card p-3 text-sm">
        <p className="text-slate-400 mb-1">{label}</p>
        <p className="text-indigo-400 font-semibold">${payload[0].value.toLocaleString()}</p>
      </div>
    );
  }
  return null;
};

const MonthlySpendChart: React.FC<MonthlySpendChartProps> = ({ data, height = 240 }) => {
  const chartData = data.map((item) => ({
    name: MONTH_NAMES[item.month - 1] || String(item.month),
    amount: item.totalAmount,
    count: item.expenseCount,
  }));

  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={chartData} margin={{ top: 5, right: 10, left: 0, bottom: 5 }}>
        <defs>
          <linearGradient id="colorAmount" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#6366F1" stopOpacity={0.4} />
            <stop offset="95%" stopColor="#6366F1" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(51,65,85,0.4)" />
        <XAxis
          dataKey="name"
          tick={{ fill: '#64748B', fontSize: 12 }}
          axisLine={{ stroke: '#334155' }}
          tickLine={false}
        />
        <YAxis
          tick={{ fill: '#64748B', fontSize: 12 }}
          axisLine={{ stroke: '#334155' }}
          tickLine={false}
          tickFormatter={(v) => `$${(v / 1000).toFixed(0)}k`}
        />
        <Tooltip content={<CustomTooltip />} />
        <Area
          type="monotone"
          dataKey="amount"
          stroke="#6366F1"
          strokeWidth={2.5}
          fill="url(#colorAmount)"
          dot={{ fill: '#6366F1', strokeWidth: 2, r: 4 }}
          activeDot={{ r: 6, fill: '#8B5CF6' }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
};

export default MonthlySpendChart;
