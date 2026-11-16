"use client";

import React, { useState } from "react";
import { ReplanProposalResponse } from "../lib/types";
import { proposeReplan, confirmReplan, rejectReplan } from "../lib/api";
import { ArrowRight, Check, X, Sparkles, AlertCircle, Clock, ShieldAlert } from "lucide-react";

interface ReplanConsoleProps {
  onRefreshSchedule: () => Promise<void>;
}

export const ReplanConsole: React.FC<ReplanConsoleProps> = ({ onRefreshSchedule }) => {
  const [prompt, setPrompt] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [proposal, setProposal] = useState<ReplanProposalResponse | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const samplePrompts = [
    "Dentist appointment Tuesday 3pm to 5pm, don't sacrifice my workout",
    "Exhausted today, downgrade evening block to 3m emergency floor",
    "Block Friday evening for dinner with friends from 7pm to 10pm",
    "Double my work focus block tomorrow morning to 4 hours",
  ];

  const handlePropose = async (userPrompt?: string) => {
    const text = userPrompt || prompt;
    if (!text.trim()) return;
    setIsSubmitting(true);
    setStatusMessage("Extracting constraints & running OR-Tools CP-SAT solver...");
    try {
      const res = await proposeReplan(text);
      setProposal(res);
      setStatusMessage(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Proposal failed";
      setStatusMessage(`Error: ${msg}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirm = async () => {
    if (!proposal) return;
    setIsSubmitting(true);
    try {
      await confirmReplan(proposal.proposal_id);
      setStatusMessage("Schedule updated atomically in PostgreSQL!");
      setProposal(null);
      setPrompt("");
      await onRefreshSchedule();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Confirmation failed";
      setStatusMessage(`Error: ${msg}`);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!proposal) return;
    try {
      await rejectReplan(proposal.proposal_id);
      setProposal(null);
      setStatusMessage("Proposal rejected. Schedule preserved.");
    } catch {
      setProposal(null);
    }
  };

  const formatSlot = (iso?: string | null) => {
    if (!iso) return "--:--";
    try {
      return new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", hour12: false });
    } catch {
      return iso;
    }
  };

  return (
    <div className="space-y-6">
      {/* Input Console */}
      <div className="bg-[#12151b] border border-slate-800 rounded-xl p-5">
        <h2 className="text-base font-semibold text-slate-100 flex items-center space-x-2 mb-2">
          <Sparkles className="w-4 h-4 text-sky-400" />
          <span>Natural Language & Voice Replan Console</span>
        </h2>
        <p className="text-xs text-slate-400 mb-4">
          Describe unexpected calendar changes. The local LLM extracts symbolic AST constraints, resolved to Asia/Kolkata, and solved via Google OR-Tools CP-SAT.
        </p>

        <div className="flex gap-2 mb-3">
          <input
            type="text"
            value={prompt}
            onChange={(e) => setPrompt(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handlePropose()}
            placeholder="e.g. Dentist appointment Tuesday 3pm to 5pm, don't sacrifice workout..."
            className="flex-1 bg-slate-900 border border-slate-700/80 rounded-lg px-4 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-sky-500"
          />
          <button
            onClick={() => handlePropose()}
            disabled={isSubmitting || !prompt.trim()}
            className="bg-sky-500 hover:bg-sky-400 text-white font-medium text-xs px-5 py-2.5 rounded-lg transition-all active:scale-95 disabled:opacity-50"
          >
            {isSubmitting ? "Solving..." : "Replan"}
          </button>
        </div>

        {/* Quick Prompt Chips */}
        <div className="flex flex-wrap gap-2">
          {samplePrompts.map((sp, idx) => (
            <button
              key={idx}
              onClick={() => { setPrompt(sp); handlePropose(sp); }}
              className="text-[11px] bg-slate-800/80 hover:bg-slate-700/80 text-slate-300 border border-slate-700/60 px-2.5 py-1 rounded-md transition-all text-left"
            >
              &quot;{sp}&quot;
            </button>
          ))}
        </div>

        {statusMessage && (
          <div className="mt-3 text-xs px-3 py-2 rounded-lg bg-sky-500/10 border border-sky-500/20 text-sky-300">
            {statusMessage}
          </div>
        )}
      </div>

      {/* Proposed Diff & Tradeoffs */}
      {proposal && (
        <div className="bg-[#12151b] border border-sky-500/30 rounded-xl p-5 space-y-5 animate-in fade-in">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-slate-100 text-sm flex items-center space-x-2">
                <span>Proposed Schedule Changes</span>
                <span className="text-xs px-2 py-0.5 rounded bg-sky-500/10 text-sky-400 font-mono">
                  {proposal.diff_items.length} Moves
                </span>
              </h3>
              <p className="text-xs text-slate-400 mt-0.5">
                Review before committing. No changes touch database until confirmed.
              </p>
            </div>
            <div className="flex items-center space-x-2">
              <button
                onClick={handleReject}
                className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700 text-xs font-medium"
              >
                <X className="w-3.5 h-3.5" />
                <span>Discard</span>
              </button>
              <button
                onClick={handleConfirm}
                disabled={isSubmitting}
                className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-emerald-500 hover:bg-emerald-400 text-white text-xs font-medium shadow-md shadow-emerald-500/20"
              >
                <Check className="w-3.5 h-3.5" />
                <span>Confirm Changes</span>
              </button>
            </div>
          </div>

          {/* Diff Cards */}
          <div className="space-y-2.5">
            {proposal.diff_items.map((diff, idx) => (
              <div
                key={idx}
                className="bg-[#161a22] border border-slate-800 rounded-lg p-3 flex flex-col md:flex-row md:items-center justify-between gap-3"
              >
                <div className="flex-1">
                  <span className="font-semibold text-sm text-slate-200">{diff.title}</span>
                  <div className="flex items-center space-x-2 text-xs text-slate-400 mt-1 font-mono">
                    <span className="line-through opacity-70">
                      {formatSlot(diff.old_start)} – {formatSlot(diff.old_end)}
                    </span>
                    <ArrowRight className="w-3 h-3 text-sky-400" />
                    <span className="text-sky-400 font-semibold">
                      {formatSlot(diff.new_start)} – {formatSlot(diff.new_end)}
                    </span>
                  </div>
                </div>
                <div className="flex items-center space-x-2">
                  <span className="text-xs px-2.5 py-1 rounded-md bg-slate-800 text-sky-300 font-mono text-[11px] border border-slate-700">
                    {diff.causal_reason}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {/* Tradeoff Cards */}
          {proposal.tradeoff_cards.length > 0 && (
            <div className="pt-3 border-t border-slate-800 space-y-2">
              <h4 className="text-xs font-semibold uppercase tracking-wider text-amber-400 flex items-center space-x-1.5">
                <ShieldAlert className="w-3.5 h-3.5" />
                <span>Slack-Based Capacity Negotiation Cards</span>
              </h4>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {proposal.tradeoff_cards.map((card, idx) => (
                  <div key={idx} className="bg-amber-500/5 border border-amber-500/20 rounded-lg p-3">
                    <div className="font-semibold text-xs text-amber-300 mb-1">{card.title}</div>
                    <p className="text-xs text-slate-300 mb-2">{card.description}</p>
                    <span className="text-[11px] font-mono text-amber-400/90 bg-amber-500/10 px-2 py-0.5 rounded">
                      +{card.recovered_minutes}m recovered
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
