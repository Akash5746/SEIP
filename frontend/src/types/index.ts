// ─── Auth ─────────────────────────────────────────────────────────────────────
export interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  department?: string;
  managerId?: number;
  active: boolean;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: number;
  username: string;
  email: string;
  role: string;
  expiresIn: number;
}

// ─── Expenses ─────────────────────────────────────────────────────────────────
export type ExpenseStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'REIMBURSED';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Category {
  id: number;
  name: string;
  code: string;
  description?: string;
}

export interface ExpenseItem {
  id: number;
  description: string;
  amount: number;
  quantity: number;
  unitPrice?: number;
}

export interface Receipt {
  id: number;
  fileName: string;
  fileUrl: string;
  contentType: string;
  uploadTime: string;
  fileSize?: number;
}

export interface Expense {
  id: number;
  expenseNumber: string;
  employeeId?: number;
  title: string;
  description: string;
  amount: number;
  currency: string;
  merchantName: string;
  categoryName?: string;
  expenseDate: string;
  status: ExpenseStatus;
  riskScore: number;
  riskLevel: RiskLevel;
  category: Category;
  items: ExpenseItem[];
  receipts: Receipt[];
  submittedAt: string;
  reviewedAt?: string;
  createdAt: string;
  updatedAt?: string;
  submittedBy?: User;
  reviewedBy?: User;
  reviewNotes?: string;
  approverId?: number;
}

export interface CreateExpenseRequest {
  title: string;
  description: string;
  amount: number;
  currency: string;
  merchantName: string;
  expenseDate: string;
  categoryId: number;
  items: Omit<ExpenseItem, 'id'>[];
}

// ─── Fraud ────────────────────────────────────────────────────────────────────
export interface FraudFlag {
  flagType: string;
  flagDescription: string;
  riskContribution: number;
}

export interface FraudAnalysis {
  expenseId: number;
  riskScore: number;
  riskLevel: RiskLevel;
  isDuplicate: boolean;
  flags: FraudFlag[];
  mlFraudProbability: number;
  analyzedAt?: string;
}

export interface FraudSummary {
  totalAnalyzed: number;
  highRiskCount: number;
  mediumRiskCount: number;
  lowRiskCount: number;
  duplicatesDetected: number;
  averageRiskScore: number;
}

// ─── Audit ────────────────────────────────────────────────────────────────────
export interface AuditLog {
  id: number;
  eventId: string;
  userId: string;
  username: string;
  action: string;
  resourceType: string;
  resourceId: string;
  timestamp: string;
  ipAddress?: string;
  service?: string;
  details?: string;
}

// ─── Analytics / Reports ──────────────────────────────────────────────────────
export interface MonthlySpend {
  month: number;
  year: number;
  totalAmount: number;
  expenseCount: number;
  monthName?: string;
}

export interface CategorySpend {
  categoryName: string;
  totalAmount: number;
  percentage: number;
  expenseCount: number;
}

export interface DepartmentSpend {
  department: string;
  totalAmount: number;
  expenseCount: number;
  averageAmount: number;
}

export interface ManagerDashboard {
  departmentName: string;
  totalEmployees: number;
  pendingApprovals: number;
  departmentExpensesThisMonth: number;
  highRiskAlerts: number;
  personalExpenseCount: number;
  personalThisMonthAmount: number;
}

// ─── API Response Wrappers ────────────────────────────────────────────────────
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ─── Employee ─────────────────────────────────────────────────────────────────
export interface Employee {
  id: number;
  authUserId?: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: string;
  department?: string;
  active: boolean;
  createdAt?: string;
}
