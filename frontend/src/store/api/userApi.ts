import { baseApi } from './baseApi';
import { ApiResponse, PageResponse, Employee } from '../../types';

interface UpdateUserRequest {
  role?: string;
  department?: string;
  active?: boolean;
}

type EmployeeApiDto = {
  id: number;
  authUserId?: number;
  username?: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role?: string;
  departmentName?: string;
  active: boolean;
  createdAt?: string;
};

const mapEmployee = (employee: EmployeeApiDto): Employee => ({
  id: employee.id,
  authUserId: employee.authUserId,
  username: employee.username || [employee.firstName, employee.lastName].filter(Boolean).join(' ') || employee.email,
  email: employee.email,
  firstName: employee.firstName,
  lastName: employee.lastName,
  role: employee.role || 'ROLE_EMPLOYEE',
  department: employee.departmentName,
  active: employee.active,
  createdAt: employee.createdAt,
});

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
      transformResponse: (response: ApiResponse<PageResponse<EmployeeApiDto>>) => ({
        ...response,
        data: {
          ...response.data,
          content: response.data.content.map(mapEmployee),
        },
      }),
      providesTags: ['Employee'],
    }),

    getEmployeeById: builder.query<ApiResponse<Employee>, number>({
      query: (id) => `/employees/${id}`,
      transformResponse: (response: ApiResponse<EmployeeApiDto>) => ({
        ...response,
        data: mapEmployee(response.data),
      }),
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

    getEmployeeByAuthUserId: builder.query<ApiResponse<Employee>, number>({
      query: (authUserId) => `/employees/auth/${authUserId}`,
      transformResponse: (response: ApiResponse<EmployeeApiDto>) => ({
        ...response,
        data: mapEmployee(response.data),
      }),
      providesTags: ['Employee'],
    }),

    getMyDepartmentEmployees: builder.query<ApiResponse<Employee[]>, void>({
      query: () => '/employees/my-department',
      transformResponse: (response: ApiResponse<EmployeeApiDto[]>) => ({
        ...response,
        data: response.data.map(mapEmployee),
      }),
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
  useGetEmployeeByAuthUserIdQuery,
  useGetMyDepartmentEmployeesQuery,
} = userApi;
