import * as Y from 'yjs'
import { WebsocketProvider } from 'y-websocket'
import { IndexeddbPersistence } from 'y-indexeddb'
import type { UserPresence } from '@/models'

export interface YjsProviderOptions {
  documentId: string
  userPresence: UserPresence['user']
}

export interface YjsContext {
  ydoc: Y.Doc
  wsProvider: WebsocketProvider
  idbProvider: IndexeddbPersistence
  awareness: WebsocketProvider['awareness']
  destroy: () => void
}

export function createYjsProvider({ documentId, userPresence }: YjsProviderOptions): YjsContext {
  const ydoc = new Y.Doc()

  const wsProvider = new WebsocketProvider(
    `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws/documents`,
    documentId,
    ydoc
  )

  const idbProvider = new IndexeddbPersistence(`erdsketch-${documentId}`, ydoc)

  wsProvider.awareness.setLocalStateField('user', userPresence)

  const destroy = () => {
    wsProvider.destroy()
    idbProvider.destroy()
    ydoc.destroy()
  }

  return { ydoc, wsProvider, idbProvider, awareness: wsProvider.awareness, destroy }
}

// Yjs document structure helpers
export function getTablesMap(ydoc: Y.Doc): Y.Map<Y.Map<unknown>> {
  return ydoc.getMap('tables') as Y.Map<Y.Map<unknown>>
}

export function getRelationshipsMap(ydoc: Y.Doc): Y.Map<Y.Map<unknown>> {
  return ydoc.getMap('relationships') as Y.Map<Y.Map<unknown>>
}

export function getNotesMap(ydoc: Y.Doc): Y.Map<Y.Map<unknown>> {
  return ydoc.getMap('notes') as Y.Map<Y.Map<unknown>>
}
