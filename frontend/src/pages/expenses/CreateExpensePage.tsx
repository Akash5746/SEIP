import React, { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  ChevronRight,
  ChevronLeft,
  Plus,
  Trash2,
  Upload,
  X,
  Check,
  FileText,
  DollarSign,
  Building,
  Calendar,
  Tag,
  AlignLeft,
  AlertCircle,
} from 'lucide-react';
import { useCreateExpenseMutation, useGetCategoriesQuery, useUploadReceiptMutation, useSubmitExpenseMutation } from '../../store/api/expenseApi';
import { format } from 'date-fns';

const expenseSchema = z.object({
  title: z.string().min(3, 'Title must be at least 3 characters').max(100),
  description: z.string().min(5, 'Description is required').max(500),
  amount: z.coerce.number().positive('Amount must be positive'),
  currency: z.string().default('USD'),
  merchantName: z.string().min(2, 'Merchant name is required'),
  expenseDate: z.string().min(1, 'Date is required'),
  categoryId: z.coerce.number().min(1, 'Category is required'),
  items: z.array(z.object({
    description: z.string().min(1, 'Description required'),
    amount: z.coerce.number().positive('Must be positive'),
    quantity: z.coerce.number().int().min(1, 'Min 1'),
  })).optional(),
});

type ExpenseFormValues = z.infer<typeof expenseSchema>;

const STEPS = ['Basic Details', 'Receipt Upload', 'Review & Submit'];

const CURRENCIES = ['USD', 'EUR', 'GBP', 'INR', 'CAD', 'AUD', 'JPY', 'SGD'];

