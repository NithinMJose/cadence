"use client";

import React, { useState } from "react";
import { WeeklyScheduleResponse } from "../lib/types";
import { Activity, ShieldCheck, BatteryCharging, Terminal, ChevronDown, ChevronUp, Cpu, Server } from "lucide-react";

interface TelemetryDashboardProps {
  schedule: WeeklyScheduleResponse | null;
}

export const TelemetryDashboard: React.FC<TelemetryDashboardProps> = ({ schedule }) => {
  const [showFlightRecorder, setShowFlightRecorder] = useState(false);

  // EMA topic duration bias stats (defaults + calibrated multipliers)
  const durationStats = [
    { tag: "work", multiplier: 1.15, samples: 14, speed: "15% slower than planned" },
    { tag: "upskill", multiplier: 0.92, samples: 8, speed: "8% faster than planned" },
    { tag: "fitness", multiplier: 1.0, samples: 12, speed: "Exact on-time baseline" },
    { tag: "gaming", multiplier: 1.25, samples: 7, speed: "25% over planned time" },
    { tag: "wellness", multiplier: 0.98, samples: 10, speed: "Optimal pace" },
  ];

  const flexCredits = schedule?.flex_credits_minutes || 60;
  const isRecovery = schedule?.recovery_mode_active || false;

  const mockFlightTrace = {
    trace_id: schedule?.trace_id || "trace-flight-sample-82b1",
    llm_provider_used: "ollama_local",
    llm_model: "qwen2.5:7b-instruct-q4_K_M",
    llm_latency_ms: 184.2,
    secrets_redacted_count: 0,
    validation_retries_used: 0,
    solver_status: "OPTIMAL",
    solver_wall_time_ms: 42.6,
    active_constraints_triggered: [
      "NO_OVERLAP_INTERVALS",
      "PROTECTED_LEISURE_WINDOW",
      "REST_RESTORATIVE_SLEEP_PROTECTION",
      "DEVICE_MATCH_PHONE_COMMUTE",
      "PEAK_HOUR_COGNITIVE_WEIGHT_ALIGNMENT",
    ],
  };

  return (
    <div className="space-y-6">
      {/* Top Telemetry KPI Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Burnout Guard */}
        <div className="bg-[#12151b] border border-slate-800 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Burnout Guard</span>
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-xl font-bold text-slate-100 flex items-center space-x-2">
            <span>{isRecovery ? "Recovery Mode" : "Nominal Load"}</span>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Rolling 7-day completion rate: <span className="text-emerald-400 font-semibold font-mono">92%</span>
          </p>
        </div>

        {/* Flex-Time Bank */}
        <div className="bg-[#12151b] border border-slate-800 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Flex-Time Banking</span>
            <BatteryCharging className="w-4 h-4 text-sky-400" />
          </div>
          <div className="text-xl font-bold text-slate-100 font-mono">
            {flexCredits}m <span className="text-xs font-normal text-slate-400">banked</span>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Auto-awarded when deep-work tasks finish ahead of schedule.
          </p>
        </div>

        {/* Local Hardware Residency */}
        <div className="bg-[#12151b] border border-slate-800 rounded-xl p-4">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Compute Hardware</span>
            <Cpu className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="text-sm font-semibold text-slate-100">
            NVIDIA RTX 5060 (Blackwell 8GB)
          </div>
          <p className="text-xs text-slate-400 mt-1 font-mono">
            Ollama qwen2.5:7b-instruct-q4_K_M
          </p>
        </div>
      </div>

      {/* Duration Bias Calibrator Table */}
      <div className="bg-[#12151b] border border-slate-800 rounded-xl p-5">
        <h3 className="text-base font-semibold text-slate-100 flex items-center space-x-2 mb-2">
          <Activity className="w-4 h-4 text-sky-400" />
          <span>Duration Bias Calibrator (EMA Multipliers)</span>
        </h3>
        <p className="text-xs text-slate-400 mb-4">
          Exponential Moving Average (alpha = 0.3, clamped [0.7, 2.5]). Future CP-SAT solver blocks auto-scale to your true velocity.
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {durationStats.map((stat) => (
            <div key={stat.tag} className="bg-slate-900/80 border border-slate-800 p-3.5 rounded-lg">
              <div className="flex items-center justify-between mb-1.5">
                <span className="font-semibold text-xs text-slate-200 uppercase">{stat.tag}</span>
                <span className="text-xs font-mono font-bold text-sky-400 bg-sky-500/10 px-2 py-0.5 rounded">
                  {stat.multiplier.toFixed(2)}x
                </span>
              </div>
              <p className="text-xs text-slate-400">{stat.speed}</p>
              <div className="text-[10px] text-slate-500 mt-2 font-mono">
                {stat.samples} historical sessions tracked
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Collapsible Flight Recorder Drawer */}
      <div className="bg-[#12151b] border border-slate-800 rounded-xl p-5">
        <button
          onClick={() => setShowFlightRecorder(!showFlightRecorder)}
          className="flex items-center justify-between w-full text-left"
        >
          <div className="flex items-center space-x-2">
            <Terminal className="w-4 h-4 text-sky-400" />
            <h3 className="text-base font-semibold text-slate-100">
              _debug_trace Flight Recorder
            </h3>
            <span className="text-xs px-2 py-0.5 rounded bg-sky-500/10 text-sky-400 font-mono">
              Live Trace
            </span>
          </div>
          {showFlightRecorder ? <ChevronUp className="w-4 h-4 text-slate-400" /> : <ChevronDown className="w-4 h-4 text-slate-400" />}
        </button>

        {showFlightRecorder && (
          <div className="mt-4 pt-4 border-t border-slate-800 space-y-3">
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs font-mono">
              <div className="bg-slate-900 p-2.5 rounded border border-slate-800">
                <div className="text-slate-500 text-[10px]">TRACE_ID</div>
                <div className="text-slate-200 truncate">{mockFlightTrace.trace_id}</div>
              </div>
              <div className="bg-slate-900 p-2.5 rounded border border-slate-800">
                <div className="text-slate-500 text-[10px]">LLM_LATENCY</div>
                <div className="text-sky-400">{mockFlightTrace.llm_latency_ms}ms</div>
              </div>
              <div className="bg-slate-900 p-2.5 rounded border border-slate-800">
                <div className="text-slate-500 text-[10px]">SOLVER_WALL_TIME</div>
                <div className="text-emerald-400">{mockFlightTrace.solver_wall_time_ms}ms</div>
              </div>
              <div className="bg-slate-900 p-2.5 rounded border border-slate-800">
                <div className="text-slate-500 text-[10px]">SOLVER_STATUS</div>
                <div className="text-indigo-400">{mockFlightTrace.solver_status}</div>
              </div>
            </div>

            <pre className="bg-[#0b0d11] p-3 rounded-lg border border-slate-800 text-[11px] font-mono text-slate-300 overflow-x-auto">
              {JSON.stringify(mockFlightTrace, null, 2)}
            </pre>
          </div>
        )}
      </div>
    </div>
  );
};
