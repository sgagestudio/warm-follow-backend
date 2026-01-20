package com.sgagestudio.warm_follow_backend.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.DisallowConcurrentExecution;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class ReminderDispatchJob implements Job {
    private final ReminderDispatchService dispatchService;

    public ReminderDispatchJob(ReminderDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        dispatchService.dispatchDueReminders();
    }
}
