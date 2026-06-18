import React from 'react';
import { RiskLevel } from '../../types';
import { AlertTriangle, CheckCircle, AlertCircle, LucideIcon } from 'lucide-react';

interface RiskBadgeProps {
  riskLevel: RiskLevel;
  score?: number;
  size?: 'sm' | 'md';
}

const riskConfig: Record<RiskLevel, { label: string; className: string; Icon: LucideIcon }> = {
  LOW: {
    label: 'Low Risk',
    className: 'bg-emerald-950 text-emerald-300 border border-emerald-800',
    Icon: CheckCircle,
  },
  MEDIUM: {
    label: 'Medium Risk',
    className: 'bg-amber-950 text-amber-300 border border-amber-800',
    Icon: AlertCircle,
  },
  HIGH: {
    label: 'High Risk',
    className: 'bg-rose-950 text-rose-300 border border-rose-800 animate-pulse',
    Icon: AlertTriangle,
  },
};

const RiskBadge: React.FC<RiskBadgeProps> = ({ riskLevel, score, size = 'md' }) => {
  const config = riskConfig[riskLevel] || riskConfig.LOW;
  const { Icon } = config;
  const sizeClass = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-3 py-1 text-xs';

  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full font-semibold ${sizeClass} ${config.className}`}>
      <Icon size={12} />
      {config.label}
      {score !== undefined && (
        <span className="ml-0.5 opacity-75">({score}%)</span>
      )}
    </span>
  );
};

export default RiskBadge;
