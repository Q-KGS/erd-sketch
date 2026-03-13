package com.erdsketch.project;

public record UpdateProjectRequest(String name, String description, DbType targetDbType) {}
