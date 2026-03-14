interface Props {
  followingName: string
  onStop: () => void
}

export default function FollowModeBanner({ followingName, onStop }: Props) {
  return (
    <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-20 flex items-center gap-3 px-4 py-2 bg-blue-600 text-white rounded-full shadow-lg export-ignore">
      <span className="text-sm font-medium">{followingName}님을 따라가는 중</span>
      <button
        onClick={onStop}
        className="text-xs bg-white/20 hover:bg-white/30 px-2 py-0.5 rounded-full"
      >
        해제
      </button>
    </div>
  )
}
