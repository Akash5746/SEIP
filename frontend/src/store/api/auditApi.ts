import { baseApi } from './baseApi';
import { ApiResponse, PageResponse, AuditLog } from '../../types';

interface AuditFilters {
  page?: number;
  size?: number;
  userId?: string;
  action?: string;
  resourceType?: string;
  startDate?: string;
  endDate?: string;
  search?: string;
}

export const auditApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getAuditLogs: builder.query<ApiResponse<PageResponse<AuditLog>>, AuditFilters>({
      query: (params) => ({
        url: '/audit/logs',
        params: {
          page: params.page ?? 0,
          size: params.size ?? 20,
          ...(params.userId && { userId: params.userId }),
          ...(params.action && { action: params.action }),
          ...(params.resourceType && { resourceType: params.resourceType }),
          ...(params.startDate && { startDate: params.startDate }),
          ...(params.endDate && { endDate: params.endDate }),
          ...(params.search && { search: params.search }),
        },
      }),
      providesTags: ['Audit'],
    }),

    getAuditLogById: builder.query<ApiResponse<AuditLog>, number>({
      query: (id) => `/audit/logs/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Audit', id }],
    }),

    getAuditActions: builder.query<ApiResponse<string[]>, void>({
      query: () => '/audit/actions',
      providesTags: ['Audit'],
    }),
  }),
});

export const {
  useGetAuditLogsQuery,
  useGetAuditLogByIdQuery,
  useGetAuditActionsQuery,
} = auditApi;
