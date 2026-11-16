-- Add proposed_blocks_json to schedule_proposals for two-phase atomic confirmation
ALTER TABLE schedule_proposals ADD COLUMN proposed_blocks_json JSONB;