const CreateExpensePage: React.FC = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(0);
  const [createdExpenseId, setCreatedExpenseId] = useState<number | null>(null);
  const [uploadedFiles, setUploadedFiles] = useState<File[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const { data: categoriesData } = useGetCategoriesQuery();
  const [createExpense] = useCreateExpenseMutation();
  const [uploadReceipt] = useUploadReceiptMutation();
  const [submitExpense] = useSubmitExpenseMutation();

  const categories = categoriesData?.data ?? [];

  const {
    register,
    handleSubmit,
    watch,
    control,
    formState: { errors },
    trigger,
  } = useForm<ExpenseFormValues>({
    resolver: zodResolver(expenseSchema),
    defaultValues: {
      currency: 'USD',
      expenseDate: format(new Date(), 'yyyy-MM-dd'),
      items: [{ description: '', amount: 0, quantity: 1 }],
    },
  });

  const { fields, append, remove } = useFieldArray({ control, name: 'items' });

  const watchedValues = watch();
  const totalItemAmount = (watchedValues.items ?? []).reduce(
    (sum, item) => sum + (item.amount || 0) * (item.quantity || 1),
    0
  );

  const handleNextStep = async () => {
    if (step === 0) {
      const valid = await trigger(['title', 'description', 'amount', 'merchantName', 'expenseDate', 'categoryId']);
      if (!valid) return;

      try {
        const result = await createExpense({
          title: watchedValues.title,
          description: watchedValues.description,
          amount: watchedValues.amount,
          currency: watchedValues.currency,
          merchantName: watchedValues.merchantName,
          expenseDate: watchedValues.expenseDate,
          categoryId: watchedValues.categoryId,
          items: (watchedValues.items ?? []).filter((i) => i.description && i.amount > 0),
        }).unwrap();

        if (result.success) {
          setCreatedExpenseId(result.data.id);
          setStep(1);
        }
      } catch {
        setSubmitError('Failed to save expense. Please try again.');
      }
    } else if (step === 1) {
      setStep(2);
    }
  };

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const files = Array.from(e.dataTransfer.files).filter((f) =>
      ['image/jpeg', 'image/png', 'image/webp', 'application/pdf'].includes(f.type)
    );
    setUploadedFiles((prev) => [...prev, ...files]);
  }, []);

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setUploadedFiles((prev) => [...prev, ...Array.from(e.target.files!)]);
    }
  };

  const handleFinalSubmit = async () => {
    if (!createdExpenseId) return;
    setIsSubmitting(true);
    setSubmitError(null);

    try {
      // Upload receipts
      for (const file of uploadedFiles) {
        const formData = new FormData();
        formData.append('file', file);
        await uploadReceipt({ expenseId: createdExpenseId, file: formData }).unwrap();
      }

      // Submit for approval
      await submitExpense(createdExpenseId).unwrap();
      navigate(`/expenses/${createdExpenseId}`);
    } catch {
      setSubmitError('Submission failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getCategoryName = (id: number) =>
    categories.find((c) => c.id === id)?.name || '—';

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-white">Create New Expense</h2>
        <p className="text-sm text-slate-500 mt-0.5">Fill in the details to submit an expense for approval</p>
      </div>

      {/* Stepper */}
      <div className="glass-card p-4">
        <div className="flex items-center">
          {STEPS.map((label, i) => (
            <React.Fragment key={label}>
              <div className="flex items-center gap-2.5">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold transition-all ${
                  i < step
                    ? 'bg-emerald-600 text-white'
                    : i === step
                    ? 'bg-gradient-to-br from-indigo-500 to-violet-600 text-white'
                    : 'bg-slate-800 text-slate-500'
                }`}>
                  {i < step ? <Check size={15} /> : i + 1}
                </div>
                <span className={`text-sm font-medium hidden sm:block ${i === step ? 'text-white' : i < step ? 'text-emerald-400' : 'text-slate-600'}`}>
                  {label}
                </span>
              </div>
              {i < STEPS.length - 1 && (
                <div className={`flex-1 h-px mx-4 transition-all ${i < step ? 'bg-emerald-600' : 'bg-slate-700'}`} />
              )}
            </React.Fragment>
          ))}
        </div>
      </div>

      {/* Error alert */}
      {submitError && (
        <div className="flex items-center gap-2.5 bg-rose-950 border border-rose-800 text-rose-300 px-4 py-3 rounded-lg text-sm">
          <AlertCircle size={16} />
          {submitError}
        </div>
      )}

      {/* Step 1: Basic Details */}
      {step === 0 && (
        <div className="glass-card p-6 space-y-5">
          <h3 className="font-semibold text-white">Expense Details</h3>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">
              <span className="flex items-center gap-1.5"><FileText size={14} /> Title *</span>
            </label>
            <input {...register('title')} placeholder="e.g. Team lunch at Downtown Grill" className={`form-input ${errors.title ? 'border-rose-500' : ''}`} />
            {errors.title && <p className="mt-1 text-xs text-rose-400">{errors.title.message}</p>}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">
                <span className="flex items-center gap-1.5"><DollarSign size={14} /> Amount *</span>
              </label>
              <input
                {...register('amount')}
                type="number"
                step="0.01"
                min="0"
                placeholder="0.00"
                className={`form-input ${errors.amount ? 'border-rose-500' : ''}`}
              />
              {errors.amount && <p className="mt-1 text-xs text-rose-400">{errors.amount.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">Currency</label>
              <select {...register('currency')} className="form-input">
                {CURRENCIES.map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">
                <span className="flex items-center gap-1.5"><Building size={14} /> Merchant *</span>
              </label>
              <input {...register('merchantName')} placeholder="e.g. Marriott Hotel" className={`form-input ${errors.merchantName ? 'border-rose-500' : ''}`} />
              {errors.merchantName && <p className="mt-1 text-xs text-rose-400">{errors.merchantName.message}</p>}
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">
                <span className="flex items-center gap-1.5"><Calendar size={14} /> Date *</span>
              </label>
              <input
                {...register('expenseDate')}
                type="date"
                max={format(new Date(), 'yyyy-MM-dd')}
                className={`form-input ${errors.expenseDate ? 'border-rose-500' : ''}`}
              />
              {errors.expenseDate && <p className="mt-1 text-xs text-rose-400">{errors.expenseDate.message}</p>}
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">
              <span className="flex items-center gap-1.5"><Tag size={14} /> Category *</span>
            </label>
            <select {...register('categoryId')} className={`form-input ${errors.categoryId ? 'border-rose-500' : ''}`}>
              <option value="">Select a category</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
            {errors.categoryId && <p className="mt-1 text-xs text-rose-400">{errors.categoryId.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-slate-300 mb-1.5">
              <span className="flex items-center gap-1.5"><AlignLeft size={14} /> Description *</span>
            </label>
            <textarea
              {...register('description')}
              rows={3}
              placeholder="Provide a brief description of this expense..."
              className={`form-input resize-none ${errors.description ? 'border-rose-500' : ''}`}
            />
            {errors.description && <p className="mt-1 text-xs text-rose-400">{errors.description.message}</p>}
          </div>

          {/* Line Items */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="text-sm font-medium text-slate-300">Line Items (optional)</label>
              <button
                type="button"
                onClick={() => append({ description: '', amount: 0, quantity: 1 })}
                className="text-xs text-indigo-400 hover:text-indigo-300 flex items-center gap-1"
              >
                <Plus size={13} /> Add item
              </button>
            </div>
            <div className="space-y-2">
              {fields.map((field, index) => (
                <div key={field.id} className="flex items-center gap-2">
                  <input
                    {...register(`items.${index}.description`)}
                    placeholder="Description"
                    className="form-input flex-1 text-sm"
                  />
                  <input
                    {...register(`items.${index}.quantity`)}
                    type="number"
                    min="1"
                    placeholder="Qty"
                    className="form-input w-16 text-sm"
                  />
                  <input
                    {...register(`items.${index}.amount`)}
                    type="number"
                    step="0.01"
                    placeholder="Price"
                    className="form-input w-24 text-sm"
                  />
                  {fields.length > 1 && (
                    <button type="button" onClick={() => remove(index)} className="p-2 text-slate-600 hover:text-rose-400 flex-shrink-0">
                      <Trash2 size={15} />
                    </button>
                  )}
                </div>
              ))}
            </div>
            {totalItemAmount > 0 && (
              <div className="mt-3 flex justify-between text-sm">
                <span className="text-slate-400">Items total:</span>
                <span className="font-semibold text-white">${totalItemAmount.toFixed(2)}</span>
              </div>
            )}
          </div>

          {/* Amount display */}
          <div className="bg-slate-800/60 rounded-xl p-4 flex justify-between items-center">
            <span className="text-slate-400 font-medium">Total Amount</span>
            <span className="text-2xl font-bold gradient-text">
              {watchedValues.currency || 'USD'} {(watchedValues.amount || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
            </span>
          </div>
        </div>
      )}

      {/* Step 2: Receipt Upload */}
      {step === 1 && (
        <div className="glass-card p-6 space-y-5">
          <div>
            <h3 className="font-semibold text-white">Upload Receipts</h3>
            <p className="text-sm text-slate-500 mt-1">Attach photos or PDFs of your receipts (optional but recommended)</p>
          </div>

          {/* Drop zone */}
          <div
            onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={handleDrop}
            className={`border-2 border-dashed rounded-xl p-10 text-center transition-all cursor-pointer ${
              isDragging
                ? 'border-indigo-500 bg-indigo-950/30'
                : 'border-slate-700 hover:border-indigo-600 hover:bg-slate-800/30'
            }`}
            onClick={() => document.getElementById('receipt-input')?.click()}
          >
            <Upload size={32} className={`mx-auto mb-3 ${isDragging ? 'text-indigo-400' : 'text-slate-600'}`} />
            <p className="text-slate-300 font-medium">Drop receipts here or click to browse</p>
            <p className="text-sm text-slate-600 mt-1">Supports JPG, PNG, WEBP, PDF · Max 10MB each</p>
            <input
              id="receipt-input"
              type="file"
              accept=".jpg,.jpeg,.png,.webp,.pdf"
              multiple
              className="hidden"
              onChange={handleFileInput}
            />
          </div>

          {/* Uploaded files list */}
          {uploadedFiles.length > 0 && (
            <div className="space-y-2">
              <p className="text-sm font-medium text-slate-300">Uploaded files ({uploadedFiles.length})</p>
              {uploadedFiles.map((file, i) => (
                <div key={i} className="flex items-center gap-3 p-3 bg-slate-800/60 rounded-lg border border-slate-700">
                  <div className="w-8 h-8 rounded-lg bg-indigo-950 border border-indigo-800 flex items-center justify-center flex-shrink-0">
                    <FileText size={15} className="text-indigo-400" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-white truncate">{file.name}</p>
                    <p className="text-xs text-slate-500">{(file.size / 1024).toFixed(0)} KB</p>
                  </div>
                  <button
                    onClick={() => setUploadedFiles((prev) => prev.filter((_, idx) => idx !== i))}
                    className="p-1 text-slate-500 hover:text-rose-400 transition-colors"
                  >
                    <X size={15} />
                  </button>
                </div>
              ))}
            </div>
          )}

          <p className="text-xs text-slate-600">
            You can skip this step and add receipts later from the expense details page.
          </p>
        </div>
      )}

      {/* Step 3: Review */}
      {step === 2 && (
        <div className="glass-card p-6 space-y-5">
          <h3 className="font-semibold text-white">Review & Submit</h3>

          <div className="bg-gradient-to-r from-indigo-950 to-violet-950 border border-indigo-800/50 rounded-xl p-5">
            <div className="flex justify-between items-start mb-4">
              <div>
                <h4 className="text-lg font-bold text-white">{watchedValues.title}</h4>
                <p className="text-slate-400 text-sm mt-0.5">{watchedValues.merchantName}</p>
              </div>
              <div className="text-right">
                <p className="text-2xl font-bold gradient-text">
                  {watchedValues.currency} {(watchedValues.amount || 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-slate-500">Date:</span>
                <span className="text-slate-300 ml-2">
                  {watchedValues.expenseDate ? format(new Date(watchedValues.expenseDate), 'MMM d, yyyy') : '—'}
                </span>
              </div>
              <div>
                <span className="text-slate-500">Category:</span>
                <span className="text-slate-300 ml-2">
                  {categories.find((c) => Number(c.id) === Number(watchedValues.categoryId))?.name || '—'}
                </span>
              </div>
              <div className="col-span-2">
                <span className="text-slate-500">Description:</span>
                <span className="text-slate-300 ml-2">{watchedValues.description}</span>
              </div>
              {uploadedFiles.length > 0 && (
                <div className="col-span-2">
                  <span className="text-slate-500">Receipts:</span>
                  <span className="text-emerald-400 ml-2">{uploadedFiles.length} file(s) attached</span>
                </div>
              )}
            </div>
          </div>

          <div className="bg-amber-950/30 border border-amber-900/50 rounded-lg p-4 text-sm text-amber-300 flex gap-2.5">
            <AlertCircle size={16} className="flex-shrink-0 mt-0.5" />
            <p>Once submitted, this expense will be sent to your manager for approval and a fraud analysis will be performed automatically.</p>
          </div>
        </div>
      )}

      {/* Navigation */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => step > 0 ? setStep(step - 1) : navigate('/expenses')}
          className="btn-secondary text-sm"
        >
          <ChevronLeft size={16} />
          {step === 0 ? 'Cancel' : 'Back'}
        </button>

        {step < 2 ? (
          <button onClick={handleNextStep} className="btn-primary text-sm">
            {step === 0 ? 'Save & Continue' : 'Continue to Review'}
            <ChevronRight size={16} />
          </button>
        ) : (
          <button
            onClick={handleFinalSubmit}
            disabled={isSubmitting}
            className="btn-primary text-sm"
          >
            {isSubmitting ? (
              <span className="flex items-center gap-2">
                <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Submitting...
              </span>
            ) : (
              <span className="flex items-center gap-2">
                <Check size={16} />
                Submit for Approval
              </span>
            )}
          </button>
        )}
      </div>
    </div>
  );
};

export default CreateExpensePage;
