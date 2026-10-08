package com.evoq.ems.asset.controller;

import java.time.LocalDate;

/** Only the asset label is added to an already authorized assignment history. */
public record AssignmentResponse(Long assignmentId, Long assetId, String assetName,
        Long employeeId, LocalDate assignedDate, LocalDate returnedDate, String assignmentStatus) { }
