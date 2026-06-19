import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

interface StatusDonutDatum {
  label: string;
  value: number;
}

interface StatusDonutChartProps {
  data: StatusDonutDatum[];
  total: number;
  height?: number;
}

const COLORS = ['#6366F1', '#10B981', '#F59E0B', '#F43F5E', '#8B5CF6', '#06B6D4'];

const CustomTooltip = ({
  active,
  payload,
}: {
  active?: boolean;
  payload?: { payload: StatusDonutDatum }[];
}) => {
  if (!active || !payload?.length) return null;

  const item = payload[0].payload;

  return (
    <div className="glass-card p-3 text-sm">
      <p className="font-semibold text-white">{item.label}</p>
      <p className="text-indigo-400">{item.value} expense(s)</p>
    </div>
  );
};

const StatusDonutChart: React.FC<StatusDonutChartProps> = ({ data, total, height = 240 }) => {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <PieChart>
        <Pie
          data={data}
          dataKey="value"
          nameKey="label"
          cx="50%"
          cy="50%"
          innerRadius={62}
          outerRadius={92}
          paddingAngle={4}
          stroke="transparent"
        >
          {data.map((_, index) => (
            <Cell key={`status-cell-${index}`} fill={COLORS[index % COLORS.length]} />
          ))}
        </Pie>
        <Tooltip content={<CustomTooltip />} />
        <text x="50%" y="46%" textAnchor="middle" dominantBaseline="middle" className="fill-white text-2xl font-bold">
          {total}
        </text>
        <text x="50%" y="56%" textAnchor="middle" dominantBaseline="middle" className="fill-slate-500 text-xs">
          Recent items
        </text>
      </PieChart>
    </ResponsiveContainer>
  );
};

export default StatusDonutChart;
