"use client";

import React, { useState } from "react";
import { TopicCard, GitHubAutopsyResult, FeynmanGradeResponse } from "../lib/types";
import { syncGitHubCommit, analyzeRawDiff, gradeFeynmanExplanation } from "../lib/api";
import { GitCommit, Sparkles, Check, X, Mic, Brain, Shield, Clock } from "lucide-react";

interface DiffAutopsyAndFeynmanProps {
  dueCards: TopicCard[];
  onRefreshDueCards: () => Promise<void>;
}

export const DiffAutopsyAndFeynman: React.FC<DiffAutopsyAndFeynmanProps> = ({
  dueCards,
  onRefreshDueCards,
}) => {
  const [rawDiff, setRawDiff] = useState(
    "diff --git a/backend/fsrs.py b/backend/fsrs.py\n+def power_law_forgetting_curve(t, S):\n+    # Retrievability decays according to FSRS-4.5 formula\n+    return (1.0 + FACTOR * (t / S)) ** DECAY"
  );
  const [autopsyResult, setAutopsyResult] = useState<GitHubAutopsyResult | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [autopsyStatus, setAutopsyStatus] = useState<string | null>(null);

  // Feynman state
  const [selectedCardId, setSelectedCardId] = useState<string>(dueCards[0]?.id || "");
  const [transcript, setTranscript] = useState("");
  const [isGrading, setIsGrading] = useState(false);
  const [feynmanResult, setFeynmanResult] = useState<FeynmanGradeResponse | null>(null);

  const handleAnalyzeDiff = async () => {
    if (!rawDiff.trim()) return;
    setIsAnalyzing(true);
    setAutopsyStatus("Scrubbing secrets & analyzing code diff...");
    try {
      const res = await analyzeRawDiff(rawDiff, "feat(fsrs): add power law curve");
      setAutopsyResult(res);
      setAutopsyStatus(null);
      await onRefreshDueCards();
    } catch (err: unknown) {
      setAutopsyStatus(`Error: ${err instanceof Error ? err.message : "Analysis failed"}`);
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handlePollGitHub = async () => {
    setIsAnalyzing(true);
    setAutopsyStatus("Polling commits via GitHub REST API...");
    try {
      const res = await syncGitHubCommit("NithinMJose", "cadence");
      setAutopsyResult(res);
      setAutopsyStatus(null);
      await onRefreshDueCards();
    } catch (err: unknown) {
      setAutopsyStatus(`Error: ${err instanceof Error ? err.message : "GitHub polling failed"}`);
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleGradeExplanation = async () => {
    const cardId = selectedCardId || dueCards[0]?.id;
    if (!cardId || !transcript.trim()) return;
    setIsGrading(true);
    try {
      const res = await gradeFeynmanExplanation(cardId, transcript);
      setFeynmanResult(res);
      await onRefreshDueCards();
    } catch (err: unknown) {
      alert(`Grading failed: ${err instanceof Error ? err.message : "Unknown error"}`);
    } finally {
      setIsGrading(false);
    }
  };

  const selectedCard = dueCards.find((c) => c.id === selectedCardId) || dueCards[0];

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {/* Left: GitHub Diff Autopsy */}
      <div className="bg-[#12151b] border border-slate-800 rounded-xl p-5 space-y-4">
        <div>
          <h2 className="text-base font-semibold text-slate-100 flex items-center space-x-2">
            <GitCommit className="w-4 h-4 text-sky-400" />
            <span>GitHub Commit Diff Autopsy</span>
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Secret-scrubbed diff analysis inside &lt;untrusted_code_diff&gt; generating FSRS cards & briefs.
          </p>
        </div>

        <textarea
          value={rawDiff}
          onChange={(e) => setRawDiff(e.target.value)}
          rows={5}
          placeholder="Paste git diff here..."
          className="w-full bg-slate-900 border border-slate-700/80 rounded-lg p-3 text-xs font-mono text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500"
        />

        <div className="flex flex-wrap gap-2">
          <button
            onClick={handleAnalyzeDiff}
            disabled={isAnalyzing}
            className="bg-sky-500 hover:bg-sky-400 text-white font-medium text-xs px-4 py-2 rounded-lg transition-all active:scale-95 disabled:opacity-50"
          >
            {isAnalyzing ? "Analyzing..." : "Analyze Raw Diff"}
          </button>
          <button
            onClick={handlePollGitHub}
            disabled={isAnalyzing}
            className="bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs px-4 py-2 rounded-lg transition-all"
          >
            Poll NithinMJose/cadence
          </button>
        </div>

        {autopsyStatus && (
          <div className="text-xs px-3 py-2 rounded bg-sky-500/10 text-sky-300 border border-sky-500/20">
            {autopsyStatus}
          </div>
        )}

        {autopsyResult && (
          <div className="bg-[#161a22] border border-slate-800 rounded-lg p-4 space-y-3">
            <div className="flex items-center justify-between text-xs">
              <span className="font-semibold text-slate-200">Commit: {autopsyResult.commitHash}</span>
              <span className="text-emerald-400 font-medium">+{autopsyResult.cardsGenerated} Flashcard(s)</span>
            </div>

            <div>
              <span className="text-[11px] font-semibold text-amber-400 uppercase tracking-wider">
                Session Warm-Up Brief:
              </span>
              <ul className="mt-1 space-y-1 text-xs text-slate-300">
                {autopsyResult.warmupBrief.map((b, i) => (
                  <li key={i} className="flex items-start space-x-1.5">
                    <span className="text-sky-400">•</span>
                    <span>{b}</span>
                  </li>
                ))}
              </ul>
            </div>

            <div className="pt-2 border-t border-slate-800">
              <span className="text-[11px] font-semibold text-rose-400 uppercase tracking-wider">
                3-Minute Emergency Floor Micro-Quiz:
              </span>
              <p className="text-xs text-slate-300 mt-0.5">{autopsyResult.emergencyFloorQuiz}</p>
            </div>
          </div>
        )}
      </div>

      {/* Right: Feynman Voice Dojo */}
      <div className="bg-[#12151b] border border-slate-800 rounded-xl p-5 space-y-4">
        <div>
          <h2 className="text-base font-semibold text-slate-100 flex items-center space-x-2">
            <Brain className="w-4 h-4 text-purple-400" />
            <span>Feynman Flashcard Voice Dojo</span>
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Explain technical concepts in plain terms. Evaluated against 4 rubric points with FSRS update.
          </p>
        </div>

        {dueCards.length === 0 ? (
          <div className="py-12 text-center text-xs text-slate-400">
            No cards due for review! Seed demo cards to test the Feynman Dojo.
          </div>
        ) : (
          <div className="space-y-4">
            <select
              value={selectedCardId}
              onChange={(e) => { setSelectedCardId(e.target.value); setFeynmanResult(null); }}
              className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-xs text-slate-200"
            >
              {dueCards.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.title} ({c.topic_tag})
                </option>
              ))}
            </select>

            {selectedCard && (
              <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-800 text-xs">
                <div className="font-semibold text-slate-200 mb-1">{selectedCard.concept_summary}</div>
                <div className="text-slate-400 font-mono text-[11px]">
                  Stability: {selectedCard.stability}d • Reps: {selectedCard.reps} • State: {selectedCard.state}
                </div>
              </div>
            )}

            <textarea
              value={transcript}
              onChange={(e) => setTranscript(e.target.value)}
              rows={4}
              placeholder="Enter your 60-second explanation transcript here..."
              className="w-full bg-slate-900 border border-slate-700/80 rounded-lg p-3 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-purple-500"
            />

            <button
              onClick={handleGradeExplanation}
              disabled={isGrading || !transcript.trim()}
              className="w-full bg-gradient-to-r from-purple-500 to-indigo-600 hover:from-purple-400 hover:to-indigo-500 text-white font-medium text-xs py-2.5 rounded-lg transition-all disabled:opacity-50"
            >
              {isGrading ? "Evaluating with Local LLM..." : "Submit Explanation for Feynman Grading"}
            </button>

            {feynmanResult && (
              <div className="bg-[#161a22] border border-purple-500/30 rounded-lg p-4 space-y-2.5 animate-in fade-in">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-200">
                    Grade {feynmanResult.fsrsGrade}/4 (
                    {feynmanResult.fsrsGrade === 4 ? "Easy" : feynmanResult.fsrsGrade === 3 ? "Good" : feynmanResult.fsrsGrade === 2 ? "Hard" : "Again"}
                    )
                  </span>
                  <span className="text-xs text-purple-400 font-mono">
                    New Stability: {feynmanResult.stability}d
                  </span>
                </div>
                <p className="text-xs text-slate-300 font-medium">{feynmanResult.feedback}</p>

                <div className="space-y-1 text-xs pt-2 border-t border-slate-800">
                  {feynmanResult.matchedCheckpoints.map((cp, i) => (
                    <div key={i} className="flex items-center space-x-1.5 text-emerald-400 text-[11px]">
                      <Check className="w-3.5 h-3.5 shrink-0" />
                      <span>{cp}</span>
                    </div>
                  ))}
                  {feynmanResult.missingCheckpoints.map((cp, i) => (
                    <div key={i} className="flex items-center space-x-1.5 text-rose-400 text-[11px]">
                      <X className="w-3.5 h-3.5 shrink-0" />
                      <span>{cp}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
