export interface TimeBlock {
  id: string;
  card_id?: string | null;
  title: string;
  topic_tag: string;
  start_time: string;
  end_time: string;
  status: "SCHEDULED" | "COMPLETED" | "SKIPPED" | "FLOOR_DOWNGRADED";
  device_type: "PHONE_COMMUTE" | "LAPTOP_DEEP";
  planned_duration_min: number;
  actual_duration_min?: number | null;
  is_emergency_floor: boolean;
  warmup_brief_json?: string | null;
}

export interface WeeklyScheduleResponse {
  start_date: string;
  end_date: string;
  timezone: string;
  total_blocks: number;
  blocks: TimeBlock[];
  flex_credits_minutes: number;
  recovery_mode_active: boolean;
  trace_id?: string;
}

export interface TopicCard {
  id: string;
  goal_id?: string | null;
  title: string;
  concept_summary: string;
  topic_tag: string;
  stability: number;
  difficulty: number;
  elapsed_days: number;
  scheduled_days: number;
  reps: number;
  lapses: number;
  state: "NEW" | "LEARNING" | "REVIEW" | "RELEARNING";
  last_review_at?: string | null;
  next_review_at?: string | null;
  emergency_floor_prompt: string;
  rubric_json?: string | null;
}

export interface ScheduleDiffItem {
  task_id: string;
  title: string;
  old_start?: string | null;
  old_end?: string | null;
  new_start?: string | null;
  new_end?: string | null;
  action: "SHIFTED" | "KEPT" | "FLOOR_DOWNGRADED" | "DEFERRED";
  causal_reason: string;
}

export interface TradeoffCard {
  card_type: "SLACK_DOWNGRADE_FLOOR" | "SLACK_DEFER_WEEKEND" | "SLACK_PROTECT_LEISURE";
  title: string;
  description: string;
  impact_summary: string;
  recovered_minutes: number;
}

export interface ReplanProposalResponse {
  proposal_id: string;
  status: "PENDING" | "CONFIRMED" | "REJECTED";
  schedule_version: number;
  ast_summary?: string;
  diff_items: ScheduleDiffItem[];
  tradeoff_cards: TradeoffCard[];
  total_scheduled_minutes: number;
  total_slack_minutes: number;
  churn_score: number;
  trace_id?: string;
  _debug_trace?: Record<string, unknown>;
}

export interface FeynmanGradeResponse {
  cardId: string;
  cardTitle: string;
  fsrsGrade: number;
  matchedCheckpoints: string[];
  missingCheckpoints: string[];
  feedback: string;
  stability: number;
  difficulty: number;
  scheduledDays: number;
  reps: number;
  lapses: number;
  state: string;
  nextReviewAt?: string | null;
  traceId?: string;
}

export interface GitHubAutopsyResult {
  commitHash: string;
  commitMessage: string;
  cardsGenerated: number;
  cardIds: string[];
  warmupBrief: string[];
  emergencyFloorQuiz: string;
  traceId: string;
}

export interface DebugFlightTrace {
  trace_id: string;
  llm_provider_used?: string;
  llm_model?: string;
  llm_latency_ms?: number;
  secrets_redacted_count?: number;
  validation_retries_used?: number;
  extracted_ast_summary?: Record<string, unknown>;
  solver_status?: string;
  solver_wall_time_ms?: number;
  active_constraints_triggered?: string[];
}
