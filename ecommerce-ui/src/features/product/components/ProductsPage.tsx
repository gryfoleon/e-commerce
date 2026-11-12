import Alert from '@mui/material/Alert'
import AlertTitle from '@mui/material/AlertTitle'
import Snackbar from '@mui/material/Snackbar'
import Box from '@mui/material/Box'
import { useState } from 'react'
import { getApiError } from '../../../core/http/api-client'
import { Loader } from '../../../shared/components/Loader'
import type { CsvImportReport, UUID } from '../api/product.dto'
import {
  useCreateProductMutation,
  useDeleteProductMutation,
  useImportProductsCsvMutation,
  useUpdateProductMutation
} from '../hooks/product.mutations'
import { useProductByIdQuery, useProductsQuery } from '../hooks/product.query'
import { useProductFilterStore } from '../stores/product-filter.store'
import type { Product, ProductInput } from '../types/product.types'
import { toProductFilterParams } from '../util/product.utils'
import { ProductDeleteDialog } from './ProductDeleteDialog'
import { ProductDetailDialog } from './ProductDetailDialog'
import { ProductFilterBar } from './ProductFilterBar'
import { ProductFormDialog, type ProductFormServerErrors } from './ProductFormDialog'
import { ProductImportResultDialog } from './ProductImportResultDialog'
import { ProductTable } from './ProductTable'

type Notice = { severity: 'success' | 'error'; message: string }

type ScreenError = { title: string; details: string[] }

const MAX_ERROR_DETAILS = 10

const FORM_FIELDS = new Set<string>(['sku', 'name', 'description', 'category', 'price', 'stock', 'weightKg'])

const toFormErrors = (error: unknown, fallback: string): { fields: ProductFormServerErrors; message: string } => {
  const apiError = getApiError(error)
  if (!apiError) return { fields: {}, message: fallback }
  const fields: ProductFormServerErrors = {}
  const general: string[] = []
  for (const detail of apiError.errorDetails ?? []) {
    if (detail.field && FORM_FIELDS.has(detail.field)) fields[detail.field as keyof ProductFormServerErrors] ??= detail.message
    else if (detail.message) general.push(detail.message)
  }
  return { fields, message: [apiError.error || fallback, ...general].join(' ') }
}

type PendingCreate = { payload: string; idempotencyKey: UUID }

const toScreenError = (error: unknown, fallback: string): ScreenError => {
  const apiError = getApiError(error)
  if (!apiError) return { title: fallback, details: [] }
  const details = (apiError.errorDetails ?? []).map((detail) => detail.message).filter(Boolean)
  return { title: apiError.error || fallback, details }
}


interface ProductsPageProps {
  readonly onAddToCart?: (product: Product) => void
  readonly canAddToCart?: (product: Product) => boolean
}

