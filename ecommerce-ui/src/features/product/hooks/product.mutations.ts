import { type QueryClient, useMutation, useQueryClient } from '@tanstack/react-query'
import { createProduct, deleteProduct, importProductsCsv, updateProduct } from '../api/product.api'
import type { ProductCreateRequest, ProductDto, ProductUpdateRequest, UUID, WithETag } from '../api/product.dto'
import { productQueryKeys } from './product.query'

export const productMutationKeys = {
  all: ['products', 'mutation'] as const,
  create: () => [...productMutationKeys.all, 'create'] as const,
  update: () => [...productMutationKeys.all, 'update'] as const,
  delete: () => [...productMutationKeys.all, 'delete'] as const,
  importCsv: () => [...productMutationKeys.all, 'import-csv'] as const
}

type CachedProduct = WithETag<ProductDto>

const matchesProduct = (cached: CachedProduct | undefined, idOrSku: UUID | string): boolean =>
  cached?.data.id === idOrSku || cached?.data.sku === idOrSku

const findCachedProduct = (client: QueryClient, idOrSku: UUID | string): CachedProduct | undefined =>
  client
    .getQueriesData<CachedProduct>({ queryKey: productQueryKeys.details() })
    .map(([, cached]) => cached)
    .find((cached) => matchesProduct(cached, idOrSku))

const setCachedProduct = (client: QueryClient, product: CachedProduct) =>
  client.setQueryData<CachedProduct>(productQueryKeys.detail(product.data.id), product)

const invalidateProductLists = (client: QueryClient) =>
  client.invalidateQueries({ queryKey: productQueryKeys.lists() })

export type CreateProductVariables = {
  product: ProductCreateRequest
  idempotencyKey?: UUID
  signal?: AbortSignal
}

export const useCreateProductMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationKey: productMutationKeys.create(),
    mutationFn: ({ product, idempotencyKey, signal }: CreateProductVariables) =>
      createProduct(product, { idempotencyKey, signal }),
    onSuccess: ({ data, etag }) => {
      setCachedProduct(queryClient, { data, etag })
      return invalidateProductLists(queryClient)
    }
  })
}

export type UpdateProductVariables = {
  idOrSku: UUID | string
  product: ProductUpdateRequest
  ifMatch?: string
  signal?: AbortSignal
}

export const useUpdateProductMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationKey: productMutationKeys.update(),
    mutationFn: ({ idOrSku, product, ifMatch, signal }: UpdateProductVariables) =>
      updateProduct(idOrSku, product, {
        ifMatch: ifMatch ?? findCachedProduct(queryClient, idOrSku)?.etag ?? undefined,
        signal
      }),
    onSuccess: (result) => {
      setCachedProduct(queryClient, result)
      return invalidateProductLists(queryClient)
    },
    onError: (_error, { idOrSku }) =>
      queryClient.invalidateQueries({
        queryKey: productQueryKeys.details(),
        predicate: (query) => matchesProduct(query.state.data as CachedProduct | undefined, idOrSku)
      })
  })
}

export type DeleteProductVariables = {
  idOrSku: UUID | string
  signal?: AbortSignal
}

export const useDeleteProductMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationKey: productMutationKeys.delete(),
    mutationFn: ({ idOrSku, signal }: DeleteProductVariables) => deleteProduct(idOrSku, signal),
    onSuccess: (_data, { idOrSku }) => {
      queryClient.removeQueries({
        queryKey: productQueryKeys.details(),
        predicate: (query) =>
          query.queryKey.at(-1) === idOrSku || matchesProduct(query.state.data as CachedProduct | undefined, idOrSku)
      })
      return invalidateProductLists(queryClient)
    }
  })
}

export type ImportProductsCsvVariables = {
  file: Blob
  idempotencyKey?: UUID
  signal?: AbortSignal
}

export const useImportProductsCsvMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationKey: productMutationKeys.importCsv(),
    mutationFn: ({ file, idempotencyKey, signal }: ImportProductsCsvVariables) =>
      importProductsCsv(file, { idempotencyKey, signal }),
    onSettled: () => queryClient.invalidateQueries({ queryKey: productQueryKeys.all })
  })
}
