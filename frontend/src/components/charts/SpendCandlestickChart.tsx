import React from 'react';
import { CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis, ComposedChart, Bar, Cell } from 'recharts';

interface SpendCandlestickDatum {
  label: string;
  open: number;
  close: number;
  high: number;
  low: number;
}

interface SpendCandlestickChartProps {
  data: SpendCandlestickDatum[];
  height?: number;
  activeLabel?: string;
  onSelect?: (item: SpendCandlestickDatum) => void;
}

const CandleShape = (props: {
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  payload?: SpendCandlestickDatum;
  yAxis?: { scale: (value: number) => number };
}) => {
  const { x = 0, width = 0, payload, yAxis } = props;
  if (!payload || !yAxis?.scale) return null;

  const scale = yAxis.scale;
  const wickX = x + width / 2;
  const bodyWidth = Math.max(width * 0.58, 10);
  const bodyX = wickX - bodyWidth / 2;
  const openY = scale(payload.open);
  const closeY = scale(payload.close);
  const highY = scale(payload.high);
  const lowY = scale(payload.low);
  const bodyTop = Math.min(openY, closeY);
  const bodyHeight = Math.max(Math.abs(openY - closeY), 4);
  const rising = payload.close >= payload.open;
  const bodyFill = rising ? '#10B981' : '#F43F5E';

  return (
    <g>
      <line x1={wickX} x2={wickX} y1={highY} y2={lowY} stroke={bodyFill} strokeWidth={2} strokeLinecap="round" />
      <rect
        x={bodyX}
        y={bodyTop}
        width={bodyWidth}
        height={bodyHeight}
        rx={3}
        fill={bodyFill}
        fillOpacity={0.24}
        stroke={bodyFill}
        strokeWidth={2}
      />
    </g>
  );
};

const CustomTooltip = ({
  active,
  payload,
}: {
  active?: boolean;
  payload?: { payload: SpendCandlestickDatum }[];
}) => {
  if (!active || !payload?.length) return null;

  const item = payload[0].payload;

  return (
      <div className="glass-card p-3 text-sm">
      <p className="mb-1 font-semibold text-white">{item.label}</p>
      <p className="text-slate-400">Open: <span className="text-white">${item.open.toLocaleString()}</span></p>
      <p className="text-slate-400">Close: <span className="text-white">${item.close.toLocaleString()}</span></p>
      <p className="text-slate-400">Range: <span className="text-white">${item.low.toLocaleString()} - ${item.high.toLocaleString()}</span></p>
    </div>
  );
};

const SpendCandlestickChart: React.FC<SpendCandlestickChartProps> = ({
  data,
  height = 260,
  activeLabel,
  onSelect,
}) => {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <ComposedChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(51,65,85,0.4)" />
        <XAxis
          dataKey="label"
          tick={{ fill: '#64748B', fontSize: 12 }}
          axisLine={{ stroke: '#334155' }}
          tickLine={false}
        />
        <YAxis
          tick={{ fill: '#64748B', fontSize: 12 }}
          axisLine={{ stroke: '#334155' }}
          tickLine={false}
          tickFormatter={(value) => `$${(value / 1000).toFixed(0)}k`}
        />
        <Tooltip content={<CustomTooltip />} />
        <Bar dataKey="close" shape={<CandleShape />} isAnimationActive={false} onClick={onSelect} cursor="pointer">
          {data.map((entry) => (
            <Cell
              key={entry.label}
              fill={entry.label === activeLabel ? 'rgba(99,102,241,0.18)' : 'rgba(0,0,0,0)'}
              stroke={entry.label === activeLabel ? '#818CF8' : 'transparent'}
              radius={8}
            />
          ))}
        </Bar>
      </ComposedChart>
    </ResponsiveContainer>
  );
};

export default SpendCandlestickChart;
