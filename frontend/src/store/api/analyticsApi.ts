import { baseApi } from './baseApi';
import { ApiResponse, MonthlySpend, CategorySpend, DepartmentSpend } from '../../types';

interface ReportFilters {
  startDate?: string;
  endDate?: string;
  year?: number;
  department?: string;
}

export const analyticsApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getMonthlySpend: builder.query<ApiResponse<MonthlySpend[]>, ReportFilters>({
      query: (params) => ({
        url: '/analytics/monthly-spend',
        params,
      }),
      providesTags: ['Analytics'],
    }),

    getCategorySpend: builder.query<ApiResponse<CategorySpend[]>, ReportFilters>({
      query: (params) => ({
        url: '/analytics/category-spend',
        params,
      }),
      providesTags: ['Analytics'],
    }),

    getDepartmentSpend: builder.query<ApiResponse<DepartmentSpend[]>, ReportFilters>({
      query: (params) => ({
        url: '/analytics/department-spend',
        params,
      }),
      providesTags: ['Analytics'],
    }),

    getDashboardStats: builder.query<ApiResponse<{
      totalExpenses: number;
      pendingCount: number;
      thisMonthAmount: number;
      highRiskAlerts: number;
      approvedThisMonth: number;
      rejectedThisMonth: number;
    }>, void>({
      query: () => '/analytics/dashboard-stats',
      providesTags: ['Analytics'],
    }),

    getRecentExpenses: builder.query<ApiResponse<{
      id: number;
      title: string;
      amount: number;
      status: string;
      riskLevel: string;
      createdAt: string;
      merchantName: string;
    }[]>, { limit?: number }>({
      query: (params) => ({
        url: '/analytics/recent-expenses',
        params: { limit: params.limit ?? 5 },
      }),
      providesTags: ['Analytics', 'Expense'],
    }),
  }),
});

export const {
  useGetMonthlySpendQuery,
  useGetCategorySpendQuery,
  useGetDepartmentSpendQuery,
  useGetDashboardStatsQuery,
  useGetRecentExpensesQuery,
} = analyticsApi;
