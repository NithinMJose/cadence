"use client";

import React, { useEffect, useState, useCallback } from "react";
import { Header } from "../components/Header";
import { WeeklyScheduleGrid } from "../components/WeeklyScheduleGrid";
import { ReplanConsole } from "../components/ReplanConsole";
import { DiffAutopsyAndFeynman } from "../components/DiffAutopsyAndFeynman";
import { TelemetryDashboard } from "../components/TelemetryDashboard";
import { WeeklyScheduleResponse, TopicCard } from "../lib/types";
import {
  fetchWeeklySchedule,
  fetchDueCards,
  completeBlock,
  seedDemoCurriculum,
  checkHealth,
} from "../lib/api";

export default function Home() {
  const [activeTab, setActiveTab] = useState<string>("schedule");
  const [schedule, setSchedule] = useState<WeeklyScheduleResponse | null>(null);
  const [dueCards, setDueCards] = useState<TopicCard[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isLoadingDemo, setIsLoadingDemo] = useState<boolean>(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [systemStatus, setSystemStatus] = useState({ backend: false, aiService: false });

  const loadData = useCallback(async () => {
    try {
      setIsLoading(true);
      const [schedRes, cardsRes, healthRes] = await Promise.all([
        fetchWeeklySchedule().catch(() => null),
        fetchDueCards().catch(() => []),
        checkHealth(),
      ]);
      setSchedule(schedRes);
      setDueCards(cardsRes);
      setSystemStatus(healthRes);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleCompleteBlock = async (id: string) => {
    try {
      await completeBlock(id, 45);
      setToastMessage("⚡ Block marked completed in <10ms! Flex-time banked.");
      await loadData();
      setTimeout(() => setToastMessage(null), 4000);
    } catch (err: unknown) {
      alert(`Complete failed: ${err instanceof Error ? err.message : "Error"}`);
    }
  };

  const handleLoadRecruiterDemo = async () => {
    setIsLoadingDemo(true);
    setToastMessage("Seeding Recruiter Persona across PostgreSQL & CP-SAT solver...");
    try {
      await seedDemoCurriculum();
      await loadData();
      setToastMessage("🎉 Recruiter Demo Persona loaded! 11 balanced daily activities & FSRS cards active.");
      setTimeout(() => setToastMessage(null), 5000);
    } catch (err: unknown) {
      setToastMessage(`Seed failed: ${err instanceof Error ? err.message : "Error"}`);
    } finally {
      setIsLoadingDemo(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#0b0d11]">
      <Header
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onLoadRecruiterDemo={handleLoadRecruiterDemo}
        isLoadingDemo={isLoadingDemo}
        systemStatus={systemStatus}
      />

      {toastMessage && (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 w-full mt-3">
          <div className="bg-sky-500/10 border border-sky-500/30 text-sky-300 text-xs px-4 py-2.5 rounded-lg flex items-center justify-between shadow-sm">
            <span>{toastMessage}</span>
            <button
              onClick={() => setToastMessage(null)}
              className="text-slate-400 hover:text-slate-200 text-xs font-bold ml-4"
            >
              ✕
            </button>
          </div>
        </div>
      )}

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {activeTab === "schedule" && (
          <WeeklyScheduleGrid
            schedule={schedule}
            onComplete={handleCompleteBlock}
            isLoading={isLoading}
          />
        )}

        {activeTab === "replan" && (
          <ReplanConsole onRefreshSchedule={loadData} />
        )}

        {activeTab === "feynman" && (
          <DiffAutopsyAndFeynman
            dueCards={dueCards}
            onRefreshDueCards={loadData}
          />
        )}

        {activeTab === "telemetry" && (
          <TelemetryDashboard schedule={schedule} />
        )}
      </main>

      <footer className="border-t border-slate-900 bg-[#0c0e13] py-4 text-center text-xs text-slate-500">
        Cadence AI Learning Coach & Constraint Scheduler • Deterministic Math Boundaries (CP-SAT & FSRS-4.5)
      </footer>
    </div>
  );
}
