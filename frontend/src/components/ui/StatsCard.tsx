import React from 'react';
import { LucideIcon } from 'lucide-react';
import { TrendingUp, TrendingDown } from 'lucide-react';

interface StatsCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  trend?: { value: number; label: string };
  colorClass?: string;
  gradient?: string;
}

const StatsCard: React.FC<StatsCardProps> = ({
  title,
  value,
  subtitle,
  icon: Icon,
  trend,
  colorClass = 'card-indigo',
  gradient = 'from-indigo-500 to-violet-600',
}) => {
  const isPositiveTrend = trend && trend.value >= 0;

  return (
    <div className={`glass-card p-5 ${colorClass} hover:scale-[1.02] transition-transform duration-200`}>
      <div className="flex items-start justify-between mb-4">
        <p className="text-sm font-medium text-slate-400">{title}</p>
        <div className={`p-2.5 rounded-xl bg-gradient-to-br ${gradient} bg-opacity-20`}>
          <Icon size={18} className="text-white" />
        </div>
      </div>
      <div className="space-y-1">
        <p className="text-2xl font-bold text-white tracking-tight">{value}</p>
        {subtitle && (
          <p className="text-xs text-slate-500">{subtitle}</p>
        )}
        {trend && (
          <div className={`flex items-center gap-1 text-xs font-medium ${isPositiveTrend ? 'text-emerald-400' : 'text-rose-400'}`}>
            {isPositiveTrend ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
            <span>{Math.abs(trend.value)}% {trend.label}</span>
          </div>
        )}
      </div>
    </div>
  );
};

export default StatsCard;
