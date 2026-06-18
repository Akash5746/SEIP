import { baseApi } from './baseApi';
import { ApiResponse, PageResponse, Employee } from '../../types';

interface UpdateUserRequest {
  role?: string;
  department?: string;
  active?: boolean;
}

export const userApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getEmployees: builder.query<ApiResponse<PageResponse<Employee>>, { page?: number; size?: number; search?: string; department?: string }>({
      query: (params) => ({
        url: '/employees',
        params: {
          page: params.page ?? 0,
          size: params.size ?? 20,
          ...(params.search && { search: params.search }),
          ...(params.department && { department: params.department }),
        },
      }),
      providesTags: ['Employee'],
    }),

    getEmployeeById: builder.query<ApiResponse<Employee>, number>({
      query: (id) => `/employees/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Employee', id }],
    }),

    updateEmployee: builder.mutation<ApiResponse<Employee>, { id: number; body: UpdateUserRequest }>({
      query: ({ id, body }) => ({
        url: `/employees/${id}`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: ['Employee'],
    }),

    activateEmployee: builder.mutation<ApiResponse<Employee>, number>({
      query: (id) => ({
        url: `/employees/${id}/activate`,
        method: 'POST',
      }),
      invalidatesTags: ['Employee'],
    }),

    deactivateEmployee: builder.mutation<ApiResponse<Employee>, number>({
      query: (id) => ({
        url: `/employees/${id}/deactivate`,
        method: 'POST',
      }),
      invalidatesTags: ['Employee'],
    }),

    getDepartments: builder.query<ApiResponse<string[]>, void>({
      query: () => '/employees/departments',
      providesTags: ['Employee'],
    }),
  }),
});

export const {
  useGetEmployeesQuery,
  useGetEmployeeByIdQuery,
  useUpdateEmployeeMutation,
  useActivateEmployeeMutation,
  useDeactivateEmployeeMutation,
  useGetDepartmentsQuery,
} = userApi;
