import React from 'react';
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { CategorySpend } from '../../types';

interface CategoryPieChartProps {
  data: CategorySpend[];
  height?: number;
}

const COLORS = [
  '#6366F1',
  '#8B5CF6',
  '#EC4899',
  '#F59E0B',
  '#10B981',
  '#06B6D4',
  '#F43F5E',
  '#84CC16',
  '#3B82F6',
  '#A78BFA',
];

const CustomTooltip = ({
  active,
  payload,
}: {
  active?: boolean;
  payload?: { name: string; value: number; payload: CategorySpend }[];
}) => {
  if (active && payload && payload.length) {
    const item = payload[0];
    return (
      <div className="glass-card p-3 text-sm">
        <p className="text-white font-semibold mb-1">{item.name}</p>
        <p className="text-indigo-400">${item.value.toLocaleString()}</p>
        <p className="text-slate-400">{item.payload.percentage.toFixed(1)}% of total</p>
        <p className="text-slate-500">{item.payload.expenseCount} expenses</p>
      </div>
    );
  }
  return null;
};

const CustomLegend = ({ payload }: { payload?: { value: string; color: string }[] }) => {
  if (!payload) return null;
  return (
    <div className="flex flex-wrap gap-2 justify-center mt-2">
      {payload.map((entry, index) => (
        <div key={index} className="flex items-center gap-1.5 text-xs text-slate-400">
          <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: entry.color }} />
          {entry.value}
        </div>
      ))}
    </div>
  );
};

const CategoryPieChart: React.FC<CategoryPieChartProps> = ({ data, height = 280 }) => {
  const chartData = data.map((item) => ({
    name: item.categoryName,
    value: item.totalAmount,
    percentage: item.percentage,
    expenseCount: item.expenseCount,
  }));

  return (
    <ResponsiveContainer width="100%" height={height}>
      <PieChart>
        <Pie
          data={chartData}
          cx="50%"
          cy="45%"
          innerRadius={55}
          outerRadius={90}
          paddingAngle={3}
          dataKey="value"
        >
          {chartData.map((_, index) => (
            <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} stroke="transparent" />
          ))}
        </Pie>
        <Tooltip content={<CustomTooltip />} />
        <Legend content={<CustomLegend />} />
      </PieChart>
    </ResponsiveContainer>
  );
};

export default CategoryPieChart;
