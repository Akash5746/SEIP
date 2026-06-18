import React from 'react';

interface LoadingSkeletonProps {
  rows?: number;
  className?: string;
  variant?: 'table' | 'card' | 'text' | 'chart';
}

const LoadingSkeleton: React.FC<LoadingSkeletonProps> = ({
  rows = 5,
  className = '',
  variant = 'table',
}) => {
  if (variant === 'card') {
    return (
      <div className={`glass-card p-6 ${className}`}>
        <div className="skeleton h-4 w-24 mb-4" />
        <div className="skeleton h-8 w-32 mb-2" />
        <div className="skeleton h-3 w-20" />
      </div>
    );
  }

  if (variant === 'chart') {
    return (
      <div className={`glass-card p-6 ${className}`}>
        <div className="skeleton h-4 w-32 mb-6" />
        <div className="skeleton h-48 w-full" />
      </div>
    );
  }

  if (variant === 'text') {
    return (
      <div className={`space-y-3 ${className}`}>
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="skeleton h-4" style={{ width: `${Math.random() * 40 + 60}%` }} />
        ))}
      </div>
    );
  }

  // Table variant
  return (
    <div className={`glass-card overflow-hidden ${className}`}>
      <div className="p-4 border-b border-slate-700">
        <div className="skeleton h-5 w-40" />
      </div>
      <div className="divide-y divide-slate-800">
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="flex items-center gap-4 p-4">
            <div className="skeleton h-4 w-24" />
            <div className="skeleton h-4 flex-1" />
            <div className="skeleton h-4 w-20" />
            <div className="skeleton h-4 w-16" />
            <div className="skeleton h-6 w-20 rounded-full" />
          </div>
        ))}
      </div>
    </div>
  );
};

export const StatsCardSkeleton: React.FC = () => (
  <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
    {Array.from({ length: 4 }).map((_, i) => (
      <div key={i} className="glass-card p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="skeleton h-4 w-28" />
          <div className="skeleton h-10 w-10 rounded-xl" />
        </div>
        <div className="skeleton h-8 w-24 mb-2" />
        <div className="skeleton h-3 w-32" />
      </div>
    ))}
  </div>
);

export default LoadingSkeleton;
