import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { workspaceApi } from '@/api/workspace'
import { projectApi } from '@/api/project'
import { documentApi } from '@/api/document'
import { useAuthStore } from '@/store/authStore'
import type { Workspace, Project, DbType, WorkspaceRole } from '@/models'

export default function DashboardPage() {
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()
  const qc = useQueryClient()
  const [selectedWorkspace, setSelectedWorkspace] = useState<Workspace | null>(null)
  const [showCreateWorkspace, setShowCreateWorkspace] = useState(false)
  const [showCreateProject, setShowCreateProject] = useState(false)
  const [showInviteMember, setShowInviteMember] = useState(false)

  const { data: workspaces = [] } = useQuery({
    queryKey: ['workspaces'],
    queryFn: workspaceApi.list,
  })

  const { data: projects = [] } = useQuery({
    queryKey: ['projects', selectedWorkspace?.id],
    queryFn: () => projectApi.list(selectedWorkspace!.id),
    enabled: !!selectedWorkspace,
  })

  const { data: members = [] } = useQuery({
    queryKey: ['workspace-members', selectedWorkspace?.id],
    queryFn: () => workspaceApi.getMembers(selectedWorkspace!.id),
    enabled: !!selectedWorkspace,
  })

  const myRole = members.find((m) => m.userId === user?.id)?.role
  const canInvite = myRole === 'OWNER' || myRole === 'ADMIN'

  const createWorkspaceMutation = useMutation({
    mutationFn: workspaceApi.create,
    onSuccess: (ws) => {
      qc.invalidateQueries({ queryKey: ['workspaces'] })
      setSelectedWorkspace(ws)
      setShowCreateWorkspace(false)
      toast.success('워크스페이스가 생성되었습니다.')
    },
  })

  const createProjectMutation = useMutation({
    mutationFn: ({ name, targetDbType }: { name: string; targetDbType: DbType }) =>
      projectApi.create(selectedWorkspace!.id, { name, targetDbType }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['projects', selectedWorkspace?.id] })
      setShowCreateProject(false)
      toast.success('프로젝트가 생성되었습니다.')
    },
  })

  const inviteMemberMutation = useMutation({
    mutationFn: ({ email, role }: { email: string; role: WorkspaceRole }) =>
      workspaceApi.inviteMember(selectedWorkspace!.id, { email, role }),
    onSuccess: () => {
      setShowInviteMember(false)
      toast.success('멤버를 초대했습니다.')
    },
    onError: () => toast.error('초대에 실패했습니다. 이메일을 확인해주세요.'),
  })

  const openDocument = async (project: Project) => {
    const docs = await documentApi.list(project.id)
    if (docs.length > 0) {
      navigate(`/workspaces/${selectedWorkspace?.id}/projects/${project.id}/documents/${docs[0].id}`)
    } else {
      const doc = await documentApi.create(project.id, { name: 'Main ERD' })
      navigate(`/workspaces/${selectedWorkspace?.id}/projects/${project.id}/documents/${doc.id}`)
    }
  }

  const handleLogout = () => {
    logout()
    qc.clear()
  }

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* Sidebar */}
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
        <div className="p-4 border-b border-gray-200">
          <h1 className="text-xl font-bold text-primary-600">ErdSketch</h1>
        </div>
        <div className="p-4 flex-1 overflow-y-auto">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-gray-500 uppercase">워크스페이스</span>
            <button
              onClick={() => setShowCreateWorkspace(true)}
              className="text-gray-400 hover:text-primary-600 text-lg leading-none"
              title="새 워크스페이스"
            >+</button>
          </div>
          <ul className="space-y-1">
            {workspaces.map((ws) => (
              <li key={ws.id}>
                <button
                  onClick={() => setSelectedWorkspace(ws)}
                  className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${
                    selectedWorkspace?.id === ws.id
                      ? 'bg-primary-50 text-primary-700 font-medium'
                      : 'text-gray-700 hover:bg-gray-100'
                  }`}
                >
                  {ws.name}
                </button>
              </li>
            ))}
          </ul>
        </div>
        <div className="p-4 border-t border-gray-200">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 font-medium text-sm">
              {user?.displayName?.[0]?.toUpperCase()}
            </div>
            <span className="text-sm text-gray-700 flex-1 truncate">{user?.displayName}</span>
            <button onClick={handleLogout} className="text-gray-400 hover:text-red-500 text-sm">
              로그아웃
            </button>
          </div>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 p-8 overflow-y-auto">
        {selectedWorkspace ? (
          <>
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold text-gray-900">{selectedWorkspace.name}</h2>
              <div className="flex items-center gap-2">
                {canInvite && (
                  <button
                    onClick={() => setShowInviteMember(true)}
                    className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 font-medium text-sm flex items-center gap-1.5"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                    </svg>
                    멤버 초대
                  </button>
                )}
                <button
                  onClick={() => setShowCreateProject(true)}
                  className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 font-medium text-sm"
                >
                  + 새 프로젝트
                </button>
              </div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {projects.map((project) => (
                <div
                  key={project.id}
                  onClick={() => openDocument(project)}
                  className="bg-white border border-gray-200 rounded-xl p-5 hover:shadow-md hover:border-primary-200 cursor-pointer transition-all"
                >
                  <h3 className="font-semibold text-gray-900 mb-1">{project.name}</h3>
                  {project.description && (
                    <p className="text-sm text-gray-500 mb-3 line-clamp-2">{project.description}</p>
                  )}
                  <span className="inline-block px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded">
                    {project.targetDbType}
                  </span>
                </div>
              ))}
            </div>
          </>
        ) : (
          <div className="flex flex-col items-center justify-center h-96 text-center">
            <p className="text-gray-400 text-lg mb-4">워크스페이스를 선택하거나 새로 만드세요</p>
            <button
              onClick={() => setShowCreateWorkspace(true)}
              className="px-6 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 font-medium"
            >
              워크스페이스 만들기
            </button>
          </div>
        )}
      </main>

      {showCreateWorkspace && (
        <CreateWorkspaceModal
          onClose={() => setShowCreateWorkspace(false)}
          onSubmit={(data) => createWorkspaceMutation.mutate(data)}
          isPending={createWorkspaceMutation.isPending}
        />
      )}

      {showCreateProject && selectedWorkspace && (
        <CreateProjectModal
          onClose={() => setShowCreateProject(false)}
          onSubmit={(data) => createProjectMutation.mutate(data)}
          isPending={createProjectMutation.isPending}
        />
      )}

      {showInviteMember && selectedWorkspace && (
        <InviteMemberModal
          workspaceName={selectedWorkspace.name}
          onClose={() => setShowInviteMember(false)}
          onSubmit={(data) => inviteMemberMutation.mutate(data)}
          isPending={inviteMemberMutation.isPending}
        />
      )}
    </div>
  )
}

function CreateWorkspaceModal({
  onClose, onSubmit, isPending,
}: { onClose: () => void; onSubmit: (data: { name: string; slug: string }) => void; isPending: boolean }) {
  const [name, setName] = useState('')
  const [slugEdited, setSlugEdited] = useState(false)
  const [slug, setSlug] = useState('')

  const derivedSlug = name.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '')
  const displaySlug = slugEdited ? slug : derivedSlug
  const slugError = displaySlug.length > 0 && !/^[a-z0-9][a-z0-9-]*$/.test(displaySlug)

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
        <h3 className="text-lg font-bold mb-4">새 워크스페이스</h3>
        <form onSubmit={(e) => { e.preventDefault(); if (!slugError && displaySlug) onSubmit({ name, slug: displaySlug }) }} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">워크스페이스 이름</label>
            <input
              type="text" required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={name} onChange={(e) => setName(e.target.value)}
              placeholder="예: My Team"
            />
            <p className="text-xs text-gray-400 mt-1">한글, 영문, 숫자, 공백 모두 사용 가능합니다.</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">슬러그 (URL 식별자)</label>
            <input
              type="text"
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 font-mono text-sm ${slugError ? 'border-red-400' : 'border-gray-300'}`}
              value={displaySlug}
              onChange={(e) => { setSlugEdited(true); setSlug(e.target.value) }}
              placeholder="my-team"
            />
            {slugError ? (
              <p className="text-xs text-red-500 mt-1">영문 소문자, 숫자, 하이픈(-)만 사용 가능하며 영문/숫자로 시작해야 합니다.</p>
            ) : (
              <p className="text-xs text-gray-400 mt-1">영문 소문자, 숫자, 하이픈(-)만 허용됩니다. 이름 입력 시 자동 생성됩니다.</p>
            )}
          </div>
          <div className="flex gap-2 justify-end">
            <button type="button" onClick={onClose} className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg">취소</button>
            <button type="submit" disabled={isPending || slugError || !displaySlug} className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50">
              {isPending ? '생성 중...' : '생성'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function CreateProjectModal({
  onClose, onSubmit, isPending,
}: { onClose: () => void; onSubmit: (data: { name: string; targetDbType: DbType }) => void; isPending: boolean }) {
  const [name, setName] = useState('')
  const [targetDbType, setTargetDbType] = useState<DbType>('POSTGRESQL')

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
        <h3 className="text-lg font-bold mb-4">새 프로젝트</h3>
        <form onSubmit={(e) => { e.preventDefault(); onSubmit({ name, targetDbType }) }} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">프로젝트 이름</label>
            <input
              type="text" required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={name} onChange={(e) => setName(e.target.value)}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">대상 DB</label>
            <select
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={targetDbType}
              onChange={(e) => setTargetDbType(e.target.value as DbType)}
            >
              <option value="POSTGRESQL">PostgreSQL</option>
              <option value="MYSQL">MySQL</option>
              <option value="ORACLE">Oracle</option>
              <option value="MSSQL">MSSQL</option>
            </select>
          </div>
          <div className="flex gap-2 justify-end">
            <button type="button" onClick={onClose} className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg">취소</button>
            <button type="submit" disabled={isPending} className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50">
              {isPending ? '생성 중...' : '생성'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function InviteMemberModal({
  workspaceName, onClose, onSubmit, isPending,
}: {
  workspaceName: string
  onClose: () => void
  onSubmit: (data: { email: string; role: WorkspaceRole }) => void
  isPending: boolean
}) {
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<WorkspaceRole>('MEMBER')

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl">
        <h3 className="text-lg font-bold mb-1">멤버 초대</h3>
        <p className="text-sm text-gray-500 mb-3">{workspaceName} 워크스페이스에 초대합니다.</p>
        <div className="bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 mb-4">
          <p className="text-xs text-amber-700">ErdSketch에 이미 가입된 계정의 이메일만 추가할 수 있습니다.</p>
        </div>
        <form onSubmit={(e) => { e.preventDefault(); onSubmit({ email, role }) }} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">이메일</label>
            <input
              type="email" required
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={email} onChange={(e) => setEmail(e.target.value)}
              placeholder="colleague@example.com"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">권한</label>
            <select
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={role}
              onChange={(e) => setRole(e.target.value as WorkspaceRole)}
            >
              <option value="MEMBER">멤버 — 편집 가능</option>
              <option value="VIEWER">뷰어 — 읽기 전용</option>
              <option value="ADMIN">관리자 — 멤버 관리 가능</option>
            </select>
          </div>
          <div className="flex gap-2 justify-end">
            <button type="button" onClick={onClose} className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg">취소</button>
            <button type="submit" disabled={isPending} className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50">
              {isPending ? '초대 중...' : '초대 보내기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
