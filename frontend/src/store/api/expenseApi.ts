import { baseApi } from './baseApi';
import { ApiResponse, PageResponse, Expense, Category, CreateExpenseRequest } from '../../types';

interface ExpenseFilters {
  page?: number;
  size?: number;
  status?: string;
  search?: string;
  startDate?: string;
  endDate?: string;
  categoryId?: number;
  riskLevel?: string;
}

interface ApprovalAction {
  expenseId: number;
  notes?: string;
}

export const expenseApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getExpenses: builder.query<ApiResponse<PageResponse<Expense>>, ExpenseFilters>({
      query: (params) => ({
        url: '/expenses',
        params: {
          page: params.page ?? 0,
          size: params.size ?? 20,
          ...(params.status && { status: params.status }),
          ...(params.search && { search: params.search }),
          ...(params.startDate && { startDate: params.startDate }),
          ...(params.endDate && { endDate: params.endDate }),
          ...(params.categoryId && { categoryId: params.categoryId }),
          ...(params.riskLevel && { riskLevel: params.riskLevel }),
        },
      }),
      providesTags: ['Expense'],
    }),

    getExpenseById: builder.query<ApiResponse<Expense>, number>({
      query: (id) => `/expenses/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Expense', id }],
    }),

    getManagerExpenseById: builder.query<ApiResponse<Expense>, number>({
      query: (id) => `/expenses/manager/item/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Expense', id }],
    }),

    createExpense: builder.mutation<ApiResponse<Expense>, CreateExpenseRequest>({
      query: (body) => ({
        url: '/expenses',
        method: 'POST',
        body,
      }),
      invalidatesTags: ['Expense'],
    }),

    updateExpense: builder.mutation<ApiResponse<Expense>, { id: number; body: Partial<CreateExpenseRequest> }>({
      query: ({ id, body }) => ({
        url: `/expenses/${id}`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_result, _error, { id }) => [{ type: 'Expense', id }, 'Expense'],
    }),

    submitExpense: builder.mutation<ApiResponse<Expense>, number>({
      query: (id) => ({
        url: `/expenses/${id}/submit`,
        method: 'POST',
      }),
      invalidatesTags: ['Expense'],
    }),

    approveExpense: builder.mutation<ApiResponse<Expense>, ApprovalAction>({
      query: ({ expenseId, notes }) => ({
        url: `/expenses/${expenseId}/approve`,
        method: 'POST',
        body: { notes },
      }),
      invalidatesTags: ['Expense'],
    }),

    rejectExpense: builder.mutation<ApiResponse<Expense>, ApprovalAction>({
      query: ({ expenseId, notes }) => ({
        url: `/expenses/${expenseId}/reject`,
        method: 'POST',
        body: { notes },
      }),
      invalidatesTags: ['Expense'],
    }),

    requestExpenseChanges: builder.mutation<ApiResponse<Expense>, ApprovalAction>({
      query: ({ expenseId, notes }) => ({
        url: `/expenses/${expenseId}/request-changes`,
        method: 'POST',
        body: { notes },
      }),
      invalidatesTags: ['Expense'],
    }),

    uploadReceipt: builder.mutation<ApiResponse<{ receiptId: number; fileUrl: string }>, { expenseId: number; file: FormData }>({
      query: ({ expenseId, file }) => ({
        url: `/expenses/${expenseId}/receipts`,
        method: 'POST',
        body: file,
      }),
      invalidatesTags: (_result, _error, { expenseId }) => [{ type: 'Expense', id: expenseId }],
    }),

    deleteReceipt: builder.mutation<ApiResponse<null>, { expenseId: number; receiptId: number }>({
      query: ({ expenseId, receiptId }) => ({
        url: `/expenses/${expenseId}/receipts/${receiptId}`,
        method: 'DELETE',
      }),
      invalidatesTags: (_result, _error, { expenseId }) => [{ type: 'Expense', id: expenseId }],
    }),

    deleteExpense: builder.mutation<ApiResponse<null>, number>({
      query: (id) => ({
        url: `/expenses/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['Expense'],
    }),

    getCategories: builder.query<ApiResponse<Category[]>, void>({
      query: () => '/expenses/categories',
      providesTags: ['Category'],
    }),

    getPendingApprovals: builder.query<ApiResponse<PageResponse<Expense>>, { page?: number; size?: number; riskLevel?: string }>({
      query: (params) => ({
        url: '/expenses/pending-approvals',
        params: {
          page: params.page ?? 0,
          size: params.size ?? 20,
          ...(params.riskLevel && { riskLevel: params.riskLevel }),
        },
      }),
      providesTags: ['Expense'],
    }),

    getManagerEmployeeExpenses: builder.query<
      ApiResponse<PageResponse<Expense>>,
      { employeeAuthUserId: number; page?: number; size?: number }
    >({
      query: ({ employeeAuthUserId, page = 0, size = 20 }) => ({
        url: `/expenses/manager/employee/${employeeAuthUserId}`,
        params: { page, size },
      }),
      providesTags: ['Expense'],
    }),

    getMyExpenses: builder.query<ApiResponse<PageResponse<Expense>>, ExpenseFilters>({
      query: (params) => ({
        url: '/expenses/my',
        params: {
          page: params.page ?? 0,
          size: params.size ?? 20,
          ...(params.status && { status: params.status }),
        },
      }),
      providesTags: ['Expense'],
    }),
  }),
});

export const {
  useGetExpensesQuery,
  useGetExpenseByIdQuery,
  useGetManagerExpenseByIdQuery,
  useCreateExpenseMutation,
  useUpdateExpenseMutation,
  useSubmitExpenseMutation,
  useApproveExpenseMutation,
  useRejectExpenseMutation,
  useRequestExpenseChangesMutation,
  useUploadReceiptMutation,
  useDeleteReceiptMutation,
  useDeleteExpenseMutation,
  useGetCategoriesQuery,
  useGetPendingApprovalsQuery,
  useGetManagerEmployeeExpensesQuery,
  useGetMyExpensesQuery,
} = expenseApi;
