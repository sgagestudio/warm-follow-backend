package com.sgagestudio.warm_follow_backend.scheduler;

import com.sgagestudio.warm_follow_backend.config.SchedulerProperties;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.JobBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail reminderDispatchJobDetail() {
        return JobBuilder.newJob(ReminderDispatchJob.class)
                .withIdentity("reminderDispatchJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger reminderDispatchTrigger(JobDetail reminderDispatchJobDetail, SchedulerProperties properties) {
        SimpleScheduleBuilder schedule = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes(properties.getReminderIntervalMinutes())
                .repeatForever();
        return TriggerBuilder.newTrigger()
                .forJob(reminderDispatchJobDetail)
                .withIdentity("reminderDispatchTrigger")
                .withSchedule(schedule)
                .build();
    }
}
