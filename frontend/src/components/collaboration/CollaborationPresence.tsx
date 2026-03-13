import { useEffect, useState } from 'react'
import type { Awareness } from 'y-protocols/awareness'
import type { UserPresence } from '@/models'

interface Props {
  awareness: Awareness
}

export default function CollaborationPresence({ awareness }: Props) {
  const [presences, setPresences] = useState<UserPresence[]>([])

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
        <div
          key={presence.clientId}
          className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold shadow-md border-2 border-white"
          style={{ backgroundColor: presence.user?.color ?? '#3b82f6' }}
          title={presence.user?.name ?? '사용자'}
        >
          {presence.user?.name?.[0]?.toUpperCase() ?? '?'}
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
