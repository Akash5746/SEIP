const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8880';

const toAbsoluteReceiptUrl = (receiptUrl: string) =>
  receiptUrl.startsWith('http://') || receiptUrl.startsWith('https://')
    ? receiptUrl
    : `${API_BASE_URL}${receiptUrl.startsWith('/') ? receiptUrl : `/${receiptUrl}`}`;

export const openReceiptInNewTab = async (receiptUrl: string, accessToken?: string | null) => {
  const response = await fetch(toAbsoluteReceiptUrl(receiptUrl), {
    headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined,
  });

  if (!response.ok) {
    throw new Error(`Failed to open receipt: ${response.status}`);
  }

  const blob = await response.blob();
  const objectUrl = URL.createObjectURL(blob);
  window.open(objectUrl, '_blank', 'noopener,noreferrer');
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
};
