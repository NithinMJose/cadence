"use client";

import React from "react";
import { Sparkles, Calendar, RotateCcw, Brain, Activity, CheckCircle2, AlertCircle } from "lucide-react";

interface HeaderProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  onLoadRecruiterDemo: () => void;
  isLoadingDemo: boolean;
  systemStatus: { backend: boolean; aiService: boolean };
}

export const Header: React.FC<HeaderProps> = ({
  activeTab,
  setActiveTab,
  onLoadRecruiterDemo,
  isLoadingDemo,
  systemStatus,
}) => {
  const tabs = [
    { id: "schedule", label: "Weekly Schedule", icon: Calendar },
    { id: "replan", label: "Natural Language Replan", icon: RotateCcw },
    { id: "feynman", label: "Diff & Feynman Dojo", icon: Brain },
    { id: "telemetry", label: "Engineering Telemetry", icon: Activity },
  ];

  return (
    <header className="border-b border-slate-800 bg-[#0f1217]/90 backdrop-blur sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Meta */}
          <div className="flex items-center space-x-3">
            <div className="w-9 h-9 rounded-lg bg-sky-500/10 border border-sky-500/30 flex items-center justify-center text-sky-400 font-bold text-lg">
              C
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-slate-100 text-lg tracking-tight">Cadence</span>
                <span className="text-[10px] uppercase font-semibold tracking-wider px-1.5 py-0.5 rounded bg-sky-500/10 text-sky-400 border border-sky-500/20">
                  Control Center
                </span>
              </div>
              <p className="text-xs text-slate-400 hidden sm:block">
                OR-Tools CP-SAT + FSRS-4.5 Math Boundaries
              </p>
            </div>
          </div>

          {/* Right Actions & Telemetry Pills */}
          <div className="flex items-center space-x-4">
            {/* System Status Pills */}
            <div className="hidden md:flex items-center space-x-3 text-xs bg-slate-900/80 px-3 py-1.5 rounded-full border border-slate-800">
              <div className="flex items-center space-x-1.5">
                <span className={`w-2 h-2 rounded-full ${systemStatus.backend ? "bg-emerald-400 animate-pulse" : "bg-rose-500"}`} />
                <span className="text-slate-300">Backend:8080</span>
              </div>
              <span className="text-slate-600">|</span>
              <div className="flex items-center space-x-1.5">
                <span className={`w-2 h-2 rounded-full ${systemStatus.aiService ? "bg-sky-400 animate-pulse" : "bg-rose-500"}`} />
                <span className="text-slate-300">AI:8000</span>
              </div>
              <span className="text-slate-600">|</span>
              <span className="text-slate-400 font-mono text-[11px]">Asia/Kolkata</span>
            </div>

            {/* Recruiter Persona 1-Click Button */}
            <button
              onClick={onLoadRecruiterDemo}
              disabled={isLoadingDemo}
              className="flex items-center space-x-2 bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white font-medium text-xs px-3.5 py-2 rounded-lg shadow-lg shadow-sky-500/10 border border-sky-400/30 transition-all active:scale-95 disabled:opacity-50"
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>{isLoadingDemo ? "Seeding..." : "Load Recruiter Demo Persona"}</span>
            </button>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="flex space-x-1 overflow-x-auto pb-2 scrollbar-none">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center space-x-2 px-3.5 py-2 text-xs font-medium rounded-lg transition-colors whitespace-nowrap ${
                  isActive
                    ? "bg-slate-800 text-sky-400 border border-slate-700 shadow-sm"
                    : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/50"
                }`}
              >
                <Icon className="w-4 h-4" />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>
    </header>
  );
};
