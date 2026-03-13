import { useCallback, useEffect, useRef, useState } from 'react'
import type { Awareness } from 'y-protocols/awareness'
import toast from 'react-hot-toast'

export interface Viewport {
  x: number
  y: number
  zoom: number
}

export interface FollowModeState {
  followingUserId: string | null
  startFollowing: (userId: string) => void
  stopFollowing: () => void
}

/**
 * useFollowMode — Yjs awareness 기반 뷰포트 팔로우 모드
 * @param awareness Yjs awareness 객체
 * @param onViewportChange 뷰포트 변경 콜백 (React Flow의 setViewport 등)
 */
export function useFollowMode(
  awareness: Awareness | null,
  onViewportChange: (viewport: Viewport) => void,
): FollowModeState {
  const [followingUserId, setFollowingUserId] = useState<string | null>(null)
  const followingRef = useRef<string | null>(null)

  // followingRef를 state와 동기화 (클로저에서 최신 값 참조용)
  useEffect(() => {
    followingRef.current = followingUserId
  }, [followingUserId])

  const startFollowing = useCallback((userId: string) => {
    setFollowingUserId(userId)
    followingRef.current = userId
  }, [])

  const stopFollowing = useCallback(() => {
    setFollowingUserId(null)
    followingRef.current = null
  }, [])

  useEffect(() => {
    if (!awareness || !followingUserId) return

    const handleChange = () => {
      const targetId = followingRef.current
      if (!targetId) return

      // awareness 상태에서 팔로우 대상 찾기
      let found = false
      awareness.getStates().forEach((state, _clientId) => {
        if (state?.user?.id === targetId) {
          found = true
          const viewport = state?.viewport as Viewport | undefined
          if (viewport) {
            onViewportChange(viewport)
          }
        }
      })

      // 팔로우 대상이 오프라인이면 팔로우 해제
      if (!found) {
        stopFollowing()
        toast('팔로우 중인 사용자가 오프라인 상태입니다.', { icon: '⚠️' })
      }
    }

    awareness.on('change', handleChange)
    return () => {
      awareness.off('change', handleChange)
    }
  }, [awareness, followingUserId, onViewportChange, stopFollowing])

  return { followingUserId, startFollowing, stopFollowing }
}
