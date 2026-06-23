export const normalizeRole = (role?: string | null): string => {
  if (!role) {
    return '';
  }

  const normalized = role.trim().toUpperCase();
  return normalized.startsWith('ROLE_') ? normalized : `ROLE_${normalized}`;
};

export const hasRole = (role: string | null | undefined, ...allowedRoles: string[]): boolean => {
  const normalizedRole = normalizeRole(role);
  return allowedRoles.map((allowedRole) => normalizeRole(allowedRole)).includes(normalizedRole);
};

export const formatRoleLabel = (role?: string | null): string => {
  const normalized = normalizeRole(role).replace(/^ROLE_/, '');
  if (!normalized) {
    return 'Employee';
  }

  return normalized
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
};
