import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Eye, EyeOff, Zap, UserPlus, AlertCircle, CheckCircle2 } from 'lucide-react';
import { useRegisterMutation } from '../../store/api/authApi';
import { setCredentials } from '../../store/slices/authSlice';

const registerSchema = z.object({
  username: z.string().min(3, 'Username must be at least 3 characters').max(50),
  email: z.string().email('Enter a valid email address'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Must contain at least one uppercase letter')
    .regex(/[0-9]/, 'Must contain at least one number'),
  confirmPassword: z.string(),
  role: z.enum(['EMPLOYEE', 'MANAGER']),
  department: z.string().optional(),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});

type RegisterForm = z.infer<typeof registerSchema>;

const DEPARTMENTS = [
  'Engineering', 'Marketing', 'Sales', 'Finance', 'Operations',
  'Human Resources', 'Legal', 'Product', 'Design', 'Customer Support',
];

const RegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [register, { isLoading, error }] = useRegisterMutation();

  const {
    register: formRegister,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: { role: 'EMPLOYEE' },
  });

  const password = watch('password', '');

  const passwordStrength = () => {
    let score = 0;
    if (password.length >= 8) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;
    return score;
  };

  const strengthColors = ['bg-rose-500', 'bg-amber-500', 'bg-yellow-500', 'bg-emerald-500'];
  const strengthLabels = ['Weak', 'Fair', 'Good', 'Strong'];
  const strength = passwordStrength();

  const onSubmit = async (data: RegisterForm) => {
    try {
      const result = await register({
        username: data.username,
        email: data.email,
        password: data.password,
        role: data.role,
        department: data.department,
      }).unwrap();

      if (result.success && result.data) {
        const { accessToken, refreshToken, userId, username, email, role } = result.data;
        dispatch(setCredentials({
          accessToken,
          refreshToken,
          user: { id: userId, username, email, role },
        }));
        navigate('/dashboard');
      }
    } catch {
      // error handled via RTK Query's error state
    }
  };

  const getErrorMessage = () => {
    if (!error) return null;
    if ('status' in error) {
      if (error.status === 409) return 'Username or email already exists.';
      const data = error.data as { message?: string } | undefined;
      return data?.message || 'Registration failed. Please try again.';
    }
    return 'Network error. Please check your connection.';
  };

  const errorMessage = getErrorMessage();

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-8" style={{
      background: 'radial-gradient(ellipse at top right, #1E1B4B 0%, #0F172A 40%, #1E293B 100%)',
    }}>
      {/* Background orbs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-0 right-0 w-96 h-96 rounded-full opacity-15"
          style={{ background: 'radial-gradient(circle, #8B5CF6, transparent)' }} />
        <div className="absolute bottom-0 left-0 w-64 h-64 rounded-full opacity-10"
          style={{ background: 'radial-gradient(circle, #6366F1, transparent)' }} />
      </div>

      <div className="w-full max-w-lg relative z-10">
        <div className="glass-card p-8" style={{ border: '1px solid rgba(99,102,241,0.2)' }}>
          {/* Brand */}
          <div className="flex items-center gap-3 mb-7">
            <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-lg">
              <Zap size={22} className="text-white" />
            </div>
            <div>
              <h1 className="text-xl font-bold gradient-text">SEIP</h1>
              <p className="text-xs text-slate-500">Smart Expense Intelligence Platform</p>
            </div>
          </div>

          <div className="mb-6">
            <h2 className="text-2xl font-bold text-white mb-1">Create account</h2>
            <p className="text-slate-400 text-sm">Join the platform to manage expenses intelligently</p>
          </div>

          {errorMessage && (
            <div className="mb-5 flex items-center gap-2.5 bg-rose-950 border border-rose-800 text-rose-300 px-4 py-3 rounded-lg text-sm">
              <AlertCircle size={16} className="flex-shrink-0" />
              {errorMessage}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {/* Username + Email row */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1.5">Username</label>
                <input
                  {...formRegister('username')}
                  type="text"
                  placeholder="johndoe"
                  className={`form-input ${errors.username ? 'border-rose-500' : ''}`}
                  autoComplete="username"
                />
                {errors.username && <p className="mt-1 text-xs text-rose-400">{errors.username.message}</p>}
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1.5">Email</label>
                <input
                  {...formRegister('email')}
                  type="email"
                  placeholder="john@example.com"
                  className={`form-input ${errors.email ? 'border-rose-500' : ''}`}
                  autoComplete="email"
                />
                {errors.email && <p className="mt-1 text-xs text-rose-400">{errors.email.message}</p>}
              </div>
            </div>

            {/* Password */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">Password</label>
              <div className="relative">
                <input
                  {...formRegister('password')}
                  type={showPassword ? 'text' : 'password'}
                  placeholder="Create a strong password"
                  className={`form-input pr-11 ${errors.password ? 'border-rose-500' : ''}`}
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {/* Password strength */}
              {password && (
                <div className="mt-2">
                  <div className="flex gap-1 mb-1">
                    {Array.from({ length: 4 }).map((_, i) => (
                      <div
                        key={i}
                        className={`h-1 flex-1 rounded-full transition-all ${
                          i < strength ? strengthColors[strength - 1] : 'bg-slate-700'
                        }`}
                      />
                    ))}
                  </div>
                  <p className="text-xs text-slate-500">
                    Strength: <span className={strength >= 3 ? 'font-medium text-emerald-400' : strength >= 2 ? 'font-medium text-amber-400' : 'font-medium text-rose-400'}>
                      {strengthLabels[strength - 1] || 'Very Weak'}
                    </span>
                  </p>
                </div>
              )}
              {errors.password && <p className="mt-1 text-xs text-rose-400">{errors.password.message}</p>}
            </div>

            {/* Confirm Password */}
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">Confirm Password</label>
              <div className="relative">
                <input
                  {...formRegister('confirmPassword')}
                  type={showConfirm ? 'text' : 'password'}
                  placeholder="Repeat your password"
                  className={`form-input pr-11 ${errors.confirmPassword ? 'border-rose-500' : ''}`}
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  onClick={() => setShowConfirm(!showConfirm)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
                >
                  {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {errors.confirmPassword && <p className="mt-1 text-xs text-rose-400">{errors.confirmPassword.message}</p>}
            </div>

            {/* Role + Department row */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1.5">Role</label>
                <select {...formRegister('role')} className="form-input">
                  <option value="EMPLOYEE">Employee</option>
                  <option value="MANAGER">Manager</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1.5">Department</label>
                <select {...formRegister('department')} className="form-input">
                  <option value="">Select department</option>
                  {DEPARTMENTS.map((d) => (
                    <option key={d} value={d}>{d}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* Password requirements */}
            <div className="bg-slate-800/50 rounded-lg p-3 space-y-1.5">
              {[
                { check: password.length >= 8, label: 'At least 8 characters' },
                { check: /[A-Z]/.test(password), label: 'One uppercase letter' },
                { check: /[0-9]/.test(password), label: 'One number' },
              ].map(({ check, label }) => (
                <div key={label} className="flex items-center gap-2 text-xs">
                  <CheckCircle2
                    size={13}
                    className={check ? 'text-emerald-400' : 'text-slate-600'}
                  />
                  <span className={check ? 'text-emerald-400' : 'text-slate-500'}>{label}</span>
                </div>
              ))}
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={isLoading}
              className="btn-primary w-full justify-center py-3 text-sm"
            >
              {isLoading ? (
                <span className="flex items-center gap-2">
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Creating account...
                </span>
              ) : (
                <span className="flex items-center gap-2">
                  <UserPlus size={16} />
                  Create Account
                </span>
              )}
            </button>
          </form>

          <p className="text-center text-sm text-slate-500 mt-5">
            Already have an account?{' '}
            <Link to="/login" className="text-indigo-400 hover:text-indigo-300 font-medium transition-colors">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
