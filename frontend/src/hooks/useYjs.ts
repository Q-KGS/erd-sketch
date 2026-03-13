import { useEffect, useRef, useState } from 'react'
import * as Y from 'yjs'
import { createYjsProvider, type YjsContext } from '@/crdt/yjsProvider'
import { useAuthStore } from '@/store/authStore'

const USER_COLORS = [
  '#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4',
  '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F',
]

function getUserColor(userId: string): string {
  const hash = userId.split('').reduce((acc, c) => acc + c.charCodeAt(0), 0)
  return USER_COLORS[hash % USER_COLORS.length]
}

export function useYjs(documentId: string | null) {
  const { user } = useAuthStore()
  const contextRef = useRef<YjsContext | null>(null)
  const [isConnected, setIsConnected] = useState(false)
  const [ydoc, setYdoc] = useState<Y.Doc | null>(null)

  useEffect(() => {
    if (!documentId || !user) return

    const ctx = createYjsProvider({
      documentId,
      userPresence: {
        id: user.id,
        name: user.displayName,
        color: getUserColor(user.id),
      },
    })

    contextRef.current = ctx
    setYdoc(ctx.ydoc)

    ctx.wsProvider.on('status', ({ status }: { status: string }) => {
      setIsConnected(status === 'connected')
    })

    return () => {
      ctx.destroy()
      contextRef.current = null
      setYdoc(null)
      setIsConnected(false)
    }
  }, [documentId, user])

  return {
    ydoc,
    awareness: contextRef.current?.awareness ?? null,
    isConnected,
  }
}