export const ProductsPage = ({ onAddToCart, canAddToCart }: ProductsPageProps = {}) => {
  const { applied, minStock, page, pageSize, setPage, setPageSize } = useProductFilterStore()
  const { products, pageDetails, isFetching, isError } = useProductsQuery(
    toProductFilterParams({ applied, minStock, page, pageSize })
  )

  const [viewing, setViewing] = useState<Product | null>(null)
  const [deleting, setDeleting] = useState<Product | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Product | null>(null)
  const [notice, setNotice] = useState<Notice | null>(null)
  const [importReport, setImportReport] = useState<CsvImportReport | null>(null)
  const [importError, setImportError] = useState<ScreenError | null>(null)
  const importMutation = useImportProductsCsvMutation()
  const deleteMutation = useDeleteProductMutation()
  const createMutation = useCreateProductMutation()
  const updateMutation = useUpdateProductMutation()
  const editQuery = useProductByIdQuery(formOpen && editing ? (editing.id as UUID) : null, { staleTime: 0 })
  const [formErrors, setFormErrors] = useState<ProductFormServerErrors>({})
  const [pendingCreate, setPendingCreate] = useState<PendingCreate | null>(null)

  const closeForm = () => {
    setFormOpen(false)
    setEditing(null)
    setFormErrors({})
    setPendingCreate(null)
  }

  const handleCreate = (input: ProductInput) => {
    const payload = JSON.stringify(input)
    const idempotencyKey =
      pendingCreate?.payload === payload ? pendingCreate.idempotencyKey : crypto.randomUUID()
    setPendingCreate({ payload, idempotencyKey })
    setFormErrors({})
    createMutation.mutate(
      { product: input, idempotencyKey },
      {
        onSuccess: ({ data }) => {
          closeForm()
          setNotice({ severity: 'success', message: `Product ${data.sku} created` })
        },
        onError: (error) => {
          const { fields, message } = toFormErrors(error, 'Could not create the product.')
          setFormErrors(fields)
          setNotice({ severity: 'error', message })
        }
      }
    )
  }

  const handleUpdate = (product: Product, input: ProductInput) => {
    setFormErrors({})
    updateMutation.mutate(
      { idOrSku: product.id, product: input },
      {
        onSuccess: ({ data }) => {
          closeForm()
          setNotice({ severity: 'success', message: `Product ${data.sku} updated` })
        },
        onError: (error) => {
          if (getApiError(error)?.statusCode === 409) {
            setNotice({
              severity: 'error',
              message: 'This product was modified by someone else. The latest data was loaded; review it and save again.'
            })
            return
          }
          const { fields, message } = toFormErrors(error, 'Could not update the product.')
          setFormErrors(fields)
          setNotice({ severity: 'error', message })
        }
      }
    )
  }

  const handleSubmit = (input: ProductInput) => {
    if (editing) handleUpdate(editing, input)
    else handleCreate(input)
  }

  const handleDelete = () => {
    if (!deleting) return
    deleteMutation.mutate(
      { idOrSku: deleting.id },
      {
        onSuccess: () => {
          if (products.length === 1 && page > 0) setPage(page - 1)
          setNotice({ severity: 'success', message: `Product ${deleting.sku} deleted` })
          setDeleting(null)
        },
        onError: (error) => {
          const { title, details } = toScreenError(error, 'Could not delete the product.')
          setNotice({ severity: 'error', message: [title, ...details].join(' ') })
        }
      }
    )
  }

  const handleImport = (file: File) => {
    setImportError(null)
    importMutation.mutate(
      { file },
      {
        onSuccess: ({ data }) => setImportReport(data),
        onError: (error) => setImportError(toScreenError(error, 'The CSV file could not be imported.'))
      }
    )
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <ProductFilterBar
        onAddProduct={() => {
          setEditing(null)
          setFormOpen(true)
        }}
        onImportCsv={handleImport}
      />
      {importError && (
        <Alert severity='error' onClose={() => setImportError(null)}>
          <AlertTitle>{importError.title}</AlertTitle>
          {importError.details.length > 0 && (
            <Box component='ul' sx={{ m: 0, pl: 2 }}>
              {importError.details.slice(0, MAX_ERROR_DETAILS).map((detail, index) => (
                <li key={index}>{detail}</li>
              ))}
            </Box>
          )}
          {importError.details.length > MAX_ERROR_DETAILS &&
            `…and ${importError.details.length - MAX_ERROR_DETAILS} more errors.`}
        </Alert>
      )}
      {isError && <Alert severity='error'>Could not load products.</Alert>}
      <ProductTable
        products={products}
        total={pageDetails?.totalElements ?? 0}
        page={page}
        pageSize={pageSize}
        isLoading={isFetching}
        onPageChange={setPage}
        onPageSizeChange={setPageSize}
        onView={setViewing}
        onEdit={(p) => {
          setEditing(p)
          setFormOpen(true)
        }}
        onDelete={setDeleting}
        onAddToCart={onAddToCart}
        canAddToCart={canAddToCart}
      />
      <ProductDetailDialog product={viewing} onClose={() => setViewing(null)} />
      <ProductFormDialog
        open={formOpen}
        mode={editing ? 'edit' : 'create'}
        product={editing ? (editQuery.data ?? null) : null}
        isLoading={editing !== null && editQuery.isPending}
        loadError={editing !== null && editQuery.isError ? 'Could not load the product.' : null}
        isSubmitting={createMutation.isPending || updateMutation.isPending}
        serverErrors={formErrors}
        onClose={closeForm}
        onSubmit={handleSubmit}
      />
      <ProductDeleteDialog product={deleting} isDeleting={deleteMutation.isPending} onClose={() => setDeleting(null)} onConfirm={handleDelete} />
      <ProductImportResultDialog report={importReport} onClose={() => setImportReport(null)} />
      {importMutation.isPending && <Loader fullScreen label='Importing products' />}
      <Snackbar open={notice !== null} autoHideDuration={4000} onClose={() => setNotice(null)}>
        {notice ? <Alert severity={notice.severity}>{notice.message}</Alert> : undefined}
      </Snackbar>
    </Box>
  )
}
