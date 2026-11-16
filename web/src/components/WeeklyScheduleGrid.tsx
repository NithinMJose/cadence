"use client";

import React, { useState } from "react";
import { TimeBlock, WeeklyScheduleResponse } from "../lib/types";
import { Laptop, Smartphone, Check, Clock, AlertTriangle, ChevronDown, ChevronUp, Zap } from "lucide-react";

interface WeeklyScheduleGridProps {
  schedule: WeeklyScheduleResponse | null;
  onComplete: (id: string) => Promise<void>;
  isLoading: boolean;
}

export const WeeklyScheduleGrid: React.FC<WeeklyScheduleGridProps> = ({ schedule, onComplete, isLoading }) => {
  const [selectedDayIndex, setSelectedDayIndex] = useState<number>(0);
  const [expandedBriefs, setExpandedBriefs] = useState<Record<string, boolean>>({});

  if (isLoading || !schedule) {
    return (
      <div className="flex items-center justify-center py-24">
        <div className="flex flex-col items-center space-y-3">
          <div className="w-8 h-8 border-2 border-sky-400 border-t-transparent rounded-full animate-spin" />
          <p className="text-slate-400 text-sm">Loading 7-day schedule from PostgreSQL...</p>
        </div>
      </div>
    );
  }

  // Group blocks by day of week
  const days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"];
  const groupedBlocks: Record<number, TimeBlock[]> = {};
  for (let i = 0; i < 7; i++) groupedBlocks[i] = [];

  schedule.blocks.forEach((block) => {
    const date = new Date(block.start_time);
    // Day of week index 0..6 starting Monday
    const day = (date.getDay() + 6) % 7;
    groupedBlocks[day].push(block);
  });

  const currentDayBlocks = groupedBlocks[selectedDayIndex] || [];

  const toggleBrief = (id: string) => {
    setExpandedBriefs((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  const getTagColor = (tag: string) => {
    switch (tag.toLowerCase()) {
      case "work": return "bg-blue-500/10 text-blue-400 border-blue-500/20";
      case "fitness": return "bg-emerald-500/10 text-emerald-400 border-emerald-500/20";
      case "gaming": return "bg-purple-500/10 text-purple-400 border-purple-500/20";
      case "upskill": return "bg-amber-500/10 text-amber-400 border-amber-500/20";
      case "wellness": return "bg-teal-500/10 text-teal-400 border-teal-500/20";
      case "routine": return "bg-slate-500/10 text-slate-400 border-slate-500/20";
      case "sleep": return "bg-indigo-500/10 text-indigo-400 border-indigo-500/20";
      default: return "bg-sky-500/10 text-sky-400 border-sky-500/20";
    }
  };

  const formatTime = (iso: string) => {
    try {
      const d = new Date(iso);
      return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", hour12: false });
    } catch {
      return iso;
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Horizon & Day Selector */}
      <div className="bg-[#12151b] border border-slate-800 rounded-xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-base font-semibold text-slate-100 flex items-center space-x-2">
            <span>Weekly Horizon</span>
            <span className="text-xs px-2 py-0.5 rounded bg-sky-500/10 text-sky-400 font-mono">
              {schedule.total_blocks} Blocks
            </span>
            {schedule.recovery_mode_active && (
              <span className="text-xs px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 font-semibold flex items-center space-x-1">
                <AlertTriangle className="w-3 h-3" />
                <span>Recovery Mode Active</span>
              </span>
            )}
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            15-minute slot resolution • Hard leisure constraints • Peak-hour cognitive alignment
          </p>
        </div>

        {/* Day Pills */}
        <div className="flex flex-wrap gap-1.5">
          {days.map((dayName, idx) => (
            <button
              key={dayName}
              onClick={() => setSelectedDayIndex(idx)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                selectedDayIndex === idx
                  ? "bg-sky-500 text-white shadow-md shadow-sky-500/20"
                  : "bg-slate-800/80 text-slate-300 hover:bg-slate-700/80 border border-slate-700/50"
              }`}
            >
              {dayName.substring(0, 3)}
              <span className="ml-1.5 opacity-60 text-[10px]">({groupedBlocks[idx]?.length || 0})</span>
            </button>
          ))}
        </div>
      </div>

      {/* Day Schedule Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {currentDayBlocks.map((block) => {
          let briefList: string[] = [];
          if (block.warmup_brief_json) {
            try {
              briefList = JSON.parse(block.warmup_brief_json);
            } catch {
              briefList = [];
            }
          }
          const isExpanded = !!expandedBriefs[block.id];
          const isCompleted = block.status === "COMPLETED";

          return (
            <div
              key={block.id}
              className={`rounded-xl border p-4 transition-all flex flex-col justify-between ${
                isCompleted
                  ? "bg-[#0f1318]/60 border-slate-800/60 opacity-80"
                  : "bg-[#141820] border-slate-800 hover:border-slate-700 shadow-sm"
              }`}
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-2">
                  <span className={`text-[11px] font-semibold uppercase px-2 py-0.5 rounded border ${getTagColor(block.topic_tag)}`}>
                    {block.topic_tag}
                  </span>
                  <div className="flex items-center space-x-1.5 text-xs text-slate-400">
                    {block.device_type === "PHONE_COMMUTE" ? (
                      <span className="flex items-center space-x-1 text-teal-400 bg-teal-500/10 px-2 py-0.5 rounded text-[11px]">
                        <Smartphone className="w-3 h-3" />
                        <span>Phone</span>
                      </span>
                    ) : (
                      <span className="flex items-center space-x-1 text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded text-[11px]">
                        <Laptop className="w-3 h-3" />
                        <span>Laptop</span>
                      </span>
                    )}
                  </div>
                </div>

                <h3 className="font-semibold text-slate-100 text-sm mb-1 leading-snug">
                  {block.title}
                </h3>

                <div className="flex items-center space-x-3 text-xs text-slate-400 font-mono mb-3">
                  <span className="flex items-center space-x-1">
                    <Clock className="w-3 h-3" />
                    <span>{formatTime(block.start_time)} – {formatTime(block.end_time)}</span>
                  </span>
                  <span>({block.planned_duration_min}m)</span>
                </div>

                {/* Session Warm-Up Brief (3 bullets) */}
                {briefList.length > 0 && (
                  <div className="mt-2 pt-2 border-t border-slate-800/80">
                    <button
                      onClick={() => toggleBrief(block.id)}
                      className="flex items-center justify-between w-full text-left text-xs text-sky-400 hover:text-sky-300 font-medium py-1"
                    >
                      <span className="flex items-center space-x-1">
                        <Zap className="w-3 h-3 text-amber-400" />
                        <span>Warm-Up Brief ({briefList.length})</span>
                      </span>
                      {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                    </button>
                    {isExpanded && (
                      <ul className="mt-1.5 space-y-1 text-xs text-slate-300 bg-slate-900/60 p-2.5 rounded-lg border border-slate-800">
                        {briefList.map((bullet, bIdx) => (
                          <li key={bIdx} className="flex items-start space-x-1.5">
                            <span className="text-sky-400 select-none">•</span>
                            <span>{bullet}</span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                )}
              </div>

              {/* Card Footer Actions */}
              <div className="mt-4 pt-3 border-t border-slate-800 flex items-center justify-between">
                <span className={`text-xs font-medium ${isCompleted ? "text-emerald-400" : "text-slate-400"}`}>
                  {block.status}
                </span>
                {!isCompleted && (
                  <button
                    onClick={() => onComplete(block.id)}
                    className="flex items-center space-x-1 text-xs bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 px-2.5 py-1 rounded-md transition-all active:scale-95"
                  >
                    <Check className="w-3 h-3" />
                    <span>Mark Done</span>
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
