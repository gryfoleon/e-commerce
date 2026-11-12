import axios, { type AxiosInstance } from 'axios'

export const API_BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'

const DEFAULT_TIMEOUT_MS = 15_000

export const createApiClient = (baseURL: string = API_BASE_URL): AxiosInstance =>
  axios.create({
    baseURL,
    timeout: DEFAULT_TIMEOUT_MS,
    headers: { Accept: 'application/json' }
  })

export const apiClient = createApiClient()

export type ApiErrorDetail = {
  field?: string | null
  rejectedValue?: string | null
  message: string
}

export type ApiErrorResponse = {
  statusCode: number
  response: string
  timestamp: string
  requestDetails?: { requestPath: string; requestMethod: string }
  error: string
  errorDetails?: ApiErrorDetail[]
}

const isApiErrorResponse = (value: unknown): value is ApiErrorResponse =>
  typeof value === 'object' &&
  value !== null &&
  typeof (value as ApiErrorResponse).statusCode === 'number' &&
  typeof (value as ApiErrorResponse).error === 'string'

export const getApiError = (error: unknown): ApiErrorResponse | null => {
  if (!axios.isAxiosError(error)) return null
  const data: unknown = error.response?.data
  return isApiErrorResponse(data) ? data : null
}

export const isApiError = (error: unknown): boolean => getApiError(error) !== null
