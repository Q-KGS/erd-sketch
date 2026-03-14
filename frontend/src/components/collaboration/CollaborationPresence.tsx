import { useEffect, useState } from 'react'
import type { Awareness } from 'y-protocols/awareness'
import type { UserPresence } from '@/models'

interface Props {
  awareness: Awareness
  onFollowUser?: (userId: string, name: string) => void
}

export default function CollaborationPresence({ awareness, onFollowUser }: Props) {
  const [presences, setPresences] = useState<UserPresence[]>([])
  const [popover, setPopover] = useState<string | null>(null)

  useEffect(() => {
    const updatePresences = () => {
      const states = Array.from(awareness.getStates().entries())
        .filter(([clientId]) => clientId !== awareness.clientID)
        .map(([clientId, state]) => ({
          clientId,
          user: (state as { user: UserPresence['user'] }).user,
        }))
        .filter((p) => p.user)
      setPresences(states)
    }

    updatePresences()
    awareness.on('change', updatePresences)
    return () => awareness.off('change', updatePresences)
  }, [awareness])

  if (presences.length === 0) return null

  return (
    <div className="absolute top-2 right-2 z-10 flex items-center gap-1 export-ignore">
      {presences.slice(0, 5).map((presence) => (
        <div key={presence.clientId} className="relative">
          <button
            className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold shadow-md border-2 border-white"
            style={{ backgroundColor: presence.user?.color ?? '#3b82f6' }}
            title={presence.user?.name ?? '사용자'}
            onClick={() => setPopover(popover === presence.user?.id ? null : presence.user?.id ?? null)}
          >
            {presence.user?.name?.[0]?.toUpperCase() ?? '?'}
          </button>
          {popover === presence.user?.id && onFollowUser && (
            <div className="absolute top-8 right-0 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg p-2 min-w-max z-20">
              <div className="text-xs text-gray-700 dark:text-gray-300 mb-2 font-medium">{presence.user?.name}</div>
              <button
                className="text-xs px-3 py-1 bg-primary-600 text-white rounded hover:bg-primary-700 w-full"
                onClick={() => {
                  onFollowUser(presence.user.id, presence.user.name)
                  setPopover(null)
                }}
              >
                따라가기
              </button>
            </div>
          )}
        </div>
      ))}
      {presences.length > 5 && (
        <div className="w-7 h-7 rounded-full bg-gray-400 flex items-center justify-center text-white text-xs font-bold shadow-md">
          +{presences.length - 5}
        </div>
      )}
    </div>
  )
}
