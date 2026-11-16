package com.cadence.android.data.remote

import retrofit2.Response
import retrofit2.http.*

interface CadenceApiService {

    @GET("/api/v1/schedule/week")
    suspend fun getWeeklySchedule(): Response<WeeklyScheduleDto>

    @POST("/api/v1/blocks/{id}/complete")
    suspend fun completeBlock(
        @Path("id") blockId: String,
        @Body request: CompleteBlockRequestDto
    ): Response<CompleteBlockResponseDto>

    @POST("/api/v1/schedule/replan/propose")
    suspend fun proposeReplan(
        @Body request: ReplanProposeRequestDto
    ): Response<ReplanProposalResponseDto>

    @POST("/api/v1/schedule/replan/{id}/confirm")
    suspend fun confirmReplan(
        @Path("id") proposalId: String
    ): Response<ReplanConfirmResponseDto>

    @POST("/api/v1/demo/seed")
    suspend fun seedDemo(): Response<Map<String, Any>>
}
