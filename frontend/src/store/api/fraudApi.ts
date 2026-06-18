import { baseApi } from './baseApi';
import { ApiResponse, FraudAnalysis, FraudSummary, PageResponse, Expense } from '../../types';

export const fraudApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getFraudAnalysis: builder.query<ApiResponse<FraudAnalysis>, number>({
      query: (expenseId) => `/fraud/analysis/${expenseId}`,
      providesTags: (_result, _error, id) => [{ type: 'Fraud', id }],
    }),

    getFraudSummary: builder.query<ApiResponse<FraudSummary>, void>({
      query: () => '/fraud/summary',
      providesTags: ['Fraud'],
    }),

    getHighRiskExpenses: builder.query<ApiResponse<PageResponse<Expense>>, { page?: number; size?: number }>({
      query: (params) => ({
        url: '/fraud/high-risk',
        params: {
          page: params.page ?? 0,
          size: params.size ?? 20,
        },
      }),
      providesTags: ['Fraud'],
    }),

    triggerFraudAnalysis: builder.mutation<ApiResponse<FraudAnalysis>, number>({
      query: (expenseId) => ({
        url: `/fraud/analyze/${expenseId}`,
        method: 'POST',
      }),
      invalidatesTags: (_result, _error, id) => [{ type: 'Fraud', id: id }],
    }),

    getFraudTrend: builder.query<ApiResponse<{ month: string; highRisk: number; mediumRisk: number; lowRisk: number }[]>, { months?: number }>({
      query: (params) => ({
        url: '/fraud/trend',
        params: { months: params.months ?? 6 },
      }),
      providesTags: ['Fraud'],
    }),
  }),
});

export const {
  useGetFraudAnalysisQuery,
  useGetFraudSummaryQuery,
  useGetHighRiskExpensesQuery,
  useTriggerFraudAnalysisMutation,
  useGetFraudTrendQuery,
} = fraudApi;
