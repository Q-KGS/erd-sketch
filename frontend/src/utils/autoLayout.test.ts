import { describe, test, expect } from 'vitest'
import { applyAutoLayout } from './autoLayout'
import type { Node, Edge } from '@xyflow/react'

function makeNode(id: string, columns: number = 0): Node {
  return {
    id,
    type: 'table',
    position: { x: 0, y: 0 },
    data: { columns: Array(columns).fill({ id: 'c', name: 'col' }) },
  }
}

function makeEdge(source: string, target: string): Edge {
  return { id: `${source}-${target}`, source, target }
}

describe('applyAutoLayout', () => {
  // F-LAYOUT-01: 노드 없음
  test('F-LAYOUT-01: 노드 없으면 빈 배열 반환', () => {
    const result = applyAutoLayout([], [])
    expect(result).toEqual([])
  })

  // F-LAYOUT-02: 노드 1개 → position 숫자로 반환
  test('F-LAYOUT-02: 노드 1개면 position이 숫자로 반환', () => {
    const nodes = [makeNode('a')]
    const result = applyAutoLayout(nodes, [])

    expect(result).toHaveLength(1)
    expect(typeof result[0].position.x).toBe('number')
    expect(typeof result[0].position.y).toBe('number')
  })

  // F-LAYOUT-03: 연결된 2개 노드 → source가 target 좌측에 위치 (LR 방향)
  test('F-LAYOUT-03: LR 방향 정렬 시 source가 target의 좌측에 위치', () => {
    const nodes = [makeNode('source'), makeNode('target')]
    const edges = [makeEdge('source', 'target')]
    const result = applyAutoLayout(nodes, edges, 'LR')

    const srcNode = result.find((n) => n.id === 'source')!
    const tgtNode = result.find((n) => n.id === 'target')!

    expect(srcNode.position.x).toBeLessThan(tgtNode.position.x)
  })

  // F-LAYOUT-04: 고컬럼 수 테이블 → 정상 레이아웃 (에러 없음)
  test('F-LAYOUT-04: 컬럼이 많은 테이블도 에러 없이 레이아웃 적용', () => {
    const nodes = [makeNode('big', 20), makeNode('small', 2)]
    const edges = [makeEdge('big', 'small')]

    expect(() => applyAutoLayout(nodes, edges)).not.toThrow()
    const result = applyAutoLayout(nodes, edges)
    expect(result).toHaveLength(2)
  })

  // 추가: TB 방향 정렬
  test('TB 방향 정렬 시 source가 target의 상단에 위치', () => {
    const nodes = [makeNode('source'), makeNode('target')]
    const edges = [makeEdge('source', 'target')]
    const result = applyAutoLayout(nodes, edges, 'TB')

    const srcNode = result.find((n) => n.id === 'source')!
    const tgtNode = result.find((n) => n.id === 'target')!

    expect(srcNode.position.y).toBeLessThan(tgtNode.position.y)
  })

  // 추가: 노드 id 유지
  test('레이아웃 적용 후 노드 id와 data가 유지됨', () => {
    const nodes = [makeNode('a', 3), makeNode('b', 1)]
    const result = applyAutoLayout(nodes, [])

    expect(result.map((n) => n.id).sort()).toEqual(['a', 'b'])
    expect((result.find((n) => n.id === 'a')!.data as { columns: unknown[] }).columns).toHaveLength(3)
  })
})
