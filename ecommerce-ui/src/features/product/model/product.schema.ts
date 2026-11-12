import { z } from 'zod'

const SKU_PATTERN = /^[A-Z0-9]{1,10}$/
const PRICE_PATTERN = /^\d{1,10}(\.\d{1,2})?$/
const WEIGHT_PATTERN = /^\d{1,5}(\.\d{1,3})?$/

export const productFormSchema = z.object({
  sku: z
    .string()
    .trim()
    .min(1, 'SKU is required')
    .regex(SKU_PATTERN, 'SKU must have 1 to 10 characters, only uppercase letters and digits'),
  name: z.string().trim().min(1, 'Name is required').max(255, 'Name must not exceed 255 characters'),
  description: z.string().max(5000, 'Description must not exceed 5000 characters'),
  category: z.string().trim().min(1, 'Category is required').max(100, 'Category must not exceed 100 characters'),
  price: z
    .string()
    .trim()
    .refine((v) => PRICE_PATTERN.test(v), 'Price must be a number with up to 2 decimals')
    .refine((v) => Number(v) > 0, 'Price must be greater than zero'),
  stock: z
    .string()
    .trim()
    .refine((v) => /^\d+$/.test(v), 'Stock must be a whole number >= 0'),
  weightKg: z
    .string()
    .trim()
    .refine((v) => v === '' || WEIGHT_PATTERN.test(v), 'Weight must be a number >= 0 with up to 3 decimals')
})

export type ProductFormValues = z.infer<typeof productFormSchema>
