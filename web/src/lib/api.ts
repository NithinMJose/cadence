import {
  WeeklyScheduleResponse,
  TopicCard,
  ReplanProposalResponse,
  FeynmanGradeResponse,
  GitHubAutopsyResult,
} from "./types";

const BACKEND_URL = process.env.NEXT_PUBLIC_BACKEND_URL || "http://localhost:8080";
const AI_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || "http://localhost:8000";
const API_KEY = process.env.NEXT_PUBLIC_API_KEY || "cadence-dev-secret-key-change-me";

function getHeaders(traceId?: string): Record<string, string> {
  const tid = traceId || `trace-web-${Math.random().toString(16).substring(2, 10)}`;
  return {
    "Content-Type": "application/json",
    "X-Cadence-Api-Key": API_KEY,
    "X-Trace-Id": tid,
  };
}

export async function fetchWeeklySchedule(): Promise<WeeklyScheduleResponse> {
  const res = await fetch(`${BACKEND_URL}/api/v1/schedule/week`, {
    headers: getHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error(`Failed to fetch weekly schedule: HTTP ${res.status}`);
  return res.json();
}

export async function fetchDueCards(): Promise<TopicCard[]> {
  const res = await fetch(`${BACKEND_URL}/api/v1/cards/due`, {
    headers: getHeaders(),
    cache: "no-store",
  });
  if (!res.ok) throw new Error(`Failed to fetch due cards: HTTP ${res.status}`);
  return res.json();
}

export async function seedDemoCurriculum(): Promise<{ status: string; message: string }> {
  const res = await fetch(`${BACKEND_URL}/api/v1/demo/seed`, {
    method: "POST",
    headers: getHeaders(),
  });
  if (!res.ok) throw new Error(`Failed to seed demo curriculum: HTTP ${res.status}`);
  return res.json();
}

export async function completeBlock(blockId: string, actualMinutes: number = 45): Promise<void> {
  const res = await fetch(`${BACKEND_URL}/api/v1/blocks/${blockId}/complete`, {
    method: "POST",
    headers: getHeaders(),
    body: JSON.stringify({ actual_duration_min: actualMinutes, rating: 3 }),
  });
  if (!res.ok) throw new Error(`Failed to complete block: HTTP ${res.status}`);
}

export async function proposeReplan(prompt: string): Promise<ReplanProposalResponse> {
  const res = await fetch(`${BACKEND_URL}/api/v1/schedule/replan/propose`, {
    method: "POST",
    headers: getHeaders(),
    body: JSON.stringify({ user_message: prompt }),
  });
  if (!res.ok) throw new Error(`Failed to propose replan: HTTP ${res.status}`);
  return res.json();
}

export async function confirmReplan(proposalId: string): Promise<{ status: string; updated_blocks_count: number }> {
  const res = await fetch(`${BACKEND_URL}/api/v1/schedule/replan/${proposalId}/confirm`, {
    method: "POST",
    headers: getHeaders(),
  });
  if (!res.ok) throw new Error(`Failed to confirm replan: HTTP ${res.status}`);
  return res.json();
}

export async function rejectReplan(proposalId: string): Promise<void> {
  await fetch(`${BACKEND_URL}/api/v1/schedule/replan/${proposalId}/reject`, {
    method: "POST",
    headers: getHeaders(),
  });
}

export async function gradeFeynmanExplanation(
  cardId: string,
  transcript: string
): Promise<FeynmanGradeResponse> {
  const res = await fetch(`${BACKEND_URL}/api/v1/cards/${cardId}/feynman-grade`, {
    method: "POST",
    headers: getHeaders(),
    body: JSON.stringify({ transcript }),
  });
  if (!res.ok) throw new Error(`Failed to grade Feynman explanation: HTTP ${res.status}`);
  return res.json();
}

export async function syncGitHubCommit(owner?: string, repo?: string): Promise<GitHubAutopsyResult> {
  const res = await fetch(`${BACKEND_URL}/api/v1/integrations/github/sync-latest`, {
    method: "POST",
    headers: getHeaders(),
    body: JSON.stringify({ owner, repo }),
  });
  if (!res.ok) throw new Error(`Failed to sync GitHub commit: HTTP ${res.status}`);
  return res.json();
}

export async function analyzeRawDiff(rawDiff: string, commitMessage?: string): Promise<GitHubAutopsyResult> {
  const res = await fetch(`${BACKEND_URL}/api/v1/integrations/github/analyze-raw-diff`, {
    method: "POST",
    headers: getHeaders(),
    body: JSON.stringify({ rawDiff, commitMessage, repoName: "cadence" }),
  });
  if (!res.ok) throw new Error(`Failed to analyze raw diff: HTTP ${res.status}`);
  return res.json();
}

export async function checkHealth(): Promise<{ backend: boolean; aiService: boolean }> {
  try {
    const [bRes, aRes] = await Promise.all([
      fetch(`${BACKEND_URL}/api/v1/system/health`).then((r) => r.ok).catch(() => false),
      fetch(`${AI_URL}/health`).then((r) => r.ok).catch(() => false),
    ]);
    return { backend: bRes, aiService: aRes };
  } catch {
    return { backend: false, aiService: false };
  }
}
