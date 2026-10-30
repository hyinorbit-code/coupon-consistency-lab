/**
 * 개발용 시나리오 설정. 개발 패널에서 바꾸고, MSW 핸들러가 읽는다. 브라우저 localStorage에 저장된다.
 */
export type EntryOutcome = 'auto' | 'sold-out' | 'server-error'

export type Scenario = {
  /** 다음 응모 결과를 강제로 정한다. auto는 실제 남은 수량대로 처리한다. */
  entryOutcome: EntryOutcome
  /** 진행 중인 이벤트의 남은 수량을 폴링할 때마다 다른 참여자가 가져간 것처럼 줄인다. */
  simulateCrowd: boolean
  /** 모든 가짜 응답에 넣는 지연(ms). */
  latencyMs: number
}

const STORAGE_KEY = 'coupon-lab:mock-scenario'

const DEFAULT_SCENARIO: Scenario = {
  entryOutcome: 'auto',
  simulateCrowd: true,
  latencyMs: 300,
}

export function readScenario(): Scenario {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? { ...DEFAULT_SCENARIO, ...(JSON.parse(stored) as Partial<Scenario>) } : DEFAULT_SCENARIO
  } catch {
    return DEFAULT_SCENARIO
  }
}

export function writeScenario(scenario: Scenario) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(scenario))
  } catch {
    // 저장소를 쓸 수 없으면 기본값으로 동작한다.
  }
}
