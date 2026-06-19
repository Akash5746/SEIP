import {
  createApi,
  fetchBaseQuery,
  BaseQueryFn,
  FetchArgs,
  FetchBaseQueryError,
} from '@reduxjs/toolkit/query/react';
import { RootState } from '../index';
import { logout, setCredentials } from '../slices/authSlice';

const REFRESH_BUFFER_MS = 30_000;

const getTokenExpiryMs = (token: string): number | null => {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;

    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    const decoded = JSON.parse(atob(padded)) as { exp?: number };

    return decoded.exp ? decoded.exp * 1000 : null;
  } catch {
    return null;
  }
};

const shouldRefreshAccessToken = (token: string | null): boolean => {
  if (!token) return false;

  const expiryMs = getTokenExpiryMs(token);
  if (!expiryMs) return false;

  return expiryMs - Date.now() <= REFRESH_BUFFER_MS;
};

const baseQuery = fetchBaseQuery({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8880',
  prepareHeaders: (headers, { getState }) => {
    const token = (getState() as RootState).auth.accessToken;
    if (token) {
      headers.set('authorization', `Bearer ${token}`);
    }
    return headers;
  },
});

const baseQueryWithReauth: BaseQueryFn<
  string | FetchArgs,
  unknown,
  FetchBaseQueryError
> = async (args, api, extraOptions) => {
  const state = api.getState() as RootState;
  const { accessToken, refreshToken, user } = state.auth;

  if (shouldRefreshAccessToken(accessToken) && refreshToken) {
    const refreshResult = await baseQuery(
      {
        url: '/auth/refresh',
        method: 'POST',
        body: { refreshToken },
      },
      api,
      extraOptions
    );

    if (refreshResult.data) {
      const data = refreshResult.data as {
        data: { accessToken: string; refreshToken: string };
      };

      api.dispatch(
        setCredentials({
          accessToken: data.data.accessToken,
          refreshToken: data.data.refreshToken,
          ...(user ? { user } : {}),
        })
      );
    } else {
      api.dispatch(logout());
      return {
        error: {
          status: 401,
          data: { message: 'Session expired' },
        } as FetchBaseQueryError,
      };
    }
  }

  let result = await baseQuery(args, api, extraOptions);

  if (result.error && result.error.status === 401) {
    const latestState = api.getState() as RootState;
    const latestRefreshToken = latestState.auth.refreshToken;
    const latestUser = latestState.auth.user;

    if (latestRefreshToken) {
      const refreshResult = await baseQuery(
        {
          url: '/auth/refresh',
          method: 'POST',
          body: { refreshToken: latestRefreshToken },
        },
        api,
        extraOptions
      );

      if (refreshResult.data) {
        const data = refreshResult.data as { data: { accessToken: string; refreshToken: string } };
        api.dispatch(
          setCredentials({
            accessToken: data.data.accessToken,
            refreshToken: data.data.refreshToken,
            ...(latestUser ? { user: latestUser } : {}),
          })
        );
        result = await baseQuery(args, api, extraOptions);
      } else {
        api.dispatch(logout());
      }
    } else {
      api.dispatch(logout());
    }
  }

  return result;
};

export const baseApi = createApi({
  reducerPath: 'api',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['Expense', 'User', 'Employee', 'Fraud', 'Audit', 'Analytics', 'Category'],
  endpoints: () => ({}),
});
