'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { authApi, AuthRequest } from '@/lib/api';
import { useDashboardStore } from '@/store/dashboardStore';

export default function LoginPage() {
  const router = useRouter();
  const { setUser } = useDashboardStore();
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<AuthRequest>();

  const onSubmit = async (data: AuthRequest) => {
    setLoading(true);
    setServerError('');
    try {
      const response = await authApi.login(data);
      setUser({ token: response.data.token, email: response.data.email });
      router.push('/dashboard');
    } catch (err: any) {
      setServerError(err.response?.data?.message || 'Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const fillDemo = () => {
    const emailInput = document.querySelector<HTMLInputElement>('input[name="email"]');
    const passInput = document.querySelector<HTMLInputElement>('input[name="password"]');
    if (emailInput && passInput) {
      const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
        window.HTMLInputElement.prototype, 'value'
      )?.set;
      if (nativeInputValueSetter) {
        nativeInputValueSetter.call(emailInput, 'user1@itr.com');
        emailInput.dispatchEvent(new Event('change', { bubbles: true }));
        nativeInputValueSetter.call(passInput, 'password123');
        passInput.dispatchEvent(new Event('change', { bubbles: true }));
      }
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#0d2137', fontFamily: "'Segoe UI',system-ui,sans-serif", padding: 16 }}>
      <div style={{ width: '100%', maxWidth: 420 }}>
        <div style={{ background: '#fff', borderRadius: 14, overflow: 'hidden', boxShadow: '0 24px 80px rgba(0,0,0,0.45)' }}>
          <div style={{ background: 'linear-gradient(160deg,#0a1829 0%,#1565c0 100%)', padding: '28px 28px 22px', textAlign: 'center' }}>
            <div style={{ width: 48, height: 48, borderRadius: 12, background: 'linear-gradient(135deg,#1976d2,#42a5f5)', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', fontSize: 22, fontWeight: 900, color: '#fff', marginBottom: 10 }}>₹</div>
            <div style={{ fontSize: 20, fontWeight: 800, color: '#e2eaf6', lineHeight: 1 }}>MyTaxERP</div>
            <div style={{ fontSize: 10.5, color: '#90c0e0', marginTop: 4, letterSpacing: '0.08em' }}>INCOME TAX FILING ASSISTANT</div>
            <div style={{ fontSize: 13, color: '#90b4d0', marginTop: 10 }}>Sign in to your account</div>
          </div>
          <div className="p-7">

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
              <input
                type="email"
                className="input-field"
                placeholder="you@example.com"
                suppressHydrationWarning
                {...register('email', {
                  required: 'Email is required',
                  pattern: {
                    value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                    message: 'Invalid email format',
                  },
                })}
              />
              {errors.email && (
                <p className="text-red-500 text-sm mt-1">{errors.email.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
              <input
                type="password"
                className="input-field"
                placeholder="Min 8 characters"
                suppressHydrationWarning
                {...register('password', {
                  required: 'Password is required',
                  minLength: {
                    value: 8,
                    message: 'Password must be at least 8 characters',
                  },
                })}
              />
              {errors.password && (
                <p className="text-red-500 text-sm mt-1">{errors.password.message}</p>
              )}
            </div>

            {serverError && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                <p className="text-red-600 text-sm">{serverError}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="btn-primary w-full flex items-center justify-center"
              suppressHydrationWarning
            >
              {loading ? (
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white" />
              ) : (
                'Sign In'
              )}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500">
              Don&apos;t have an account?{' '}
              <Link href="/register" className="text-primary-600 hover:text-primary-700 font-medium">
                Register
              </Link>
            </p>
          </div>

          <div className="mt-4 pt-4 border-t border-gray-100">
            <p className="text-xs text-gray-400 text-center mb-2">Demo Credentials</p>
            <button
              type="button"
              onClick={fillDemo}
              className="w-full text-xs bg-gray-50 hover:bg-gray-100 text-gray-600 py-2 px-3 rounded-lg transition-colors"
              suppressHydrationWarning
            >
              user1@itr.com / password123
            </button>
          </div>
          </div>
        </div>
      </div>
    </div>
  );
}
