package org.appdevncsu.foodfinder.server

import org.appdevncsu.foodfinder.scraper.runScraper
import org.appdevncsu.foodfinder.shared.NCSU_ZONE
import org.quartz.CronScheduleBuilder
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobExecutionContext
import org.quartz.Scheduler
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory
import java.util.TimeZone

private val log = LoggerFactory.getLogger("scheduler")

@DisallowConcurrentExecution
class DailyScrapeJob : Job {
    override fun execute(context: JobExecutionContext) {
        log.info("Starting scheduled scrape")
        runCatching { runScraper() }
            .onSuccess { log.info("Scheduled scrape finished") }
            .onFailure { log.error("Scheduled scrape failed", it) }
    }
}

// Fires at 00:00 America/New_York every day.
fun dailyScrapeTrigger(): Trigger =
    TriggerBuilder.newTrigger()
        .withIdentity("daily-scrape")
        .withSchedule(
            CronScheduleBuilder.dailyAtHourAndMinute(0, 0)
                .inTimeZone(TimeZone.getTimeZone(NCSU_ZONE)),
        )
        .build()

fun startDailyScrapeScheduler(): Scheduler {
    val scheduler = StdSchedulerFactory.getDefaultScheduler()
    scheduler.scheduleJob(
        JobBuilder.newJob(DailyScrapeJob::class.java).withIdentity("daily-scrape").build(),
        dailyScrapeTrigger(),
    )
    scheduler.start()
    log.info("Daily scrape scheduled for 00:00 {}", NCSU_ZONE)
    Runtime.getRuntime().addShutdownHook(Thread { scheduler.shutdown(true) })
    return scheduler
}
