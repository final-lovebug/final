import { useAuthStore } from '../stores/authStore'
import { API_BASE_URL } from '../config/env'

// 공용 http client(frontend/docs/ARCHITECTURE.md "shared/api — 공용 http client, 인증 헤더/401
// 처리 등 통신 공통 로직"). Authorization 헤더 부착 + access token 만료(AUTH_TOKEN_EXPIRED) 시
// /api/auth/refresh 자동 재시도를 여기서 한 번만 처리하면, features/{domain}/api의 각 함수는
// 매번 이 로직을 반복하지 않는다(frontend/CLAUDE.md 백엔드 연동 체크리스트 4번).
//
// refresh token은 HttpOnly 쿠키라 JS에서 못 읽는다 — credentials: 'include'로 브라우저가
// 자동으로 실어 보내게 한다(docs/API.md "콜백 및 토큰 교환" 쿠키 속성 참고).

export interface ApiErrorBody {
  code: string
  message: string
  errors?: { field: string; message: string }[]
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly errors?: { field: string; message: string }[]

  constructor(status: number, code: string, message: string, errors?: { field: string; message: string }[]) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.errors = errors
  }
}

interface RequestOptions {
  body?: unknown
  /** refresh 재시도 자체가 다시 재시도를 타지 않도록 막는 내부 플래그. */
  skipAuthRetry?: boolean
}

async function rawRequest(method: string, path: string, options: RequestOptions, accessToken: string | null): Promise<Response> {
  const headers: Record<string, string> = {}
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`
  }

  return fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    credentials: 'include',
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  })
}

async function parseBody<T>(response: Response): Promise<T | undefined> {
  const text = await response.text()
  return text ? (JSON.parse(text) as T) : undefined
}

let refreshPromise: Promise<string> | null = null

// 동시에 여러 요청이 401을 받아도 /api/auth/refresh는 한 번만 호출한다(요청끼리 promise 공유).
async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const response = await rawRequest('POST', '/api/auth/refresh', {}, null)
      if (!response.ok) {
        useAuthStore.getState().logout()
        const body = await parseBody<ApiErrorBody>(response).catch(() => undefined)
        throw new ApiError(response.status, body?.code ?? 'AUTH_TOKEN_INVALID', body?.message ?? '재로그인이 필요합니다.')
      }
      const body = await parseBody<{ accessToken: string }>(response)
      if (!body) {
        throw new ApiError(response.status, 'AUTH_TOKEN_INVALID', '재발급 응답이 비어 있습니다.')
      }
      useAuthStore.getState().setAccessToken(body.accessToken)
      return body.accessToken
    })().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const accessToken = useAuthStore.getState().accessToken
  let response = await rawRequest(method, path, options, accessToken)

  if (response.status === 401 && !options.skipAuthRetry) {
    const body = await parseBody<ApiErrorBody>(response.clone()).catch(() => undefined)
    if (body?.code === 'AUTH_TOKEN_EXPIRED') {
      try {
        const newAccessToken = await refreshAccessToken()
        response = await rawRequest(method, path, { ...options, skipAuthRetry: true }, newAccessToken)
      } catch {
        // refreshAccessToken이 이미 로그아웃 처리했다 — 아래 !response.ok 분기로 원래 401을 던진다.
      }
    }
  }

  if (!response.ok) {
    const body = await parseBody<ApiErrorBody>(response).catch(() => undefined)
    throw new ApiError(response.status, body?.code ?? 'UNKNOWN_ERROR', body?.message ?? '요청 처리 중 오류가 발생했습니다.', body?.errors)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await parseBody<T>(response)) as T
}

export const httpClient = {
  get: <T>(path: string): Promise<T> => request<T>('GET', path),
  post: <T>(path: string, body?: unknown): Promise<T> => request<T>('POST', path, { body }),
  patch: <T>(path: string, body?: unknown): Promise<T> => request<T>('PATCH', path, { body }),
  delete: <T>(path: string): Promise<T> => request<T>('DELETE', path),
}
