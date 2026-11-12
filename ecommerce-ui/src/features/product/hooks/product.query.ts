import {
  type DefaultError,
  type InfiniteData,
  keepPreviousData,
  type QueryFunctionContext,
  skipToken,
  useInfiniteQuery,
  useQuery,
  type UseInfiniteQueryOptions,
  type UseQueryOptions
} from '@tanstack/react-query'
import { getProductById, searchProducts } from '../api/product.api'
import type {
  PageableResponse,
  ProductDto,
  ProductFilterParams,
  ProductSearchDto,
  UUID,
  WithETag
} from '../api/product.dto'

type ProductsPage = PageableResponse<ProductSearchDto>
type InfiniteProductsFilters = Omit<ProductFilterParams, 'page'>

export const productQueryKeys = {
  all: ['products'] as const,
  lists: () => [...productQueryKeys.all, 'list'] as const,
  list: (filters: ProductFilterParams) => [...productQueryKeys.lists(), filters] as const,
  infinite: (filters: InfiniteProductsFilters) => [...productQueryKeys.lists(), 'infinite', filters] as const,
  details: () => [...productQueryKeys.all, 'detail'] as const,
  detail: (id: UUID) => [...productQueryKeys.details(), id] as const
}

const fetchProductWithETag = async (
  id: UUID,
  { client, queryKey, signal }: QueryFunctionContext<ReturnType<typeof productQueryKeys.detail>>
): Promise<WithETag<ProductDto>> => {
  const cached = client.getQueryData<WithETag<ProductDto>>(queryKey)
  const result = await getProductById(id, { ifNoneMatch: cached?.etag ?? undefined, signal })
  if (!result.notModified) return { data: result.data, etag: result.etag }
  if (!cached) throw new Error(`Received 304 Not Modified for product ${id} without a cached version`)
  return cached
}

export type ProductByIdQueryOptions = Omit<
  UseQueryOptions<WithETag<ProductDto>, DefaultError, ProductDto, ReturnType<typeof productQueryKeys.detail>>,
  'queryKey' | 'queryFn' | 'select'
>

export const useProductByIdQuery = (id: UUID | null | undefined, options: ProductByIdQueryOptions = {}) =>
  useQuery({
    queryKey: productQueryKeys.detail(id as UUID),
    queryFn: id ? (context) => fetchProductWithETag(id, context) : skipToken,
    select: (result) => result.data,
    ...options
  })

export type ProductsQueryOptions = Omit<
  UseQueryOptions<ProductsPage, DefaultError, ProductsPage, ReturnType<typeof productQueryKeys.list>>,
  'queryKey' | 'queryFn'
>

export const useProductsQuery = (filters: ProductFilterParams = {}, options: ProductsQueryOptions = {}) => {
  const query = useQuery({
    queryKey: productQueryKeys.list(filters),
    queryFn: ({ signal }) => searchProducts(filters, signal),
    placeholderData: keepPreviousData,
    ...options
  })

  return {
    ...query,
    products: query.data?.content ?? [],
    pageDetails: query.data?.pageDetails,
    filters
  }
}

export type ProductsInfiniteQueryOptions = Omit<
  UseInfiniteQueryOptions<
    ProductsPage,
    DefaultError,
    InfiniteData<ProductsPage, number>,
    ReturnType<typeof productQueryKeys.infinite>,
    number
  >,
  'queryKey' | 'queryFn' | 'initialPageParam' | 'getNextPageParam'
>

export const useProductsInfiniteQuery = (
  filters: InfiniteProductsFilters = {},
  options: ProductsInfiniteQueryOptions = {}
) => {
  const { data: rawData, ...query } = useInfiniteQuery({
    queryKey: productQueryKeys.infinite(filters),
    queryFn: ({ pageParam, signal }) => searchProducts({ ...filters, page: pageParam }, signal),
    initialPageParam: 0,
    getNextPageParam: ({ pageDetails }) =>
      pageDetails.isLast || pageDetails.page + 1 >= pageDetails.totalPages ? undefined : pageDetails.page + 1,
    ...options
  })

  return {
    ...query,
    rawData,
    products: rawData?.pages.flatMap((page) => page.content) ?? [],
    totalElements: rawData?.pages.at(-1)?.pageDetails.totalElements ?? 0,
    filters
  }
}
