import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Eye, EyeOff, Zap, LogIn, AlertCircle } from 'lucide-react';
import { useLoginMutation } from '../../store/api/authApi';
import { setCredentials } from '../../store/slices/authSlice';

const loginSchema = z.object({
  username: z.string().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
});

type LoginForm = z.infer<typeof loginSchema>;

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [showPassword, setShowPassword] = useState(false);
  const [login, { isLoading, error }] = useLoginMutation();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (data: LoginForm) => {
    try {
      const result = await login(data).unwrap();
      if (result.success && result.data) {
        const { accessToken, refreshToken, userId, username, email, role } = result.data;
        dispatch(
          setCredentials({
            accessToken,
            refreshToken,
            user: { id: userId, username, email, role },
          })
        );
        navigate('/dashboard');
      }
    } catch {
      // error handled via RTK Query's error state
    }
  };

  const getErrorMessage = () => {
    if (!error) return null;
    if ('status' in error) {
      if (error.status === 401) return 'Invalid username or password.';
      if (error.status === 429) return 'Too many attempts. Please try again later.';
      const data = error.data as { message?: string } | undefined;
      return data?.message || 'Login failed. Please try again.';
    }
    return 'Network error. Please check your connection.';
  };

  const errorMessage = getErrorMessage();

  return (
    <div className="min-h-screen flex items-center justify-center px-4" style={{
      background: 'radial-gradient(ellipse at top left, #1E1B4B 0%, #0F172A 40%, #1E293B 100%)',
    }}>
      {/* Background orbs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full opacity-20"
          style={{ background: 'radial-gradient(circle, #6366F1, transparent)' }} />
        <div className="absolute -bottom-32 -right-32 w-96 h-96 rounded-full opacity-10"
          style={{ background: 'radial-gradient(circle, #8B5CF6, transparent)' }} />
      </div>

      <div className="w-full max-w-md relative z-10">
        {/* Card */}
        <div className="glass-card p-8" style={{ border: '1px solid rgba(99,102,241,0.2)' }}>
          {/* Brand */}
          <div className="flex items-center gap-3 mb-8">
            <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-lg">
              <Zap size={22} className="text-white" />
            </div>
            <div>
              <h1 className="text-xl font-bold gradient-text">SEIP</h1>
              <p className="text-xs text-slate-500">Smart Expense Intelligence Platform</p>
            </div>
          </div>

          <div className="mb-6">
            <h2 className="text-2xl font-bold text-white mb-1">Welcome back</h2>
            <p className="text-slate-400 text-sm">Sign in to your account to continue</p>
          </div>

          {/* Error alert */}
          {errorMessage && (
            <div className="mb-5 flex items-center gap-2.5 bg-rose-950 border border-rose-800 text-rose-300 px-4 py-3 rounded-lg text-sm">
              <AlertCircle size={16} className="flex-shrink-0" />
              {errorMessage}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {/* Username */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">
                Username
              </label>
              <input
                {...register('username')}
                type="text"
                placeholder="Enter your username"
                className={`form-input ${errors.username ? 'border-rose-500 focus:border-rose-500' : ''}`}
                autoComplete="username"
                autoFocus
              />
              {errors.username && (
                <p className="mt-1 text-xs text-rose-400">{errors.username.message}</p>
              )}
            </div>

            {/* Password */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">
                Password
              </label>
              <div className="relative">
                <input
                  {...register('password')}
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Enter your password"
                  className={`form-input pr-11 ${errors.password ? 'border-rose-500 focus:border-rose-500' : ''}`}
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition-colors"
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {errors.password && (
                <p className="mt-1 text-xs text-rose-400">{errors.password.message}</p>
              )}
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={isLoading}
              className="btn-primary w-full justify-center py-3 mt-2 text-sm"
            >
              {isLoading ? (
                <span className="flex items-center gap-2">
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Signing in...
                </span>
              ) : (
                <span className="flex items-center gap-2">
                  <LogIn size={16} />
                  Sign in
                </span>
              )}
            </button>
          </form>

          <p className="text-center text-sm text-slate-500 mt-6">
            Don't have an account?{' '}
            <Link to="/register" className="text-indigo-400 hover:text-indigo-300 font-medium transition-colors">
              Create one
            </Link>
          </p>
        </div>

        {/* Demo credentials hint */}
        <div className="mt-4 text-center">
          <p className="text-xs text-slate-600">
            Demo: admin / password123 &nbsp;•&nbsp; manager / password123
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
